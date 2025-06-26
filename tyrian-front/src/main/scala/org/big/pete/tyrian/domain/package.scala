package org.big.pete.tyrian

import org.big.pete.sft.domain.Category

import scala.annotation.tailrec


package object domain {
  @tailrec
  private def parentCats(id: Option[Int], parents: List[Category])(using categories: Map[Int, Category]): List[Category] =
    id match {
      case None => List.empty
      case Some(value) => parentCats(categories(value).parent, categories(value) :: parents)
    }
}
