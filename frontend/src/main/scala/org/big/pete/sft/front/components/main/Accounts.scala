package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.sft.domain.Account
import org.big.pete.sft.front.SftMain.{SftPages, TransactionsPage}


object Accounts {
  case class Props(accounts: List[Account], ap: AccountProps)
  case class AccountProps(
      router: RouterCtl[SftPages],
      activePage: SftPages,
      onPageChange: (SftPages, Option[SftPages]) => Callback
  )

  val component: Scala.Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      val accounts = props.accounts.map { account =>
        accountComponent.withKey(s"a-${account.id}").apply((account, props.ap))
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

  val accountComponent: Component[(Account, AccountProps), CtorType.Props] = ScalaFnComponent.apply[(Account, AccountProps)] { case (account, props) =>
    <.tr(
      <.td(^.cls := "hide-on-small-only id right-align", account.id.toString),
      <.td(^.cls := "name",
        <.a(
          ^.href := props.router.urlFor(TransactionsPage(account.permalink)).value,
          ^.onClick ==> (e => props.router.setEH(TransactionsPage(account.permalink))(e) >>
            props.onPageChange(TransactionsPage(account.permalink), Some(props.activePage))),
          account.name
        )
      ),
      <.td(^.cls := "permalink", account.permalink)
    )
  }
}
