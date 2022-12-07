package org.big.pete.cache

import cats.syntax._
import cats.Monad
import cats.data.OptionT
import cats.effect.kernel.syntax.AsyncSyntax
import cats.effect.std.Semaphore
import cats.effect.{Async, Clock, Deferred, ExitCode, IO, IOApp, Ref}
import cats.implicits.catsSyntaxParallelSequence1

import scala.collection.mutable
import scala.concurrent.duration.{DurationInt, FiniteDuration}


case class Entry[V](inserted: FiniteDuration, value: V)

trait BpCache[F[_], K, V] extends MonadSyntax with FlatMapSyntax with FunctorSyntax {
  implicit val clockF: Clock[F]
  implicit val monadF: Monad[F]

  protected val data: Ref[F, mutable.Map[K, Entry[V]]]

  def contains(key: K): F[Boolean] =
    data.get.map(_.contains(key))

  def getEntry(key: K): F[Option[Entry[V]]] =
    data.get.map(_.get(key))

  def get(key: K): F[Option[V]] =
    getEntry(key).map(_.map(_.value))

  def putEntry(key: K, entry: Entry[V]): F[Entry[V]] =
    data.modify(_.addOne(key -> entry) -> entry)

  def put(key: K, value: V): F[Entry[V]] = for {
    inserted <- clockF.realTime
    entry <- putEntry(key, Entry(inserted, value))
  } yield entry

  def remove(key: K): F[Option[Entry[V]]] = data.modify { map =>
    val entry = map.remove(key)
    map -> entry
  }

  def clear(): F[Unit] = data.update { map =>
    map.clear()
    map
  }
}

trait MaxSizeBpCache[F[_], K, V] extends BpCache[F, K, V] with TraverseSyntax {
  protected val keyAges: Ref[F, mutable.SortedMap[FiniteDuration, mutable.Set[K]]]
  protected val canModify: Semaphore[F]
  val maxSize: Int
  val allowedOverhead: Int


  abstract override def putEntry(key: K, value: Entry[V]): F[Entry[V]] = {
    for {
      _ <- canModify.acquire
      alreadyContains <- contains(key)
      entryOpt <- if (alreadyContains) getEntry(key) else monadF.pure(None)
      _ <- if (entryOpt.isDefined) removeAge(entryOpt.get.inserted, key) else monadF.unit
      entry <- super.putEntry(key, value)
      _ <- addAge(entry.inserted, key)
      _ <- canModify.release
      _ <- checkAndRemoveOldestEntries()
    } yield entry
  }

  abstract override def remove(key: K): F[Option[Entry[V]]] = {
    val option = for {
      entry <- OptionT(super.remove(key))
      _ <- OptionT.liftF(canModify.acquire)
      _ <- OptionT.liftF(removeAge(entry.inserted, key))
      _ <- OptionT.liftF(canModify.release)
    } yield entry
    option.value
  }

  abstract override def clear(): F[Unit] = {
    for {
      _ <- canModify.acquire
      _ <- super.clear()
      _ <- keyAges.update(map => {map.clear(); map})
      _ <- canModify.release
    } yield ()
  }

  private def removeAge(inserted: FiniteDuration, key: K): F[Unit] = keyAges.update { map =>
    map.get(inserted).map(_.remove(key))
    map
  }

  private def addAge(inserted: FiniteDuration, key: K): F[Unit] = keyAges.update { map =>
    map.get(inserted) match {
      case Some(value) => value.addOne(key)
      case None => map.addOne((inserted, mutable.Set(key)))
    }
    map
  }

  private def checkAndRemoveOldestEntries(): F[Unit] = {
    for {
      entries <- data.get
      _ <- if (entries.size > maxSize + allowedOverhead) removeOldestEntries(entries.size - maxSize) else monadF.unit
    } yield ()
  }

