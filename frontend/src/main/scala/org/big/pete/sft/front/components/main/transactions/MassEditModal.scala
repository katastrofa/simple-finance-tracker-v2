package org.big.pete.sft.front.components.main.transactions

import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.{Callback, CtorType, Reusability, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.DropDown
import org.big.pete.react.WithFocus
import org.big.pete.sft.domain.EnhancedMoneyAccount
import org.big.pete.sft.front.domain.{CategoryTree, EnhancedTransaction}
import org.big.pete.sft.front.helpers.ModalButtons
import org.scalajs.dom.html.Form

import java.time.LocalDate


object MassEditModal {
  import org.big.pete.sft.front.domain.Implicits._

  case class Props(
      selectedTransactions: List[EnhancedTransaction],
      linearCats: List[CategoryTree],
      moneyAccounts: Map[Int, EnhancedMoneyAccount],
      selectedCat: StateSnapshot[Option[CategoryTree]],
      selectedMA: StateSnapshot[Option[EnhancedMoneyAccount]],
      confirm: Callback,
      close: Callback
  )

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props](
    "massEditCatChange", "massEditMAChange", "confirm", "close"
  )

  final private val LeaveAsIsCat = CategoryTree(-1, "Leave as is", None, "Leave as is", 0, None, List.empty)
  final private val LeaveAsIsMa = EnhancedMoneyAccount(-1, "Leave as is", LocalDate.now(), List.empty, List.empty, None)

  class Backend extends WithFocus[DropDown.Props, DropDown.State, DropDown.Backend] {
    def render(props: Props): VdomTagOf[Form] = {
      val extendedCats = LeaveAsIsCat :: props.linearCats
      val usedCurrencies = props.selectedTransactions.map(_.currency.id).toSet
      val extendedMas = props.moneyAccounts.filter { case (_, ma) =>
        ma.currencies.map(_.currency.id).toSet.intersect(usedCurrencies).size == usedCurrencies.size
      } + (-1 -> LeaveAsIsMa)

      <.form(
        <.div(^.cls := "row",
          DropDown.withRef(focusRef)(
            "mass-edit-tr-category",
            "Category",
            extendedCats,
            props.selectedCat,
            430,
            List("col", "s12")
          )
        ),
        <.div(^.cls := "row",
          DropDown(
            "mass-edit-tr-ma",
            "Money Account",
            extendedMas.values.toList,
            props.selectedMA,
            431,
            List("col", "s12")
          )
        ),
        ModalButtons.comp(
          ModalButtons.Props("Save", 432, props.confirm, props.close)
        )
      )
    }
  }

  val component: Component[Props, Unit, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build
}
