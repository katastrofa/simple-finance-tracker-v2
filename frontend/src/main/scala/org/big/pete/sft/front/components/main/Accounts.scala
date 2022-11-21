package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, Reusability, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.{MaterialIcon, TextInput}
import org.big.pete.sft.domain.Account
import org.big.pete.sft.front.SftMain.{SftPages, TransactionsPage}
import org.big.pete.sft.front.helpers.{AddModal, ModalButtons}
import org.big.pete.sft.front.utilz
import org.scalajs.dom.html.Element


object Accounts {
  import org.big.pete.sft.front.domain.Implicits.{accountReuse, sftPagesReuse}

  case class Props(
      accounts: List[Account],
      activePage: SftPages,
      router: RouterCtl[SftPages],
      onPageChange: (SftPages, Option[SftPages]) => Callback,
      save: (Option[String], Option[Int], String, String) => Callback
  )
  case class State(isEditModalOpen: Boolean, id: Option[Int], name: String, permalink: String)
  case class AccountProps(
      activePage: SftPages,
      router: RouterCtl[SftPages],
      onPageChange: (SftPages, Option[SftPages]) => Callback,
      openEditModal: Account => Callback
  )

  case class FormProps(
      id: Option[Int],
      name: String,
      permalink: String,
      changeName: ReactFormEventFromInput => Callback,
      changePermalink: ReactFormEventFromInput => Callback,
      save: Callback,
      close: Callback
  )


  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept("router", "onPageChange", "save")
  implicit val stateReuse: Reusability[State] = Reusability.derive[State]
  implicit val accountPropsReuse: Reusability[AccountProps] =
    Reusability.caseClassExcept[AccountProps]("router", "onPageChange", "openEditModal")
  implicit val formPropsReuse: Reusability[FormProps] =
    Reusability.caseClassExcept[FormProps]("changeName", "changePermalink", "save", "close")


  class Backend($: BackendScope[Props, State]) {
    def changeName(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(name = e.target.value, permalink = utilz.parsePermalink(e.target.value)))

    def changePermalink(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(permalink = utilz.parsePermalink(e.target.value)))

    def close: Callback =
      $.modState(_.copy(isEditModalOpen = false))

    def save: Callback = {
      for {
        props <- $.props
        state <- $.state

        oldPermalink = props.accounts.find(account => state.id.contains(account.id)).map(_.permalink)
        _ <- props.save(oldPermalink, state.id, state.name, state.permalink)
        _ <- close
      } yield ()
    }

    def openAddNew: Callback =
      $.modState(_.copy(isEditModalOpen = true, None, "", ""))

    def openEditModal(account: Account): Callback =
      $.modState(_.copy(isEditModalOpen = true, Some(account.id), account.name, account.permalink))

    def render(props: Props, state: State): html_<^.VdomTagOf[Element] = {
      val accountProps = AccountProps(props.activePage, props.router, props.onPageChange, openEditModal)
      val accounts = props.accounts.map { account =>
        accountComponent.withKey(s"a-${account.id}").apply((account, accountProps))
      }.toVdomArray

      tableWrap(
        AddModal.component(AddModal.Props("add-account-modal"))(
          addModal.apply(FormProps(state.id, state.name, state.permalink, changeName, changePermalink, save, close))
        ).when(state.isEditModalOpen),
        headerComponent(),
        accounts,
        headerComponent(),
        <.a(
          ^.cls := "waves-effect waves-light btn nice",
          ^.onClick --> openAddNew,
          MaterialIcon("add"),
          "Add"
        )
      )
    }
  }

  val component: Scala.Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialState[State](State(isEditModalOpen = false, None, "", ""))
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  val headerComponent: Component[Unit, CtorType.Nullary] = ScalaFnComponent.apply[Unit] { _ =>
    <.tr(
      <.th(^.cls := "hide-on-small-only id", "ID"),
      <.th(^.cls := "name", "Name"),
      <.th(^.cls := "permalink", "Permalink")
    )
  }

  val accountComponent: Component[(Account, AccountProps), CtorType.Props] = ScalaFnComponent.withReuse[(Account, AccountProps)] { case (account, props) =>
    <.tr(
      <.td(^.cls := "hide-on-small-only id right-align", account.id.toString),
      <.td(^.cls := "name",
        MaterialIcon.Icon(MaterialIcon.Props(MaterialIcon.i, MaterialIcon.small, "edit", props.openEditModal(account), Set("right"))),
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

  val addModal: Scala.Component[FormProps, Unit, Unit, CtorType.Props] = ScalaComponent.builder[FormProps]
    .stateless
    .render_P { props =>
      <.form(
        <.div(
          ^.cls := "row",
          TextInput.component(TextInput.Props("add-account-name", "Name", props.name, props.changeName, 101, List("col", "s12")))
        ),
        <.div(
          ^.cls := "row",
          TextInput.component(
            TextInput.Props("add-account-permalink", "Permalink", props.permalink, props.changePermalink, 102, List("col", "s12"))
          )
        ),
        ModalButtons(props.id.map(_ => "Save").getOrElse("Add"), 103, props.save, props.close)
      )
    }
    .configure(Reusability.shouldComponentUpdate)
    .build

}
