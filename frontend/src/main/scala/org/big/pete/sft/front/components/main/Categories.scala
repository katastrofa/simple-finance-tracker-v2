package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.component.{Scala, ScalaFn}
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, Reusability, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.{MaterialIcon, TextInput}
import org.big.pete.sft.front.SftMain
import org.big.pete.sft.front.domain.CategoryTree
import org.scalajs.dom.html.Div


object Categories {
  import org.big.pete.sft.front.domain.Implicits._

  case class Props(categories: List[CategoryTree], publish: (String, String, Option[Int]) => Callback)
  case class CategoryProps(category: CategoryTree)
  final val TopLevelCat: CategoryTree = CategoryTree(-42, "Top Level", None, 0, List.empty)

  case class ModalProps(
      isOpen: Boolean,
      categories: List[CategoryTree],
      publish: (String, String, Option[Int]) => Callback,
      close: Callback
  )
  case class ModalState(name: String, description: String, parent: CategoryTree)

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("publish")
  implicit val modalPropsReuse: Reusability[ModalProps] = Reusability.caseClassExcept[ModalProps]("publish", "close")
  implicit val modalStateReuse: Reusability[ModalState] = Reusability.derive[ModalState]


  val component: Scala.Component[Props, Boolean, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .initialState[Boolean](false)
    .renderPS { ($, props, isOpen) =>
      def expandCategories(cat: CategoryTree): List[ScalaFn.Unmounted[CategoryProps]] = {
        categoryComponent.withKey(s"cat-${cat.id}").apply(CategoryProps(cat)) ::
          cat.children.flatMap(expandCategories)
      }
      val categoryLines = props.categories.flatMap(expandCategories).toVdomArray

      tableWrap(
        addCategoryModal.apply(ModalProps(isOpen, props.categories, props.publish, $.modState(_ => false))),
        headerComponent(),
        categoryLines,
        headerComponent(),
        <.a(^.cls := "waves-effect waves-light btn nice",
          ^.onClick --> $.modState(_ => true),
          MaterialIcon("add"),
          "Add"
        )
      )
    }.configure(Reusability.shouldComponentUpdate)
    .build


  class ModalBackend($: BackendScope[ModalProps, ModalState]) {
    def changeName(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(name = e.target.value))

    def changeDescription(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(description = e.target.value))

    def changeParent(cat: CategoryTree): Callback =
      $.modState(_.copy(parent = cat))

    def clean: Callback =
      $.modState(_ => ModalState("", "", TopLevelCat))

    def publish: Callback = for {
      props <- $.props
      state <- $.state
      _ <- props.close
      _ <- props.publish(state.name, state.description, Some(state.parent.id))
      _ <- clean
    } yield ()


    def render(props: ModalProps, state: ModalState): VdomTagOf[Div] = {
      <.div(^.id := "add-category-modal",
        ^.tabIndex := 1,
        ^.classSet("modal" -> true, "open" -> props.isOpen),
        <.div(^.cls := "modal-content",
          <.div(^.cls := "container",
            <.form(
              <.div(^.cls := "row",
                TextInput.component(TextInput.Props("add-category-name", "Name", state.name, changeName, 101, List("col", "s12")))
              ),
              <.div(^.cls := "row",
                TextInput.component(TextInput.Props("add-category-desc", "Description", state.description, changeDescription, 102, List("col", "s12")))
              ),
              <.div(^.cls := "row",
                SftMain.dropDownCategoryTree.component(SftMain.dropDownCategoryTree.Props(
                  "add-category-parent",
                  "Parent",
                  TopLevelCat :: props.categories,
                  CategoryTree.name,
                  cat => s"add-cat-${cat.id}",
                  changeParent,
                  Some(state.parent)
                ))
              ),
              <.div(^.cls := "row",
                <.div(^.cls := "col s12 right-align",
                  <.button(^.cls := "waves-effect waves-light btn",
                    ^.`type` := "button",
                    ^.tabIndex := 103,
                    ^.onClick --> publish,
                    MaterialIcon("add_task"),
                    "Add"
                  ),
                  <.button(^.cls := "waves-effect waves-light btn",
                    ^.`type` := "button",
                    ^.tabIndex := 104,
                    ^.onClick --> (props.close >> clean),
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
  }

  val addCategoryModal: Scala.Component[ModalProps, ModalState, ModalBackend, CtorType.Props] = ScalaComponent.builder[ModalProps]
    .initialState(ModalState("", "", TopLevelCat))
    .renderBackend[ModalBackend]
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
      <.td(^.cls := "name", CategoryTree.name(props.category)),
      <.td(^.cls := "description", props.category.description.getOrElse("").toString)
    )
  }

}
