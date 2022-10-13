package org.big.pete.sft.front.state

import japgolly.scalajs.react.ReactFormEventFromInput
import japgolly.scalajs.react.callback.Callback
import org.big.pete.react.MICheckbox
import org.big.pete.sft.domain.{TransactionTracking, TransactionType}
import org.big.pete.sft.front.components.header.SidenavFilters.FiltersOpen


trait Filtering extends Base {
  def setActiveFilter(opened: FiltersOpen): Callback = $.modState { state =>
    if (state.activeFilter.contains(opened))
      state.copy(activeFilter = None)
    else
      state.copy(activeFilter = Some(opened))
  }

  def setTtFilter(status: MICheckbox.Status, tt: String): Callback = $.modState { state =>
    val newFilter = modStateForSet(status, state, _.transactionTypeActiveFilters, TransactionType.withName(tt))
    state.copy(
      transactionTypeActiveFilters = newFilter,
      displayTransactions = filterTransactions(state, transactionTypeActiveFilters = Some(newFilter))
    )
  }

  def setTrackingFilter(status: MICheckbox.Status, tracking: String): Callback = $.modState { state =>
    val newFilter = modStateForSet(status, state, _.trackingActiveFilters, TransactionTracking.withName(tracking))
    state.copy(
      trackingActiveFilters = newFilter,
      displayTransactions = filterTransactions(state, trackingActiveFilters = Some(newFilter))
    )
  }

  def setContentFilter(e: ReactFormEventFromInput): Callback = $.modState { state =>
    val newFilter = e.target.value.trim
    state.copy(
      contentFilter = newFilter,
      displayTransactions = filterTransactions(state, contentFilter = Some(newFilter))
    )
  }

  def setCategoriesFilter(status: MICheckbox.Status, catId: String): Callback = $.modState { state =>
    val newFilter = modStateForSet(status, state, _.categoriesActiveFilters, catId.toInt)
    state.copy(
      categoriesActiveFilters = newFilter,
      displayTransactions = filterTransactions(state, categoriesActiveFilters = Some(newFilter))
    )
  }

  def setMoneyAccountsFilter(status: MICheckbox.Status, moneyAccountId: String): Callback = $.modState { state =>
    val newFilter = modStateForSet(status, state, _.moneyAccountsActiveFilters, moneyAccountId.toInt)
    state.copy(
      moneyAccountsActiveFilters = newFilter,
      displayTransactions = filterTransactions(state, moneyAccountsActiveFilters = Some(newFilter))
    )
  }
}
