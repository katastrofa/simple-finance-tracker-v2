package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, Reusability, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.{MaterialIcon, TextInput}
import org.big.pete.sft.domain.{FullWallet, SimpleUser}
import org.big.pete.sft.front.SftMain.{WalletSelectionPage, SftPages, TransactionsPage, dropDownPatron}
import org.big.pete.sft.front.helpers.{FormModal, ModalButtons}
import org.big.pete.sft.front.utilz
import org.scalajs.dom.html.Element


object Accounts {
  import org.big.pete.sft.front.domain.Implicits._

  case class Props(
      me: SimpleUser,
      patrons: List[SimpleUser],
      accounts: List[FullWallet],
      activePage: SftPages,
      router: RouterCtl[SftPages],
      onPageChange: (SftPages, Option[SftPages]) => Callback,
      save: (Option[String], Option[Int], String, String, List[Int]) => Callback
  )
  case class State(
      isEditModalOpen: Boolean,
      id: Option[Int],
      name: String,
      permalink: String,
      patrons: Map[Int, SimpleUser]
  )
  case class AccountProps(
      activePage: SftPages,
      router: RouterCtl[SftPages],
      onPageChange: (SftPages, Option[SftPages]) => Callback,
      openEditModal: FullWallet => Callback
  )

  case class FormProps(
      id: Option[Int],
      name: String,
      permalink: String,
      me: SimpleUser,
      availablePatrons: List[SimpleUser],
      patrons: Map[Int, SimpleUser],
      changeName: ReactFormEventFromInput => Callback,
      changePermalink: ReactFormEventFromInput => Callback,
      addPatron: Callback,
      changePatron: Int => SimpleUser => Callback,
      save: Callback,
      close: Callback
  )
  case class PatronEditProps(
      availablePatrons: List[SimpleUser],
      patron: Option[SimpleUser],
      tabIndex: Int,
      changePatron: SimpleUser => Callback
  )


  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept("router", "onPageChange", "save")
  implicit val stateReuse: Reusability[State] = Reusability.derive[State]
  implicit val accountPropsReuse: Reusability[AccountProps] =
    Reusability.caseClassExcept[AccountProps]("router", "onPageChange", "openEditModal")
  implicit val formPropsReuse: Reusability[FormProps] =
    Reusability.caseClassExcept[FormProps]("changeName", "changePermalink", "addPatron", "changePatron", "save", "close")
  implicit val patronEditPropsReuse: Reusability[PatronEditProps] =
    Reusability.caseClassExcept[PatronEditProps]("changePatron")


  private def getFreePatrons(me: SimpleUser, patrons: List[SimpleUser], usedPatrons: Map[Int, SimpleUser]): List[SimpleUser] =
    patrons.filter(patron => !usedPatrons.values.exists(_.id == patron.id))
      .filter(_.id != me.id)


  class Backend($: BackendScope[Props, State]) {
    def changeName(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(name = e.target.value, permalink = utilz.parsePermalink(e.target.value)))

    private def changePermalink(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(permalink = utilz.parsePermalink(e.target.value)))

    private def addPatron(): Callback = $.props.flatMap { props =>
      $.modState { state =>
        val freePatrons = getFreePatrons(props.me, props.patrons, state.patrons)
        val keys = state.patrons.keys
        val newIndex = if (keys.nonEmpty) keys.max + 1 else 1
        if (freePatrons.nonEmpty) {
          state.copy(patrons = state.patrons + (newIndex -> freePatrons.head))
        } else
          state
      }
    }

    private def changePatron(id: Int)(patron: SimpleUser): Callback = $.modState { state =>
      state.copy(patrons = state.patrons + (id -> patron))
    }

    def close: Callback =
      $.modState(_.copy(isEditModalOpen = false))

    def save: Callback = {
      for {
        props <- $.props
        state <- $.state

        oldPermalink = props.accounts.find(account => state.id.contains(account.id)).map(_.permalink)
        _ <- props.save(oldPermalink, state.id, state.name, state.permalink, props.me.id :: state.patrons.values.toList.map(_.id))
        _ <- close
      } yield ()
    }

    def openAddNew: Callback =
      $.modState(_.copy(isEditModalOpen = true, None, "", "", Map.empty))

