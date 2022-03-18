package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.component.{Scala, ScalaFn}
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, Reusability, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.{MaterialIcon, TextInput}
import org.big.pete.sft.front.SftMain
import org.big.pete.sft.front.domain.CategoryTree
import org.big.pete.sft.front.helpers.{AddModal, ModalButtons}
import org.scalajs.dom.html.Form


object Categories {
  import org.big.pete.sft.front.domain.Implicits._

  case class Props(categories: List[CategoryTree], publish: (String, String, Option[Int]) => Callback)
  case class CategoryProps(category: CategoryTree)
  final val TopLevelCat: CategoryTree = CategoryTree(-42, "Top Level", None, 0, List.empty)

  case class FormProps(
      categories: List[CategoryTree],
      publish: (String, String, Option[Int]) => Callback,
      close: Callback
  )
  case class FormState(name: String, description: String, parent: CategoryTree)

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("publish")
  implicit val formPropsReuse: Reusability[FormProps] = Reusability.caseClassExcept[FormProps]("publish", "close")
  implicit val formStateReuse: Reusability[FormState] = Reusability.derive[FormState]


  val component: Scala.Component[Props, Boolean, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .initialState[Boolean](false)
    .renderPS { ($, props, isOpen) =>
      def expandCategories(cat: CategoryTree): List[ScalaFn.Unmounted[CategoryProps]] = {
        categoryComponent.withKey(s"cat-${cat.id}").apply(CategoryProps(cat)) ::
          cat.children.flatMap(expandCategories)
      }
      val categoryLines = props.categories.flatMap(expandCategories).toVdomArray

      tableWrap(
        AddModal.component(AddModal.Props("add-category-modal", isOpen))(
          addCategoryModal.apply(FormProps(props.categories, props.publish, $.modState(_ => false)))
        ),
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


  class FormBackend($: BackendScope[FormProps, FormState]) {
    def changeName(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(name = e.target.value))

    def changeDescription(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(description = e.target.value))

    def changeParent(cat: CategoryTree): Callback =
      $.modState(_.copy(parent = cat))

    def clean: Callback =
      $.modState(_ => FormState("", "", TopLevelCat))

    def publish: Callback = for {
      props <- $.props
      state <- $.state
      _ <- props.close
      _ <- props.publish(state.name, state.description, Some(state.parent.id))
      _ <- clean
    } yield ()


    def render(props: FormProps, state: FormState): VdomTagOf[Form] = {
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
            Some(state.parent),
            103,
            List("col", "s12")
          ))
        ),
        ModalButtons.add(104, publish, props.close >> clean)
      )
    }
  }

  val addCategoryModal: Scala.Component[FormProps, FormState, FormBackend, CtorType.Props] = ScalaComponent.builder[FormProps]
    .initialState(FormState("", "", TopLevelCat))
    .renderBackend[FormBackend]
    .configure(Reusability.shouldComponentUpdate)
    .build

}
