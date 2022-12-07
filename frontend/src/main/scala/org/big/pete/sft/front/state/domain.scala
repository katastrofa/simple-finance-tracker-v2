package org.big.pete.sft.front.state

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import japgolly.scalajs.react.extra.router.RouterCtl
import org.big.pete.sft.domain.{Account, Category, Currency, EnhancedMoneyAccount, Transaction, TransactionTracking, TransactionType}
import org.big.pete.sft.front.SftMain.SftPages
import org.big.pete.sft.front.components.header.SidenavFilters.FiltersOpen
import org.big.pete.sft.front.domain.{CategoryTree, EnhancedTransaction, Order, SortingColumn}

import java.time.LocalDate


case class Props(
    router: RouterCtl[SftPages],
    activePage: SftPages,
    apiBase: String
)

case class State(
    from: LocalDate,
    to: LocalDate,
    isMenuOpen: Boolean,

    activeFilter: Option[FiltersOpen],
    transactionTypeActiveFilters: Set[TransactionType],
    trackingActiveFilters: Set[TransactionTracking],
    contentFilter: String,
    categoriesActiveFilters: Set[Int],
    moneyAccountsActiveFilters: Set[Int],
    checkedTransactions: Set[Int],
    transactionsSorting: List[(SortingColumn, Order)],

    accounts: List[Account],
    currencies: List[Currency],
    categories: Map[Int, Category],
    moneyAccounts: Map[Int, EnhancedMoneyAccount],
    transactions: List[Transaction],

    categoryTree: List[CategoryTree],
    displayTransactions: List[EnhancedTransaction]
)

case class BrowserSettings(
    from: LocalDate,
    to: LocalDate
)

case class AddTransactionSetup(
    date: LocalDate,
    transactionType: TransactionType,
    categoryId: Option[Int],
    moneyAccountId: Option[Int],
    destMAId: Option[Int]
)

object Implicits {
  implicit val browserSettingsEncoder: Encoder[BrowserSettings] = deriveEncoder[BrowserSettings]
  implicit val browserSettingsDecoder: Decoder[BrowserSettings] = deriveDecoder[BrowserSettings]
  implicit val addTransactionSetupEncoder: Encoder[AddTransactionSetup] = deriveEncoder[AddTransactionSetup]
  implicit val addTransactionSetupDecoder: Decoder[AddTransactionSetup] = deriveDecoder[AddTransactionSetup]
}

object domain {}
