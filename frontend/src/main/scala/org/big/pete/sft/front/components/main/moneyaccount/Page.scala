package org.big.pete.sft.front.components.main.moneyaccount

import japgolly.scalajs.react.Reusability
import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.extra.internal.StateSnapshot
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}
import monocle.Lens
import org.big.pete.react.MaterialIcon
import org.big.pete.sft.domain.{Currency, EnhancedMoneyAccount, MoneyAccountCurrency, ShiftStrategyPerCurrency}
import org.big.pete.sft.front.components.main.tableWrap
import org.big.pete.sft.front.helpers.AddModal
import org.scalajs.dom.html.Element

import java.time.LocalDate


object Page {
  import org.big.pete.sft.front.domain.Implicits._

  case class Props(
      accounts: List[EnhancedMoneyAccount],
      currencies: Map[String, Currency],
      save: (Option[Int], String, LocalDate, List[MoneyAccountCurrency]) => Callback,
      delete: (Int, List[ShiftStrategyPerCurrency]) => Callback
  )

  case class CurrencyAmount(currency: Option[Currency], amount: BigDecimal) {
    def toDbObj(maId: Option[Int]): MoneyAccountCurrency =
      MoneyAccountCurrency(0, maId.getOrElse(0), currency.map(_.id).get, amount)
  }
  object CurrencyAmount {
    val currency: Lens[CurrencyAmount, Option[Currency]] =
      Lens[CurrencyAmount, Option[Currency]](_.currency)(x => _.copy(currency = x))
    val amount: Lens[CurrencyAmount, BigDecimal] =
      Lens[CurrencyAmount, BigDecimal](_.amount)(x => _.copy(amount = x))
  }

  case class State(
      isOpen: Boolean,
      deleteIsOpen: Boolean,
      id: Option[Int],
      name: String,
      created: LocalDate,
      currencyAmounts: Map[Int, CurrencyAmount],
      shiftTransactions: Map[String, Option[EnhancedMoneyAccount]],
      toDelete: Option[Int]
  )

  implicit val maPagePropsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("save", "delete")
  implicit val maPageStateReuse: Reusability[State] = Reusability.derive[State]
  implicit val currencyAmountReuse: Reusability[CurrencyAmount] = Reusability.derive[CurrencyAmount]


  private val initialCurrencyAmount: CurrencyAmount =
    CurrencyAmount(None, BigDecimal(0))

  def availableCurrencies(currencies: Map[String, Currency], currencyAmounts: Iterable[CurrencyAmount]): Map[String, Currency] = {
    val usedCurrencies = currencyAmounts.flatMap(_.currency.map(_.id)).toSet
    currencies.filter { case (id, _) => !usedCurrencies.contains(id) }
  }


  class Backend($: BackendScope[Props, State]) {

    private val nameSnap = StateSnapshot.withReuse.prepare(changeName)
    private val createdSnap = StateSnapshot.withReuse.prepare(changeCreated)
//    private val startAmountSnap = (id: Int) => StateSnapshot.withReuse.prepare(updateStartAmount(id))
//    private val currencySnap = (id: Int) => StateSnapshot.withReuse.prepare(updateCurrency(id))
    private val currencyAmountSnap = (id: Int) => StateSnapshot.withReuse.prepare(updateCurrencyAmount(id))
    private val shiftTransactionsSnap = (cur: String) => StateSnapshot.withReuse.prepare(changeShiftTransactions(cur))


    def changeName(name: Option[String], fn: Callback): Callback =
      $.modState(_.copy(name = name.getOrElse(""))) >> fn

    private def changeCreated(date: Option[LocalDate], fn: Callback): Callback =
      date.map(ld => $.modState(_.copy(created = ld))).getOrElse(Callback.empty) >> fn

//    private def updateStartAmount(id: Int)(amount: Option[BigDecimal], fn: Callback): Callback = $.modState { state =>
//      val newTuple = state.currencyAmounts(id).copy(amount = amount.getOrElse(BigDecimal(0)))
//      state.copy(currencyAmounts = state.currencyAmounts + (id -> newTuple))
//    } >> fn
//
//    private def updateCurrency(id: Int)(currency: Option[Option[Currency]], fn: Callback): Callback = $.modState { state =>
//      val newTuple = state.currencyAmounts(id).copy(currency = currency.flatten)
//      state.copy(currencyAmounts = state.currencyAmounts + (id -> newTuple))
//    } >> fn

