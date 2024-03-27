package org.big.pete.sft.front.components.main.account

import japgolly.scalajs.react.Reusability
import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.extra.internal.StateSnapshot
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}
import monocle.Lens
import org.big.pete.react.MaterialIcon
import org.big.pete.sft.domain.{Currency, EnhancedAccount, AccountCurrency, ShiftStrategyPerCurrency}
import org.big.pete.sft.front.components.main.tableWrap
import org.big.pete.sft.front.helpers.FormModal
import org.scalajs.dom.html.Element

import java.time.LocalDate


object Page {
  import org.big.pete.sft.front.domain.Implicits._

  case class Props(
      accounts: List[EnhancedAccount],
      currencies: Map[String, Currency],
      save: (Option[Int], String, LocalDate, List[AccountCurrency]) => Callback,
      delete: (Int, List[ShiftStrategyPerCurrency]) => Callback
  )

  case class CurrencyAmount(currency: Option[Currency], amount: BigDecimal) {
    def toDbObj(maId: Option[Int]): AccountCurrency =
      AccountCurrency(0, maId.getOrElse(0), currency.map(_.id).get, amount)
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
      shiftTransactions: Map[String, Option[EnhancedAccount]],
      toDelete: Option[Int]
  )

  implicit val accountPagePropsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("save", "delete")
  implicit val accountPageStateReuse: Reusability[State] = Reusability.derive[State]
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
    private val currencyAmountSnap = (id: Int) => StateSnapshot.withReuse.prepare(updateCurrencyAmount(id))
    private val shiftTransactionsSnap = (cur: String) => StateSnapshot.withReuse.prepare(changeShiftTransactions(cur))


    def changeName(name: Option[String], fn: Callback): Callback =
      $.modState(_.copy(name = name.getOrElse(""))) >> fn

    private def changeCreated(date: Option[LocalDate], fn: Callback): Callback =
      date.map(ld => $.modState(_.copy(created = ld))).getOrElse(Callback.empty) >> fn

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


    def changeShiftTransactions(currency: String)(accountOpt: Option[Option[EnhancedAccount]], fn: Callback): Callback = accountOpt.map { account =>
      $.modState { state =>
        val realAccount = if (account.isDefined && account.get.id == DeleteForm.NoShiftAccount.id) None else account
        state.copy(shiftTransactions = state.shiftTransactions + (currency -> realAccount))
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

    def openEditModal(account: EnhancedAccount): Callback = $.modState { state =>
      state.copy(
        isOpen = true, deleteIsOpen = false, Some(account.id), account.name, account.created,
        account.currencies.map(cur => cur.id -> CurrencyAmount(Some(cur.currency), cur.startAmount)).toMap,
        Map.empty, None
      )
    }

    def openDeleteModal(account: EnhancedAccount): Callback = $.modState { state =>
      val newShift = account.currencies.map(_.currency.id -> Some(DeleteForm.NoShiftAccount)).toMap
      state.copy(deleteIsOpen = true, shiftTransactions = newShift, toDelete = Some(account.id))
    }

    def deleteAccount(): Callback = for {
      props <- $.props
      state <- $.state
      shifting = state.shiftTransactions.map { case (currency, accountOpt) =>
        val id = if (accountOpt.isDefined && accountOpt.get.id == DeleteForm.NoShiftAccount.id) None else accountOpt.map(_.id)
        ShiftStrategyPerCurrency(id, currency)
      }.toList
      _ <- props.delete(state.toDelete.get, shifting)
      _ <- closeDeleteModal
    } yield ()


    def render(props: Props, state: State): VdomTagOf[Element] = {
      val accounts = props.accounts
        .map(account => Display.lineComponent.withKey(s"account-${account.id}")(Display.Props(account, openEditModal, openDeleteModal)))
        .toVdomArray

      val currencyAmounts = state.currencyAmounts.map { case (id, value) =>
        id -> currencyAmountSnap(id)(value)
      }
      val shiftTransactions = state.shiftTransactions.map { case (cur, ema) =>
        cur -> shiftTransactionsSnap(cur)(ema)
      }

      tableWrap(
        "accounts-table",
        List(
          FormModal.component(FormModal.Props("add-account-modal")) {
            EditForm(
              props.currencies, state.id, nameSnap(state.name), createdSnap(state.created), currencyAmounts,
              addCurrency(), removeCurrency, saveModal, closeModal
            )
          }.when(state.isOpen),
          FormModal.component(FormModal.Props("delete-account-modal")) {
            DeleteForm(
              props.accounts, state.toDelete.flatMap(id => props.accounts.find(_.id == id)), shiftTransactions,
              deleteAccount(), closeDeleteModal
            )
          }.when(state.deleteIsOpen)
        ).toTagMod,
        Display.headerComponent(),
        accounts,
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