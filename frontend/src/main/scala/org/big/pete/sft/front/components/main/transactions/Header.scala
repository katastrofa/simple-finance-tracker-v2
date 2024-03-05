package org.big.pete.sft.front.components.main.transactions

import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, Reusability, ScalaFnComponent}
import org.big.pete.react.{MICheckbox, MaterialIcon}
import org.big.pete.sft.front.domain.{Order, SortingColumn}
import org.big.pete.sft.front.state.CheckAllId


object Header {
  import org.big.pete.sft.front.domain.Implicits._

  case class Props(
      checkAll: StateSnapshot[MICheckbox.Status],
      ordering: List[(SortingColumn, Order)],
      clickOrdering: SortingColumn => Callback
  )

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("clickOrdering")

  val component: Component[Props, CtorType.Props] = ScalaFnComponent.withReuse[Props] { props =>
    def orderingIcon(column: SortingColumn): String = {
      props.ordering.find(_._1 == column) match {
        case Some((_, Order.Asc)) => "arrow_drop_up"
        case Some((_, Order.Desc)) => "arrow_drop_down"
        case None => "sort"
      }
    }

    <.tr(
      MICheckbox.th(CheckAllId, "", props.checkAll, Map("check" -> true, "hide-on-med-and-down" -> true, "center-align" -> true)),
      <.th(^.cls := "date", "Date",
        MaterialIcon(
          MaterialIcon.i, MaterialIcon.small, orderingIcon(SortingColumn.Date), props.clickOrdering(SortingColumn.Date)
        )
      ),
      <.th(^.cls := "description", "Description",
        MaterialIcon(
          MaterialIcon.i, MaterialIcon.small, orderingIcon(SortingColumn.Description),
          props.clickOrdering(SortingColumn.Description)
        )
      ),
      <.th(
        ^.cls := "amount right-align", "Amount",
        MaterialIcon(
          MaterialIcon.i, MaterialIcon.small, orderingIcon(SortingColumn.Amount), props.clickOrdering(SortingColumn.Amount)
        )
      ),
      <.th(^.cls := "category", "Category"),
      <.th(^.cls := "money-account hide-on-med-and-down", "Account"),
      <.th(^.cls := "delete hide-on-med-and-down", ""),
      <.th(^.cls := "status center-align", "")
    )
  }
}