    private def updateCurrencyAmount(id: Int)(curOpt: Option[CurrencyAmount], fn: Callback): Callback = curOpt.map { curAmount =>
      $.modState { state => state.copy(currencyAmounts = state.currencyAmounts + (id -> curAmount)) }
    }.getOrElse(Callback.empty) >> fn

    private def addCurrency(): Callback = $.props.flatMap { props =>
      $.modState { state =>
        val availableCurs = availableCurrencies(props.currencies, state.currencyAmounts.values)
        val newId = if (state.currencyAmounts.isEmpty) 1 else state.currencyAmounts.keySet.max + 1
        val newItem = newId -> CurrencyAmount(availableCurs.headOption.map(_._2), BigDecimal(0))
        state.copy(currencyAmounts = state.currencyAmounts + newItem)
      }
    }

    private def removeCurrency(id: Int): Callback = $.modState { state =>
      state.copy(currencyAmounts = state.currencyAmounts - id)
    }


    def changeShiftTransactions(currency: String)(emaOpt: Option[Option[EnhancedMoneyAccount]], fn: Callback): Callback = emaOpt.map { ema =>
      $.modState { state =>
        val realEma = if (ema.isDefined && ema.get.id == DeleteForm.NoShiftMoneyAccount.id) None else ema
        state.copy(shiftTransactions = state.shiftTransactions + (currency -> realEma))
      }
    }.getOrElse(Callback.empty) >> fn


    def closeModal: Callback =
      $.modState(_.copy(isOpen = false))

    def closeDeleteModal: Callback =
      $.modState(_.copy(deleteIsOpen = false))

    private def saveModal: Callback = {
      for {
        props <- $.props
        state <- $.state
        _ <- props.save(state.id, state.name, state.created, state.currencyAmounts.values.toList.map(_.toDbObj(state.id)))
        _ <- closeModal
      } yield ()
    }

    def openAddNew: Callback = $.modState(_.copy(
      isOpen = true, deleteIsOpen = false, None, "", LocalDate.now(), Map(1 -> initialCurrencyAmount), Map.empty, None
    ))

    def openEditModal(account: EnhancedMoneyAccount): Callback = $.modState { state =>
      state.copy(
        isOpen = true, deleteIsOpen = false, Some(account.id), account.name, account.created,
        account.currencies.map(cur => cur.id -> CurrencyAmount(Some(cur.currency), cur.startAmount)).toMap,
        Map.empty, None
      )
    }

    def openDeleteModal(account: EnhancedMoneyAccount): Callback = $.modState { state =>
      val newShift = account.currencies.map(_.currency.id -> Some(DeleteForm.NoShiftMoneyAccount)).toMap
      state.copy(deleteIsOpen = true, shiftTransactions = newShift, toDelete = Some(account.id))
    }

    def deleteMoneyAccount(): Callback = for {
      props <- $.props
      state <- $.state
      shifting = state.shiftTransactions.map { case (currency, emaOpt) =>
        val id = if (emaOpt.isDefined && emaOpt.get.id == DeleteForm.NoShiftMoneyAccount.id) None else emaOpt.map(_.id)
        ShiftStrategyPerCurrency(id, currency)
      }.toList
      _ <- props.delete(state.toDelete.get, shifting)
      _ <- closeDeleteModal
    } yield ()


    def render(props: Props, state: State): VdomTagOf[Element] = {
      val moneyAccounts = props.accounts
        .map(ema => Display.lineComponent.withKey(s"ma-${ema.id}").apply(Display.Props(ema, openEditModal, openDeleteModal)))
        .toVdomArray

      val currencyAmounts = state.currencyAmounts.map { case (id, value) =>
        id -> currencyAmountSnap(id)(value)
      }
      val shiftTransactions = state.shiftTransactions.map { case (cur, ema) =>
        cur -> shiftTransactionsSnap(cur)(ema)
      }

      tableWrap(
        "money-accounts-table",
        List(
          AddModal.component(AddModal.Props("add-money-account-modal")) {
            EditForm(
              props.currencies, state.id, nameSnap(state.name), createdSnap(state.created), currencyAmounts,
              addCurrency(), removeCurrency, saveModal, closeModal
            )
          }.when(state.isOpen),
          AddModal.component(AddModal.Props("delete-money-account-modal")) {
            DeleteForm(
              props.accounts, state.toDelete.flatMap(id => props.accounts.find(_.id == id)), shiftTransactions,
              deleteMoneyAccount(), closeDeleteModal
            )
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
    .initialState(State(isOpen = false, deleteIsOpen = false, None, "", LocalDate.now(), Map(1 -> initialCurrencyAmount), Map.empty, None))
    .renderBackend[Backend]
    .build

}
