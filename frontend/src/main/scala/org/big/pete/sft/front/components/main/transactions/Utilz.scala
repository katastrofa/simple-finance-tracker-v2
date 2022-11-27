package org.big.pete.sft.front.components.main.transactions

import japgolly.scalajs.react.Callback
import org.scalajs.dom.{HTMLTableCellElement, console, document, window}


trait Utilz {
  protected var currentWaitingExecution: Option[Int] = None

  def calculateColSpan: Int = {
    document.querySelectorAll("#transactions-table tr:first-child td").filterNot { el =>
      window.getComputedStyle(el).display == "none"
    }.length
  }

  def updateColSpan(): Unit = {
    currentWaitingExecution = None
    val colSpan = calculateColSpan
    document.querySelectorAll("#transactions-table .details-col").foreach {
      case element: HTMLTableCellElement =>
        element.colSpan = colSpan
      case element =>
        console.log(element.tagName)
    }
  }

  def justLogStuff(): Unit = {
    currentWaitingExecution = None
    console.log("resize called")
  }

  def controlledInvocationOfUpdateColSpan(): Unit = {
    currentWaitingExecution.foreach(id => window.clearTimeout(id))
    currentWaitingExecution = Some(window.setTimeout(() => updateColSpan(), Utilz.DefaultDelayOfExec))
  }

  def controlledInvocationOfUpdateColSpanCB: Callback =
    Callback(controlledInvocationOfUpdateColSpan())
}

object Utilz {
  final val DefaultDelayOfExec = 100
}
