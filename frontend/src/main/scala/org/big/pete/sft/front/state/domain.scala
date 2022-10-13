package org.big.pete.sft.front.state

import japgolly.scalajs.react.extra.router.RouterCtl
import org.big.pete.sft.domain.{Account, Category, Currency, EnhancedMoneyAccount, Transaction, TransactionTracking, TransactionType}
import org.big.pete.sft.front.SftMain.SftPages
import org.big.pete.sft.front.components.header.SidenavFilters.FiltersOpen
import org.big.pete.sft.front.domain.{CategoryTree, EnhancedTransaction, Order, SortingColumn}

import java.time.LocalDate


case class Props(
    router: RouterCtl[SftPages],
    activePage: SftPages,
    initialFrom: LocalDate,
    initialTo: LocalDate,
    apiBase: String
)

case class State(
    from: LocalDate,
    to: LocalDate,

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

object domain {

}
