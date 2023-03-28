package org.big.pete.sft.front.helpers

import japgolly.scalajs.react.{BackendScope, Reusability}
import japgolly.scalajs.react.callback.Callback
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.sft.domain.Category
import org.big.pete.sft.front.SftMain.dropDownCategoryTree
import org.big.pete.sft.front.domain.CategoryTree


object CategorySelection {
  final case class Props(
      idPrefix: String,
      label: String,
      cats: List[CategoryTree],
      categoryMap: Map[Int, Category],
      tabIndex: Int,
      selected: StateSnapshot[Option[CategoryTree]],
      onEnterHit: Callback = Callback.empty
  )

  implicit val categoryPropsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("onEnterHit")

  final class Backend($: BackendScope[Props, Unit]) {
    def render(props: Props) = {
      <.div(^.cls := "row",
        dropDownCategoryTree.component(
          dropDownCategoryTree.Props(
            s"${props.idPrefix}-category",
            props.label,
            props.cats,
            CategoryTree.fullName(props.categoryMap),
            item => s"${props.idPrefix}-key-${item.id}",
            item => props.selected.setState(Some(item)),
            props.selected.value,
            props.tabIndex,
            List("col", "s12"),
            props.onEnterHit
          )
        )
      )
    }
  }

  val component =
}
