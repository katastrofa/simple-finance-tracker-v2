package org.big.pete.tyrian.domain

trait DropDownItem[T] {
  extension (x: T) def key: String
  extension (x: T) def display: String
}