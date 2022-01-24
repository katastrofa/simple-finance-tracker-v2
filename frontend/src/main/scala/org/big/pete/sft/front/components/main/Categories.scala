package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.component.{Scala, ScalaFn}
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.{MaterialIcon, TextInput}
import org.big.pete.sft.front.domain.CategoryTree


object Categories {
  case class Props(categories: List[CategoryTree], publish: (String, String, Option[Int]) => Callback)
  case class CategoryProps(category: CategoryTree)

  case class ModalProps(isOpen: Boolean, publish: (String, String, Option[Int]) => Callback, close: Callback)

  val component: Scala.Component[Props, Boolean, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .initialState[Boolean](false)
    .renderPS { ($, props, isOpen) =>
      def expandCategories(cat: CategoryTree): List[ScalaFn.Unmounted[CategoryProps]] = {
        categoryComponent.withKey(s"cat-${cat.id}").apply(CategoryProps(cat)) ::
          cat.children.flatMap(expandCategories)
      }
      val categoryLines = props.categories.flatMap(expandCategories).toVdomArray

      tableWrap(
        addModal.apply(ModalProps(isOpen, props.publish, $.modState(_ => false))),
        headerComponent(),
        categoryLines,
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
    .useState[Option[Int]](None)
    .render { (props: ModalProps, name: Hooks.UseState[String], description: Hooks.UseState[String], parent: Hooks.UseState[Option[Int]]) =>
      def changeName(e: ReactFormEventFromInput): Callback =
        name.modState(_ => e.target.value)
      def changeDescription(e: ReactFormEventFromInput): Callback =
        description.modState(_ => e.target.value)

      def clean: Callback =
        name.modState(_ => "") >> description.modState(_ => "") >> parent.modState(_ => None)
      def publish: Callback =
        props.close >> props.publish(name.value, description.value, parent.value) >> clean

      <.div(^.id := "add-category-modal",
        ^.tabIndex := 1,
        ^.classSet("modal" -> true, "open" -> props.isOpen),
        <.div(^.cls := "modal-content",
          <.div(^.cls := "container",
            <.form(
              <.div(^.cls := "row",
                TextInput.component(TextInput.Props("add-category-name", "Name", name.value, changeName, 101, List("col", "s12")))
              ),
              <.div(^.cls := "row",
                TextInput.component(TextInput.Props("add-category-desc", "Description", description.value, changeDescription, 102, List("col", "s12")))
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
