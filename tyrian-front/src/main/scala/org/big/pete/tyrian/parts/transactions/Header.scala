package org.big.pete.tyrian.parts.transactions

import org.big.pete.tyrian.component.ICheckbox
import org.big.pete.tyrian.domain.SortingName
import org.big.pete.tyrian.toolz.Views
import tyrian.{Html as ^, Html as <, Attr, Elem}


object Header {
  private def thWrapper(attributes: List[Attr[ICheckbox.Msg]])(children: List[Elem[ICheckbox.Msg]]): <[ICheckbox.Msg] =
    <.th(attributes)(children)

  def viewHeader(m: Model): <[Msg] = {
    <.tr(
      ICheckbox.view(m.headerCheckbox, thWrapper, "check hide-on-med-and-down center-align", "sft-all", "")
        .map(msg => Msg.MainCheckboxClick(msg)),
      <.th(^.cls := "date")(
        <.text("Date"),
        Views.icon(orderingIcon(m, SortingName.Date), Msg.SortingClick(SortingName.Date))
      ),
      <.th(^.cls := "description")(
        <.text("Description"),
        Views.icon(orderingIcon(m, SortingName.Description), Msg.SortingClick(SortingName.Description))
      ),
      <.th(^.cls := "amount right-align")(
        <.text("Amount"),
        Views.icon(orderingIcon(m, SortingName.Amount), Msg.SortingClick(SortingName.Amount))
      ),
      <.th(^.cls := "category")("Category"),
      <.th(^.cls := "money-account hide-on-med-and-down")("Account"),
      <.th(^.cls := "delete hide-on-med-and-down")(""),
      <.th(^.cls := "status center-align")("")
    )
  }

  private def orderingIcon(m: Model, column: SortingName): String = {
    import org.big.pete.tyrian.domain.SortingOrder.{Asc, Desc}
    
    m.ordering.find(_.name == column) match {
      case Some(x) if x.order == Asc => "arrow_drop_up"
      case Some(x) if x.order == Desc => "arrow_drop_down"
      case None => "sort"
      case Some(_) => "sort"
    }
  }
}
