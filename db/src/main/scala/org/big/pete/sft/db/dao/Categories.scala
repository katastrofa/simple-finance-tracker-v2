package org.big.pete.sft.db.dao

import doobie.ConnectionIO
import doobie.implicits._
import org.big.pete.sft.domain.Category


object Categories {
  def getCategory(id: Int): ConnectionIO[Option[Category]] =
    sql"SELECT * FROM categories WHERE id = $id".query[Category].option

  def listCategories(wallet: Int): ConnectionIO[List[Category]] =
    sql"SELECT * FROM categories WHERE wallet = $wallet".query[Category].to[List]

  def addCategory(category: Category): ConnectionIO[Int] =
    sql"""INSERT INTO categories (name, description, parent, wallet, owner) VALUE (
         ${category.name}, ${category.description}, ${category.parent}, ${category.wallet}, ${category.owner}
       )""".update.withUniqueGeneratedKeys[Int]("id")

  def editCategory(cat: Category, wallet: Int): ConnectionIO[Int] =
    sql"UPDATE categories SET name = ${cat.name}, description = ${cat.description}, parent = ${cat.parent} WHERE id = ${cat.id} AND wallet = $wallet"
      .update.run

  def deleteCategory(id: Int, wallet: Int): ConnectionIO[Int] =
    sql"DELETE FROM categories WHERE id = $id AND wallet = $wallet".update.run

  def updateCatParent(oldParent: Int, newParent: Option[Int], wallet: Int): ConnectionIO[Int] =
    sql"UPDATE categories SET parent = $newParent WHERE parent = $oldParent AND wallet = $wallet".update.run
}
