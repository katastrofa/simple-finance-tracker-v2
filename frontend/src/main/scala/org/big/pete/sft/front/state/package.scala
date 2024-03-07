package org.big.pete.sft.front

import org.big.pete.sft.front.domain.{AccountUpdateAction, AccountUpdateOperation, Order, SortingColumn}


package object state {
  final val DefaultSorting: List[(SortingColumn, Order)] = List(
    SortingColumn.Date -> Order.Desc,
    SortingColumn.Description -> Order.Asc
  )

  final private val AddBigDecimals: (BigDecimal, BigDecimal) => BigDecimal = _ + _
  final private val SubtractBigDecimals: (BigDecimal, BigDecimal) => BigDecimal = _ - _
  final val AccountOperations: Map[AccountUpdateAction, Map[AccountUpdateOperation, (BigDecimal, BigDecimal) => BigDecimal]] = Map(
    AccountUpdateAction.Attach -> Map(
      AccountUpdateOperation.Add -> AddBigDecimals,
      AccountUpdateOperation.Remove -> SubtractBigDecimals
    ),
    AccountUpdateAction.Reverse -> Map(
      AccountUpdateOperation.Add -> SubtractBigDecimals,
      AccountUpdateOperation.Remove -> AddBigDecimals
    ),
  )

  final val CheckAllId = "sft-all"
}