    def openEditModal(account: FullWallet): Callback = $.props.flatMap { props =>
      $.modState(_.copy(
        isEditModalOpen = true,
        Some(account.id),
        account.name,
        account.permalink,
        account.patrons.map(patron => patron.id -> patron).toMap - props.me.id
      ))
    }

    def render(props: Props, state: State): html_<^.VdomTagOf[Element] = {
      val accountProps = AccountProps(props.activePage, props.router, props.onPageChange, openEditModal)
      val accounts = props.accounts.map { account =>
        accountComponent.withKey(s"a-${account.id}").apply((account, accountProps))
      }.toVdomArray

      tableWrap(
        "accounts-table",
        FormModal.component(FormModal.Props("add-account-modal"))(
          addModal.apply(FormProps(
            state.id, state.name, state.permalink,
            props.me, props.patrons, state.patrons,
            changeName, changePermalink, addPatron(), changePatron,
            save, close
          ))
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
    .initialState[State](State(isEditModalOpen = false, None, "", "", Map.empty))
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

  private val accountComponent: Component[(FullWallet, AccountProps), CtorType.Props] = ScalaFnComponent.withReuse[(FullWallet, AccountProps)] { case (account, props) =>
    <.tr(
      <.td(^.cls := "hide-on-small-only id right-align", account.id.toString),
      <.td(^.cls := "name",
        MaterialIcon.Icon(MaterialIcon.Props(MaterialIcon.i, MaterialIcon.small, "edit", props.openEditModal(account), Set("right"))),
        <.a(
          ^.href := props.router.urlFor(TransactionsPage(account.permalink)).value,
          ^.onClick ==> (e => props.router.setEH(TransactionsPage(account.permalink))(e) >>
            props.onPageChange(TransactionsPage(account.permalink), Some(WalletSelectionPage))),
          account.name
        )
      ),
      <.td(^.cls := "permalink", account.permalink)
    )
  }

  private val addModal: Scala.Component[FormProps, Unit, Unit, CtorType.Props] = ScalaComponent.builder[FormProps]
    .stateless
    .render_P { props =>
      val freePatrons = getFreePatrons(props.me, props.availablePatrons, props.patrons)
      val patronComponents = props.patrons.zipWithIndex.map { case ((id, patron), i) =>
        patronEditComponent.withKey(s"add-patron-ck-$id")(PatronEditProps(
          patron :: freePatrons,
          Some(patron),
          105 + i,
          props.changePatron(id)
        ))
      }.toVdomArray

      <.form(
        <.div(^.cls := "row",
          TextInput.component(TextInput.Props("add-account-name", "Name", props.name, props.changeName, 101, List("col", "s12")))
        ),
        <.div(^.cls := "row",
          TextInput.component(
            TextInput.Props("add-account-permalink", "Permalink", props.permalink, props.changePermalink, 102, List("col", "s12"))
          )
        ),
        <.div(^.cls := "row", <.div(^.cls := "col s12", <.h6("Default Patron"), <.div(^.cls := "patron", props.me.displayName))),
        <.div(^.cls := "row",
          <.div(^.cls := "col s10", <.div(^.cls := "interaction", "Add Other Patron")),
          <.div(^.cls := "col s2",
            MaterialIcon.Icon(MaterialIcon.Props(MaterialIcon.i, MaterialIcon.midMedium, "add", props.addPatron)).when(freePatrons.nonEmpty)
          )
        ),
        patronComponents,
        ModalButtons(props.id.map(_ => "Save").getOrElse("Add"), 103, props.save, props.close)
      )
    }
    .configure(Reusability.shouldComponentUpdate)
    .build

  private val patronEditComponent = ScalaFnComponent.withReuse[PatronEditProps] { props =>
    <.div(^.cls := "row",
      dropDownPatron.component(dropDownPatron.Props(
        s"add-account-patron-${props.tabIndex}",
        "Patron",
        props.availablePatrons,
        _.displayName,
        patron => s"patron-${props.tabIndex}-${patron.id}",
        props.changePatron,
        props.patron,
        props.tabIndex,
        List("col", "s12")
      ))
    )
  }

}
