package org.big.pete.sft.db.dao

import doobie.ConnectionIO
import doobie.implicits._


object Transactions {
  def changeCategory(oldCat: Int, newCat: Int, accountId: Int): ConnectionIO[Int] =
    sql"""UPDATE transactions AS t JOIN categories AS c ON t.category = c.id
         SET t.category = $newCat WHERE t.category = $oldCat AND c.account = $accountId""".update.run
}
