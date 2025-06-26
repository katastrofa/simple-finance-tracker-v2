package org.big.pete.tyrian.domain

import org.big.pete.sft.domain.Category


object CategoryExtension {
  extension (cat: Category) {
    def fullName(using categories: Map[Int, Category]): String =
      parentCats(Some(cat.id), List.empty).map(_.name).mkString(" - ")
  }
}
