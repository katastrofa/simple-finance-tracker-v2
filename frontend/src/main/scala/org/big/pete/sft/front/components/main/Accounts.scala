package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, Reusability, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.{MaterialIcon, TextInput}
import org.big.pete.sft.domain.Account
import org.big.pete.sft.front.SftMain.{SftPages, TransactionsPage}
import org.big.pete.sft.front.helpers.{AddModal, ModalButtons}
import org.big.pete.sft.front.utilz
import org.scalajs.dom.html.Form


object Accounts {
  case class Props(accounts: List[Account], ap: AccountProps, publish: (String, String) => Callback)
  case class AccountProps(
      router: RouterCtl[SftPages],
      activePage: SftPages,
      onPageChange: (SftPages, Option[SftPages]) => Callback
  )

  case class FormProps(publish: (String, String) => Callback, close: Callback)
  case class FormState(name: String, permalink: String)

  implicit val formPropsReuse: Reusability[FormProps] = Reusability.caseClassExcept[FormProps]("publish", "close")
  implicit val formStateReuse: Reusability[FormState] = Reusability.derive[FormState]


  val component: Scala.Component[Props, Boolean, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .initialState[Boolean](false)
    .renderPS { ($, props, isOpen) =>
      val accounts = props.accounts.map { account =>
        accountComponent.withKey(s"a-${account.id}").apply((account, props.ap))
      }.toVdomArray

      tableWrap(
        AddModal.component(AddModal.Props("add-account-modal", isOpen))(
          addModal.apply(FormProps(props.publish, $.modState(_ => false)))
        ),
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

  class FormBackend($: BackendScope[FormProps, FormState]) {
    def changeName(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(e.target.value, utilz.parsePermalink(e.target.value)))

    def changePermalink(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(permalink = utilz.parsePermalink(e.target.value)))

    def clean: Callback =
      $.modState(_.copy("", ""))

    def publish: Callback = for {
      props <- $.props
      state <- $.state
      _ <- props.close
      _ <- props.publish(state.name, state.permalink)
      _ <- clean
    } yield ()

    def render(props: FormProps, state: FormState): VdomTagOf[Form] = {
      <.form(
        <.div(^.cls := "row",
          TextInput.component(TextInput.Props("add-account-name", "Name", state.name, changeName, 101, List("col", "s12")))
        ),
        <.div(^.cls := "row",
          TextInput.component(TextInput.Props("add-account-permalink", "Permalink", state.permalink, changePermalink, 102, List("col", "s12")))
        ),
        ModalButtons.add(103, publish, props.close >> clean)
      )
    }
  }

  val addModal: Scala.Component[FormProps, FormState, FormBackend, CtorType.Props] = ScalaComponent.builder[FormProps]
    .initialState(FormState("", ""))
    .renderBackend[FormBackend]
    .configure(Reusability.shouldComponentUpdate)
    .build

}
