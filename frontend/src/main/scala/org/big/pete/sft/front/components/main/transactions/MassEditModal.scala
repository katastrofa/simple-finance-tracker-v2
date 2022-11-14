package org.big.pete.sft.front.components.main.transactions

import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.{Callback, CtorType, Reusability, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.WithFocus
import org.big.pete.sft.domain.{Currency, EnhancedMoneyAccount, PeriodAmountStatus}
import org.big.pete.sft.front.SftMain.{dropDownCategoryTree, dropDownMoneyAccount}
import org.big.pete.sft.front.domain.CategoryTree
import org.big.pete.sft.front.helpers.ModalButtons
import org.scalajs.dom.html.Form

import java.time.LocalDate


object MassEditModal {
  import org.big.pete.sft.front.domain.Implicits.categoryTreeReuse
  import Page.moneyAccountMapReuse

  case class Props(
      linearCats: List[CategoryTree],
      moneyAccounts: Map[Int, EnhancedMoneyAccount],
      selectedCat: Option[Int],
      selectedMA: Option[Int],
      massEditCatChange: CategoryTree => Callback,
      massEditMAChange: EnhancedMoneyAccount => Callback,
      confirm: Callback,
      close: Callback
  )

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props](
    "massEditCatChange", "massEditMAChange", "confirm", "close"
  )

  final val LeaveAsIsCat = CategoryTree(-1, "Leave as is", None, 0, List.empty)
  final val LeaveAsIsMa = EnhancedMoneyAccount(
    -1, "Leave as is", BigDecimal(0), Currency("na", "na", "na"), LocalDate.now(), PeriodAmountStatus(BigDecimal(0), BigDecimal(0)), None
  )

  class Backend extends WithFocus[dropDownCategoryTree.Props, dropDownCategoryTree.State, dropDownCategoryTree.Backend] {
    def render(props: Props): VdomTagOf[Form] = {
      val extendedCats = LeaveAsIsCat :: props.linearCats
      val extendedMas = props.moneyAccounts + (-1 -> LeaveAsIsMa)

      <.form(
        <.div(^.cls := "row",
          dropDownCategoryTree.component.withRef(focusRef)(
            dropDownCategoryTree.Props(
              "mass-edit-tr-category",
              "Category",
              extendedCats,
              CategoryTree.name,
              cat => s"medit-cat-${cat.id}",
              props.massEditCatChange,
              props.selectedCat.flatMap(id => extendedCats.find(_.id == id)),
              430,
              List("col", "s12")
            )
          )
        ),
        <.div(^.cls := "row",
          dropDownMoneyAccount.component(
            dropDownMoneyAccount.Props(
              "mass-edit-tr-ma",
              "Money Account",
              extendedMas.values.toList,
              _.name,
              ma => s"medit-ma-${ma.id}",
              props.massEditMAChange,
              props.selectedMA.flatMap(id => extendedMas.get(id)),
              431,
              List("col", "s12")
            )
          )
        ),
        ModalButtons.component(
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
