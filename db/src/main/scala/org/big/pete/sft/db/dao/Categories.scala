package org.big.pete.sft.db.dao

import doobie.ConnectionIO
import doobie.implicits._
import org.big.pete.sft.domain.Category


object Categories {
  def getCategory(id: Int): ConnectionIO[Option[Category]] =
    sql"SELECT * FROM categories WHERE id = $id".query[Category].option

  def listCategories(accountId: Int): ConnectionIO[List[Category]] =
    sql"SELECT * FROM categories WHERE account = $accountId".query[Category].to[List]

  def addCategory(category: Category): ConnectionIO[Int] =
    sql"""INSERT INTO categories (name, description, parent, account, owner) VALUE (
         ${category.name}, ${category.description}, ${category.parent}, ${category.accountId}, ${category.owner}
       )""".update.withUniqueGeneratedKeys[Int]("id")

  def editCategory(cat: Category, accountId: Int): ConnectionIO[Int] =
    sql"UPDATE categories SET name = ${cat.name}, description = ${cat.description}, parent = ${cat.parent} WHERE id = ${cat.id} AND account = $accountId"
      .update.run

  def deleteCategory(id: Int, accountId: Int): ConnectionIO[Int] =
    sql"DELETE FROM categories WHERE id = $id AND account = $accountId".update.run

  def updateCatParent(oldParent: Int, newParent: Option[Int], accountId: Int): ConnectionIO[Int] =
    sql"UPDATE categories SET parent = $newParent WHERE parent = $oldParent AND account = $accountId".update.run
}