  private def removeOldestEntries(approximateCount: Int): F[Unit] = {
    for {
      _ <- canModify.acquire
      keys <- getAndRemoveOldestKeys(approximateCount, Set.empty)
      _ <- keys.toList.traverse(key => super.remove(key).map(_ => ()))
      _ <- canModify.release
    } yield ()
  }

  private def removeOldest(): F[Set[K]] = keyAges.modify { keys =>
    keys.tail -> keys.head._2.toSet
  }

  private def getAndRemoveOldestKeys(approximateCount: Int, keys: Set[K]): F[Set[K]] = {
    if (keys.size >= approximateCount)
      monadF.pure(keys)
    else for {
      next <- removeOldest()
      finalSet <- getAndRemoveOldestKeys(approximateCount, keys ++ next)
    } yield finalSet
  }
}

trait AutoFetchBpCache[F[_], K, V] extends BpCache[F, K, V] {
  implicit val asyncF: Async[F]
  val fetchMethod: K => F[Option[V]]
  val fetchAttempts: Ref[F, Map[K, Deferred[F, Option[Entry[V]]]]]

  abstract override def getEntry(key: K): F[Option[Entry[V]]] = for {
    exists <- contains(key)
    value <- if (exists) super.getEntry(key) else fetchData(key)
  } yield value

  private def fetchData(key: K): F[Option[Entry[V]]] = {
    for {
      attempts <- fetchAttempts.get
      data <- if (attempts.contains(key)) attempts(key).get else fetchAndStoreData(key)
    } yield data
  }

  private def fetchAndStoreData(key: K): F[Option[Entry[V]]] = {
    for {
      defer <- Deferred[F, Option[Entry[V]]]
      _ <- fetchAttempts.update(_ + (key -> defer))
      data <- rawFetchData(key)
      _ <- if (data.isDefined) putEntry(key, data.get) else monadF.pure(data)
      _ <- defer.complete(data)
      _ <- fetchAttempts.update(_ - key)
    } yield data
  }

  private def rawFetchData(key: K): F[Option[Entry[V]]] = {
    val dataT = for {
      value <- OptionT(fetchMethod(key))
      ts <- OptionT.liftF(clockF.realTime)
    } yield Entry(ts, value)
    dataT.value
  }
}


class SimpleBpCache[F[_], K, V](val data: Ref[F, mutable.Map[K, Entry[V]]])(implicit val clockF: Clock[F], val monadF: Monad[F])
  extends BpCache[F, K, V]

object SimpleBpCache {
  def apply[K, V]: IO[SimpleBpCache[IO, K, V]] = for {
    data <- Ref[IO].of(mutable.Map.empty[K, Entry[V]])
  } yield new SimpleBpCache[IO, K, V](data)
}


class MaxSizeBpCacheImpl[F[_], K, V](
    val maxSize: Int,
    val data: Ref[F, mutable.Map[K, Entry[V]]],
    val keyAges: Ref[F, mutable.SortedMap[FiniteDuration, mutable.Set[K]]],
    val canModify: Semaphore[F],
    overhead: Option[Int] = None
)(
    implicit val clockF: Clock[F],
    val monadF: Monad[F]
) extends BpCache[F, K, V]
  with MaxSizeBpCache[F, K, V]
{
  override val allowedOverhead: Int = overhead.getOrElse(maxSize / 10)
}

object MaxSizeBpCache {
  def apply[K, V](maxSize: Int, overhead: Option[Int] = None): IO[MaxSizeBpCacheImpl[IO, K, V]] = for {
    data <- Ref[IO].of(mutable.Map.empty[K, Entry[V]])
    keyAges <- Ref[IO].of(mutable.SortedMap.empty[FiniteDuration, mutable.Set[K]])
    canModify <- Semaphore.apply[IO](1)
  } yield new MaxSizeBpCacheImpl[IO, K, V](maxSize, data, keyAges, canModify, overhead)
}


