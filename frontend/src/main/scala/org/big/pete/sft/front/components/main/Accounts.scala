package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.{MaterialIcon, TextInput}
import org.big.pete.sft.domain.Account
import org.big.pete.sft.front.SftMain.{SftPages, TransactionsPage}
import org.big.pete.sft.front.utilz


object Accounts {
  case class Props(accounts: List[Account], ap: AccountProps, publish: (String, String) => Callback)
  case class AccountProps(
      router: RouterCtl[SftPages],
      activePage: SftPages,
      onPageChange: (SftPages, Option[SftPages]) => Callback
  )

  case class ModalProps(isOpen: Boolean, publish: (String, String) => Callback, close: Callback)

  val component: Scala.Component[Props, Boolean, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .initialState[Boolean](false)
    .renderPS { ($, props, isOpen) =>
      val accounts = props.accounts.map { account =>
        accountComponent.withKey(s"a-${account.id}").apply((account, props.ap))
      }.toVdomArray

      tableWrap(
        addModal.apply(ModalProps(isOpen, props.publish, $.modState(_ => false))),
        headerComponent(),
        accounts,
        headerComponent(),
        <.a(^.cls := "waves-effect waves-light btn nice",
          ^.onClick --> $.modState(_ => true),
          MaterialIcon("add"),
          "Add"
        )
      )
    }.build

  val addModal: Component[ModalProps, CtorType.Props] = ScalaFnComponent.withHooks[ModalProps]
    .useState[String]("")
    .useState[String]("")
    .render { (props: ModalProps, name: Hooks.UseState[String], permalink: Hooks.UseState[String]) =>
      def changeName(e: ReactFormEventFromInput): Callback =
        name.modState(_ => e.target.value) >> permalink.modState(_ => utilz.parsePermalink(e.target.value))
      def changePermalink(e: ReactFormEventFromInput): Callback =
        permalink.modState(_ => utilz.parsePermalink(e.target.value))

      <.div(^.id := "add-account-modal",
        ^.tabIndex := 1,
        ^.classSet("modal" -> true, "open" -> props.isOpen),
        <.div(^.cls := "modal-content",
          <.div(^.cls := "container",
            <.form(
              <.div(^.cls := "row",
                TextInput.component(TextInput.Props("add-account-name", "Name", name.value, changeName, 101, List("col", "s12")))
              ),
              <.div(^.cls := "row",
                TextInput.component(TextInput.Props("add-account-permalink", "Permalink", permalink.value, changePermalink, 102, List("col", "s12")))
              ),
              <.div(^.cls := "row",
                <.div(^.cls := "col s12 right-align",
                  <.button(^.cls := "waves-effect waves-light btn",
                    ^.`type` := "button",
                    ^.tabIndex := 103,
                    ^.onClick --> (props.close >>
                      props.publish(name.value, permalink.value) >>
                      name.modState(_ => "") >>
                      permalink.modState(_ => "")
                    ),
                    MaterialIcon("add_task"),
                    "Add"
                  ),
                  <.button(^.cls := "waves-effect waves-light btn",
                    ^.`type` := "button",
                    ^.tabIndex := 104,
                    ^.onClick --> (props.close >> name.modState(_ => "") >> permalink.modState(_ => "")),
                    MaterialIcon("highlight_off"),
                    "Close"
                  )
                )
              )
            )
          )
        )
      )
    }

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
