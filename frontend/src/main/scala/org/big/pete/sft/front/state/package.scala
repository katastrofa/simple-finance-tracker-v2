package org.big.pete.sft.front

import org.big.pete.sft.front.domain.{MAUpdateAction, MAUpdateOperation, Order, SortingColumn}


package object state {
  final val DefaultSorting: List[(SortingColumn, Order)] = List(
    SortingColumn.Date -> Order.Desc,
    SortingColumn.Description -> Order.Asc
  )

  final val AddBigDecimals: (BigDecimal, BigDecimal) => BigDecimal = _ + _
  final val SubtractBigDecimals: (BigDecimal, BigDecimal) => BigDecimal = _ - _
  final val MAOperations: Map[MAUpdateAction, Map[MAUpdateOperation, (BigDecimal, BigDecimal) => BigDecimal]] = Map(
    MAUpdateAction.Attach -> Map(
      MAUpdateOperation.Add -> AddBigDecimals,
      MAUpdateOperation.Remove -> SubtractBigDecimals
    ),
    MAUpdateAction.Reverse -> Map(
      MAUpdateOperation.Add -> SubtractBigDecimals,
      MAUpdateOperation.Remove -> AddBigDecimals
    ),
  )
}
