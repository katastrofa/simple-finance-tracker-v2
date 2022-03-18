package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.callback.CallbackTo
import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.{MICheckbox, MaterialIcon, TextInput}
import org.big.pete.sft.domain.{EnhancedMoneyAccount, TransactionTracking, TransactionType}
import org.big.pete.sft.front.SftMain.{dropDownCategoryTree, dropDownMoneyAccount}
import org.big.pete.sft.front.domain.{CategoryTree, EnhancedTransaction}
import org.big.pete.sft.front.helpers.ModalButtons
import org.scalajs.dom.html.Form

import java.time.LocalDate


object Transactions {
  final val trackingToIcon = Map[TransactionTracking, String](
    TransactionTracking.None -> "horizontal_rule",
    TransactionTracking.Auto -> "blur_circular",
    TransactionTracking.Verified -> "check_circle"
  )

  case class Props(
      transactions: List[EnhancedTransaction],
      checkTransaction: (MICheckbox.Status, String) => Callback,
      trackingChanged: (Int, TransactionTracking) => Callback
  )
  case class HeaderProps(checkTransaction: (MICheckbox.Status, String) => Callback)
  case class TransactionProps(
      transaction: EnhancedTransaction,
      checkTransaction: (MICheckbox.Status, String) => Callback,
      trackingChanged: (Int, TransactionTracking) => Callback
  )

  case class FormProps(
      initialDate: LocalDate,
      linearCats: List[CategoryTree],
      moneyAccounts: Map[Int, EnhancedMoneyAccount],
      close: Callback,
      publish: (LocalDate, TransactionType, BigDecimal, String, Int, Int, Option[BigDecimal], Option[Int]) => Callback
  )
  case class FormState(
      date: LocalDate,
      transactionType: TransactionType,
      amount: BigDecimal,
      destAmount: Option[BigDecimal],
      description: String,
      categoryId: Option[Int],
      moneyAccountId: Option[Int],
      destinationMoneyAccountId: Option[Int]
  )


  val component: Scala.Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      val reactTransactions = props.transactions.map { transaction =>
        transactionComponent.withKey(s"t-${transaction.id}")
          .apply(TransactionProps(transaction, props.checkTransaction, props.trackingChanged))
      }.toVdomArray

