package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.component.{Scala, ScalaFn}
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.vdom.html_<^
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, Reusability, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.{MaterialIcon, TextInput}
import org.big.pete.sft.front.SftMain
import org.big.pete.sft.front.domain.CategoryTree
import org.big.pete.sft.front.helpers.{AddModal, ModalButtons}
import org.scalajs.dom.html.Element


object Categories {
  import org.big.pete.sft.front.domain.Implicits._

  case class Props(categories: List[CategoryTree], save: (Option[Int], String, String, Option[Int]) => Callback)
  case class State(isOpen: Boolean, id: Option[Int], name: String, description: String, parent: CategoryTree)
  case class CategoryProps(category: CategoryTree, openEditModal: CategoryTree => Callback)
  final val TopLevelCat: CategoryTree = CategoryTree(-42, "Top Level", None, 0, List.empty)

  case class FormProps(
      availableParents: List[CategoryTree],
      id: Option[Int],
      name: String,
      description: String,
      parent: CategoryTree,
      changeName: ReactFormEventFromInput => Callback,
      changeDescription: ReactFormEventFromInput => Callback,
      changeParent: CategoryTree => Callback,
      save: Callback,
      close: Callback
  )

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("save")
  implicit val stateReuse: Reusability[State] = Reusability.derive[State]
  implicit val formPropsReuse: Reusability[FormProps] =
    Reusability.caseClassExcept[FormProps]("changeName", "changeDescription", "changeParent", "save", "close")


  class Backend($: BackendScope[Props, State]) {

    def changeName(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(name = e.target.value))

    def changeDescription(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(description = e.target.value))

    def changeParent(cat: CategoryTree): Callback =
      $.modState(_.copy(parent = cat))

    def saveEdit: Callback = for {
      state <- $.state
      props <- $.props
      _ <- closeModal
      _ <- props.save(state.id, state.name, state.description, Some(state.parent.id))
    } yield ()

    def closeModal: Callback =
      $.modState(_.copy(isOpen = false))

    def openAddNew: Callback = $.modState { state =>
      state.copy(isOpen = true, Some(-42), "", "", TopLevelCat)
    }

    def openEditModal(cat: CategoryTree): Callback = $.modState { state =>
      state.copy(isOpen = true, Some(cat.id), state.name, state.description, state.parent)
    }

    def render(props: Props, state: State): html_<^.VdomTagOf[Element] = {
      def expandCategories(cat: CategoryTree): List[ScalaFn.Unmounted[CategoryProps]] = {
        if (state.parent.id == -42 || state.parent.id != cat.id)
          categoryComponent.withKey(s"cat-${cat.id}").apply(CategoryProps(cat, openEditModal)) ::
            cat.children.flatMap(expandCategories)
        else
          Nil
      }

      val categoryLines = props.categories.flatMap(expandCategories).toVdomArray

      tableWrap(
        AddModal.component(AddModal.Props("add-category-modal", state.isOpen))(
          addCategoryModal.apply(FormProps(
            props.categories,
            state.id,
            state.name,
            state.description,
            state.parent,
            changeName,
            changeDescription,
            changeParent,
            saveEdit,
            closeModal
          ))
        ),
        headerComponent(),
        categoryLines,
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
    .initialState[State](State(isOpen = false, None, "", "", TopLevelCat))
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  val headerComponent: Component[Unit, CtorType.Nullary] = ScalaFnComponent.apply[Unit] { _ =>
    <.tr(
      <.th(^.cls := "id hide-on-small-only center-align", "ID"),
      <.th(^.cls := "name", "Name"),
      <.th(^.cls := "description", "Description")
    )
  }

  val categoryComponent: Component[CategoryProps, CtorType.Props] = ScalaFnComponent.apply[CategoryProps] { props =>
    <.tr(
      <.td(^.cls := "id hide-on-small-only right-align", props.category.id.toString),
      <.td(
        ^.cls := "name",
        ^.onClick --> props.openEditModal(props.category),
        CategoryTree.name(props.category)
      ),
      <.td(^.cls := "description", props.category.description.getOrElse("").toString)
    )
  }


  val addCategoryModal: Component[FormProps, CtorType.Props] = ScalaFnComponent.withReuse[FormProps] { props =>
    <.form(
      <.div(
        ^.cls := "row",
        TextInput.component(TextInput.Props("add-category-name", "Name", props.name, props.changeName, 101, List("col", "s12")))
      ),
      <.div(
        ^.cls := "row",
        TextInput.component(
          TextInput.Props("add-category-desc", "Description", props.description, props.changeDescription, 102, List("col", "s12"))
        )
      ),
      <.div(
        ^.cls := "row",
        SftMain.dropDownCategoryTree.component(
          SftMain.dropDownCategoryTree.Props(
            "add-category-parent",
            "Parent",
            TopLevelCat :: props.availableParents,
            CategoryTree.name,
            cat => s"add-cat-${cat.id}",
            props.changeParent,
            Some(props.parent),
            103,
            List("col", "s12")
          )
        )
      ),
      ModalButtons(props.id.map(_ => "Save").getOrElse("Add"), 104, props.save, props.close)
    )
  }
}
