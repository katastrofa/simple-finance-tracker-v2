package org.big.pete.sft.front.state

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import japgolly.scalajs.react.extra.router.RouterCtl
import org.big.pete.sft.domain.{Category, Currency, EnhancedAccount, FullWallet, SimpleUser, Transaction, TransactionTracking, TransactionType}
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
    accountsActiveFilters: Set[Int],
    checkedTransactions: Set[Int],
    transactionsSorting: List[(SortingColumn, Order)],

    me: SimpleUser,
    availablePatrons: List[SimpleUser],
    wallets: List[FullWallet],
    currencies: Map[String, Currency],
    categories: Map[Int, Category],
    accounts: Map[Int, EnhancedAccount],
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
    accountId: Option[Int],
    currency: Option[String],
    destAccountId: Option[Int],
    destCurrency: Option[String]
)

object Implicits {
  implicit val browserSettingsEncoder: Encoder[BrowserSettings] = deriveEncoder[BrowserSettings]
  implicit val browserSettingsDecoder: Decoder[BrowserSettings] = deriveDecoder[BrowserSettings]
  implicit val addTransactionSetupEncoder: Encoder[AddTransactionSetup] = deriveEncoder[AddTransactionSetup]
  implicit val addTransactionSetupDecoder: Decoder[AddTransactionSetup] = deriveDecoder[AddTransactionSetup]
}

object domain {
  final val emptyMe = SimpleUser(-1, "")
}