class FullBpCache[F[_], K, V](
    val maxSize: Int,
    val fetchMethod: K => F[Option[V]],
    val data: Ref[F, mutable.Map[K, Entry[V]]],
    val keyAges: Ref[F, mutable.SortedMap[FiniteDuration, mutable.Set[K]]],
    val canModify: Semaphore[F],
    val fetchAttempts: Ref[F, Map[K, Deferred[F, Option[Entry[V]]]]],
    overhead: Option[Int] = None
)(
    implicit val clockF: Clock[F],
    val monadF: Monad[F],
    val asyncF: Async[F]
) extends BpCache[F, K, V]
    with MaxSizeBpCache[F, K, V]
    with AutoFetchBpCache[F, K, V]
{
  override val allowedOverhead: Int = overhead.getOrElse(maxSize / 10)
}

object FullBpCache {
  def apply[K, V](maxSize: Int, fetchMethod: K => IO[Option[V]], overhead: Option[Int] = None): IO[FullBpCache[IO, K, V]] = for {
    data <- Ref[IO].of(mutable.Map.empty[K, Entry[V]])
    keyAges <- Ref[IO].of(mutable.SortedMap.empty[FiniteDuration, mutable.Set[K]])
    canModify <- Semaphore.apply[IO](1)
    attempts <- Ref[IO].of(Map.empty[K, Deferred[IO, Option[Entry[V]]]])
  } yield new FullBpCache[IO, K, V](maxSize, fetchMethod, data, keyAges, canModify, attempts, overhead)
}

object Test extends IOApp with AsyncSyntax {
  def fetch(key: Int): IO[Option[String]] =
    IO.println(s"fetching $key") >> IO.sleep(1000.millis).map(_ => Some(s"$key - stored"))

  def getMultiple(cache: FullBpCache[IO, Int, String], count: Int): IO[List[Option[String]]] = {
    Range.apply(0, count).map(_ => cache.get(42)).toList.parSequence
  }

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      cache <- FullBpCache(10, fetch, Some(1))
      val1 <- cache.get(3)
      _ <- IO.println(val1)
      val2 <- cache.get(1)
      _ <- IO.println(val2)
      val3 <- cache.get(3)
      _ <- IO.println(val3)
      val4 <- cache.get(2)
      _ <- IO.println(val4)

      data <- getMultiple(cache, 5)
      _ <- IO.println(data)

//      cache <- SimpleBpCache.apply[Int, String]
//      val1 <- cache.get(42)
//      _ <- IO.println(val1)
//      _ <- cache.put(42, "42 - stored")
//      val2 <- cache.get(42)
//      _ <- IO.println(val2)

//      cache <- MaxSizeBpCache.apply[Int, String](10, Some(1))
//      val1 <- cache.get(42)
//      _ <- IO.println(val1)
//      _ <- cache.put(42, "42 - stored")
//      val2 <- cache.get(42)
//      _ <- IO.println(val2)
//      _ <- cache.put(44, "44 - stored")
//      _ <- cache.put(45, "45 - stored")
//      _ <- cache.put(46, "46 - stored")
//      _ <- cache.put(47, "47 - stored")
//      _ <- cache.put(48, "48 - stored")
//      _ <- cache.put(49, "49 - stored")
//      _ <- cache.put(12, "12 - stored")
//      _ <- cache.put(22, "22 - stored")
//      _ <- cache.put(32, "32 - stored")
//      _ <- cache.put(442, "442 - stored")
//      _ <- cache.put(141, "141 - stored")
//      _ <- cache.put(142, "142 - stored")
//      _ <- cache.put(143, "143 - stored")
//      _ <- cache.put(144, "144 - stored")
//      _ <- cache.put(145, "145 - stored")
//      _ <- cache.put(146, "146 - stored")
//      val3 <- cache.get(42)
//      _ <- IO.println(val3)
//      val4 <- cache.get(45)
//      _ <- IO.println(val4)
//      val5 <- cache.get(142)
//      _ <- IO.println(val5)
//      val6 <- cache.get(145)
//      _ <- IO.println(val6)
    } yield ExitCode.Success
  }
}
