package org.big.pete.sft.front.components.main.moneyaccount

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.vdom.html_<^
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CallbackTo, CtorType, ReactFormEventFromInput, ScalaComponent}
import org.big.pete.react.MaterialIcon
import org.big.pete.sft.domain.{Currency, EnhancedMoneyAccount, MoneyAccountCurrency, ShiftStrategyPerCurrency}
import org.big.pete.sft.front.components.main.tableWrap
import org.big.pete.sft.front.helpers.AddModal
import org.scalajs.dom.html.Element

import java.time.LocalDate
import scala.util.{Failure, Success, Try}


object Page {

  case class Props(
      accounts: List[EnhancedMoneyAccount],
      currencies: Map[String, Currency],
      save: (Option[Int], String, LocalDate, List[MoneyAccountCurrency]) => Callback,
      delete: (Int, List[ShiftStrategyPerCurrency]) => Callback
  )

  case class State(
      isOpen: Boolean,
      deleteIsOpen: Boolean,
      id: Option[Int],
      name: String,
      created: LocalDate,
      editCurrencies: Map[Int, MoneyAccountCurrency],
      shiftTransactionsTo: Map[String, Int],
      toDelete: Option[Int]
  )

  private def initialMACurrency(currencies: Map[String, Currency]) =
    MoneyAccountCurrency(0, 0, currencies.head._2.id, BigDecimal(0))


  class Backend($: BackendScope[Props, State]) {
    def changeName(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(name = e.target.value))

    private def changeAmount(curId: Int)(e: ReactFormEventFromInput): Callback = $.modState { state =>
      val newCurrency = Try(BigDecimal(e.target.value.trim)) match {
        case Failure(_) => state.editCurrencies(curId)
        case Success(value) => state.editCurrencies(curId).copy(startAmount = value)
      }
      state.copy(editCurrencies = state.editCurrencies + (curId -> newCurrency))
    }

    private def changeCurrency(curId: Int)(cur: Currency): Callback = $.modState { state =>
      val newCurrency = state.editCurrencies(curId).copy(currency = cur.id)
      state.copy(editCurrencies = state.editCurrencies + (curId -> newCurrency))
    }

    private def removeEditCurrency(curId: Int): Callback = $.modState { state =>
      state.copy(editCurrencies = state.editCurrencies.removed(curId))
    }

    private def addEditCurrency(): Callback = $.props.flatMap { props =>
      $.modState { state =>
        val usedCurrencies = state.editCurrencies.values.map(_.currency).toSet
        val unusedCurrency = props.currencies.find(item => !usedCurrencies.contains(item._1)).get._1
        val newId = state.editCurrencies.keys.max + 1
        val newEditCurrency = MoneyAccountCurrency(newId, state.id.getOrElse(0), unusedCurrency, BigDecimal(0))
        state.copy(editCurrencies = state.editCurrencies + (newId -> newEditCurrency))
      }
    }

    private def changeCreated(date: LocalDate): CallbackTo[LocalDate] =
      $.modState(_.copy(created = date)) >> CallbackTo.pure(date)

    def changeShiftTransactions(currency: String)(ema: EnhancedMoneyAccount): Callback = $.modState { state =>
      state.copy(shiftTransactionsTo = state.shiftTransactionsTo + (currency -> ema.id))
    }

    def closeModal: Callback =
      $.modState(_.copy(isOpen = false))

    def closeDeleteModal: Callback =
      $.modState(_.copy(deleteIsOpen = false))

    private def saveModal: Callback = {
      for {
        props <- $.props
        state <- $.state
        _ <- props.save(state.id, state.name, state.created, state.editCurrencies.values.toList)
        _ <- closeModal
      } yield ()
    }

    def openAddNew: Callback = $.props.flatMap { props =>
      val maCurrency = initialMACurrency(props.currencies)
      $.modState { state =>
        state.copy(isOpen = true, deleteIsOpen = false, None, "", LocalDate.now(), Map(1 -> maCurrency))
      }
    }

    def openEditModal(account: EnhancedMoneyAccount): Callback = $.modState { state =>
      state.copy(
        isOpen = true, deleteIsOpen = false, Some(account.id), account.name, account.created,
        account.currencies.map(cur => cur.id -> cur.simple).toMap
      )
    }

    def openDeleteModal(account: EnhancedMoneyAccount): Callback = $.modState { state =>
      val newShift = account.currencies.map(_.currency.id -> Forms.NoShiftMoneyAccount.id).toMap
      state.copy(deleteIsOpen = true, shiftTransactionsTo = newShift, toDelete = Some(account.id))
    }

    def deleteMoneyAccount(): Callback = for {
      props <- $.props
      state <- $.state
      shifting = state.shiftTransactionsTo.map { case (currency, id) =>
        ShiftStrategyPerCurrency(if (id == Forms.NoShiftMoneyAccount.id) None else Some(id), currency)
      }.toList
      _ <- props.delete(state.toDelete.get, shifting)
      _ <- closeDeleteModal
    } yield ()


    def render(props: Props, state: State): html_<^.VdomTagOf[Element] = {
      val moneyAccounts = props.accounts
        .map(ema => Display.lineComponent.withKey(s"ma-${ema.id}").apply(Display.Props(ema, openEditModal, openDeleteModal)))
        .toVdomArray

      tableWrap(
        "money-accounts-table",
        List(
          AddModal.component(AddModal.Props("add-money-account-modal")) {
            Forms.editForm(Forms.FormProps(
              props.currencies, state.id, state.name, state.created, state.editCurrencies,
              changeName, changeAmount, changeCurrency, changeCreated, addEditCurrency(), removeEditCurrency, saveModal, closeModal
            ))
          }.when(state.isOpen),
          AddModal.component(AddModal.Props("delete-money-account-modal")) {
            Forms.deleteMoneyAccountForm(Forms.DeleteMoneyAccountProps(
              props.accounts, state.toDelete.flatMap(id => props.accounts.find(_.id == id)), state.shiftTransactionsTo,
              changeShiftTransactions, deleteMoneyAccount(), closeDeleteModal
            ))
          }.when(state.deleteIsOpen)
        ).toTagMod,
        Display.headerComponent(),
        moneyAccounts,
        Display.headerComponent(),
        <.a(
          ^.cls := "waves-effect waves-light btn nice",
          ^.onClick --> openAddNew,
          MaterialIcon("add"),
          "Add"
        )
      )
    }
  }

  val component: Scala.Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialStateFromProps(props => State(
      isOpen = false, deleteIsOpen = false, None, "", LocalDate.now(), Map(1 -> initialMACurrency(props.currencies)), Map.empty, None
    ))
    .renderBackend[Backend]
    .build





}