      tableWrap(
        TagMod.empty,
        headerComponent(HeaderProps(props.checkTransaction)),
        reactTransactions,
        headerComponent(HeaderProps(props.checkTransaction)),
        TagMod.empty
      )
    }.build

  val headerComponent: Component[HeaderProps, CtorType.Props] = ScalaFnComponent.apply[HeaderProps] { props =>
    <.tr(
      /// TODO: Checked status
      MICheckbox.component(MICheckbox.Props(
        <.th(_: _*),
        Map("check" -> true, "hide-on-med-and-down" -> true, "center-align" -> true),
        "sft-all",
        "",
        MICheckbox.Status.none,
        props.checkTransaction
      )),
      <.th(^.cls := "date", "Date"),
      <.th(^.cls := "description", "Description"),
      <.th(^.cls := "amount", "Amount"),
      <.th(^.cls := "category", "Category"),
      <.th(^.cls := "money-account", "Account"),
      <.th(^.cls := "status center-align hide-on-med-and-down", "")
    )
  }

  val transactionComponent: Component[TransactionProps, CtorType.Props] = ScalaFnComponent.apply[TransactionProps] { props =>
    <.tr(
      /// TODO: Checked transaction
      MICheckbox.component(MICheckbox.Props(
        <.td(_: _*),
        Map("check" -> true, "hide-on-med-and-down" -> true, "center-align" -> true),
        props.transaction.toString,
        "",
        MICheckbox.Status.none,
        props.checkTransaction
      )),
      <.td(^.cls := "date", props.transaction.date.format(DateFormat)),
      <.td(^.cls := "description", props.transaction.description),
      <.td(^.cls := "right-align amount", formatAmount(props.transaction.currencySymbol, props.transaction.amount)),
      <.td(^.cls := "category",
        <.span(^.cls := "show-on-large", props.transaction.categoryFullName),
        <.span(^.cls := "show-on-medium-and-down", props.transaction.categoryName)
      ),
      <.td(^.cls := "money-account", props.transaction.moneyAccountName),
      <.td(^.cls := "status center-align hide-on-med-and-down",
        MaterialIcon(
          MaterialIcon.`i`,
          MaterialIcon.`small`,
          trackingToIcon(props.transaction.tracking),
          props.trackingChanged(props.transaction.id, props.transaction.tracking)
        )
      )
    )
  }

  class FormBackend($: BackendScope[FormProps, FormState]) {

    def clean: Callback =
      $.modState(_.copy(amount = BigDecimal(0), destAmount = None, description = ""))

    def publish: Callback = for {
      props <- $.props
      s <- $.state
      _ <- props.close
      _ <- props.publish(s.date, s.transactionType, s.amount, s.description, s.categoryId.get, s.moneyAccountId.get, s.destAmount, s.destinationMoneyAccountId)
      _ <- clean
    } yield ()

    def render(props: FormProps, state: FormState): VdomTagOf[Form] = {
      def expandTransactionType(tt: TransactionType) =
        <.option(^.value := tt.toString, ^.key := s"add-tt-${tt.toString}", ^.selected := (state.transactionType == tt), tt.toString)

      <.form(
        <.div(^.cls := "row",
          ReactDatePicker.DatePicker(ReactDatePicker.Props(
            "add-tr-date",
            "col s12",
            ld => $.modState(_.copy(date = ld)) >> CallbackTo.pure(ld),
            Some(state.date),
            isOpened = false,
            ReactDatePicker.ExtendedKeyBindings
          ))
        ),
        <.div(^.cls := "row",
          <.select(^.cls := "browser-default", ^.tabIndex := 402, TransactionType.values.toVdomArray(expandTransactionType)),
          <.label("Transaction Type")
        ),
        <.div(^.cls := "row",
          TextInput(
            "add-tr-amount",
            "Amount",
            state.amount.toString(),
            na => $.modState(ns => ns.copy(amount = parseAmount(na.target.value, ns.amount))),
            403,
            List("col", "s12")
          )
        ),
        <.div(^.cls := "row",
          TextInput(
            "add-tr-description",
            "Description",
            state.description,
            e => $.modState(_.copy(description = e.target.value)),
            404,
            List("col", "s12")
          )
        ),
        <.div(^.cls := "row",
          dropDownCategoryTree.component(dropDownCategoryTree.Props(
            "add-tr-category",
            "Category",
            props.linearCats,
            CategoryTree.name,
            cat => s"k-cat-${cat.id}",
            cat => $.modState(_.copy(categoryId = Some(cat.id))),
            state.categoryId.flatMap(id => props.linearCats.find(_.id == id)),
            405,
            List("col", "s12")
          ))
        ),
        <.div(^.cls := "row",
          dropDownMoneyAccount.component(dropDownMoneyAccount.Props(
            "add-tr-ma",
            "Money Account",
            props.moneyAccounts.values.toList,
            _.name,
            ma => s"k-ma-${ma.id}",
            ma => $.modState(_.copy(moneyAccountId = Some(ma.id))),
            state.moneyAccountId.flatMap(id => props.moneyAccounts.get(id)),
            406,
            List("col", "s12")
          ))
        ),
        <.div(^.cls := "row",
          dropDownMoneyAccount.component(dropDownMoneyAccount.Props(
            "add-tr-ma-dest",
            "Destination Money Account",
            props.moneyAccounts.values.toList,
            _.name,
            ma => s"k-ma-${ma.id}",
            ma => $.modState(_.copy(destinationMoneyAccountId = Some(ma.id))),
            state.destinationMoneyAccountId.flatMap(id => props.moneyAccounts.get(id)),
            407,
            List("col", "s12")
          ))
        ).when(state.transactionType == TransactionType.Transfer),
        <.div(^.cls := "row",
          TextInput(
            "add-tr-amount-dest",
            "Destination Amount",
            state.destAmount.map(_.toString()).getOrElse(""),
            na => $.modState(ns => ns.copy(destAmount = Some(parseAmount(na.target.value, ns.amount)))),
            408,
            List("col", "s12")
          )
        ).when(state.transactionType == TransactionType.Transfer),
        ModalButtons.add(410, publish, props.close >> clean)
      )
    }
  }

}
