package org.big.pete.sft.front.domain

import org.big.pete.sft.domain.Category


case class CategoryTree(id: Int, name: String, treeLevel: Int, children: List[CategoryTree])

object CategoryTree {
  def generateTree(categories: List[Category]): List[CategoryTree] = {
    val groupedData = categories.groupBy(_.parent)

    def catToTree(cat: Category, level: Int): CategoryTree = {
      CategoryTree(
        cat.id,
        cat.name,
        level,
        groupedData.getOrElse(Some(cat.id), List.empty[Category])
          .map(childCat => catToTree(childCat, level + 1))
      )
    }

    groupedData.getOrElse(None, List.empty[Category])
      .map(cat => catToTree(cat, 0))
  }
}

object domain {

}
