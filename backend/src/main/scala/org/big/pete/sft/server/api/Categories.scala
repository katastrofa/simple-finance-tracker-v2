package org.big.pete.sft.server.api

import cats.effect.kernel.MonadCancelThrow
import cats.syntax.{FlatMapSyntax, FunctorSyntax}
import doobie.syntax.ToConnectionIOOps
import doobie.util.transactor.Transactor
import io.circe.syntax.EncoderOps
import org.big.pete.sft.db.dao.{Categories => CategoriesDao, Transactions => TransactionsDao}
import org.big.pete.sft.domain.{Category, ShiftStrategy}
import org.big.pete.sft.domain.Implicits._
import org.http4s.Response
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._


class Categories[F[_]: MonadCancelThrow](
    dsl: Http4sDsl[F],
    implicit val transactor: Transactor[F]
) extends ToConnectionIOOps with FlatMapSyntax with FunctorSyntax {
  import dsl._

  def getCategory(catId: Int): F[Category] =
    CategoriesDao.getCategory(catId).transact(transactor).map(_.get)

  def listCategories(accountId: Int): F[Response[F]] = {
    for {
      cats <- CategoriesDao.listCategories(accountId).transact(transactor)
      response <- Ok(cats.asJson)
    } yield response
  }

  def addCategory(cat: Category): F[Response[F]] = {
    for {
      newId <- CategoriesDao.addCategory(cat).transact(transactor)
      newCat <- CategoriesDao.getCategory(newId).transact(transactor)
      response <- Ok(newCat.get.asJson)
    } yield response
  }

  def editCategory(cat: Category, accountId: Int): F[Response[F]] = {
    for {
      _ <- CategoriesDao.editCategory(cat, accountId).transact(transactor)
      newCat <- CategoriesDao.getCategory(cat.id).transact(transactor)
      response <- Ok(newCat.get.asJson)
    } yield response
  }

  def deleteCategory(id: Int, accountId: Int, catsShiftStrategy: ShiftStrategy, transactionsShiftStrategy: ShiftStrategy): F[Response[F]] = {
    for {
      _ <- CategoriesDao.updateCatParent(id, catsShiftStrategy.newId, accountId).transact(transactor)
      _ <- TransactionsDao.changeCategory(id, transactionsShiftStrategy.newId.get, accountId).transact(transactor)
      _ <- CategoriesDao.deleteCategory(id, accountId).transact(transactor)
      response <- Ok("")
    } yield response
  }
}
