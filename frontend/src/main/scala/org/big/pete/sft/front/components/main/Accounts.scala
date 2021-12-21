package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.{CtorType, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.sft.domain.Account
import org.big.pete.sft.front.SftMain.{SftPages, TransactionsPage}


object Accounts {
  case class Props(router: RouterCtl[SftPages], accounts: List[Account])
  case class AccountProps(router: RouterCtl[SftPages], account: Account)

  val component: Scala.Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      val accounts = props.accounts.map { account =>
        accountComponent.withKey(s"a-${account.id}").apply(AccountProps(props.router, account))
      }.toVdomArray

      tableWrap(headerComponent(), accounts, headerComponent())
    }.build

  val headerComponent: Component[Unit, CtorType.Nullary] = ScalaFnComponent.apply[Unit] { _ =>
    <.tr(
      <.th(^.cls := "hide-on-small-only id", "ID"),
      <.th(^.cls := "name", "Name"),
      <.th(^.cls := "permalink", "Permalink")
    )
  }

  val accountComponent: Component[AccountProps, CtorType.Props] = ScalaFnComponent.apply[AccountProps] { props =>
    <.tr(
      <.td(^.cls := "hide-on-small-only id right-align", props.account.id.toString),
      <.td(^.cls := "name",
        props.router.link(TransactionsPage(props.account.permalink))(props.account.name)
      ),
      <.td(^.cls := "permalink", props.account.permalink)
    )
  }
}
