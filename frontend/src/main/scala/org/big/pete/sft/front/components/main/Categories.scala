package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.component.{Scala, ScalaFn}
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.{CtorType, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.sft.front.domain.CategoryTree


object Categories {
  case class Props(categories: List[CategoryTree])
  case class CategoryProps(category: CategoryTree)

  val component: Scala.Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      def expandCategories(cat: CategoryTree): List[ScalaFn.Unmounted[CategoryProps]] = {
        categoryComponent.withKey(s"cat-${cat.id}").apply(CategoryProps(cat)) ::
          cat.children.flatMap(expandCategories)
      }
      val categoryLines = props.categories.flatMap(expandCategories).toVdomArray

      tableWrap(
        headerComponent(),
        categoryLines,
        headerComponent()
      )
    }.build

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
