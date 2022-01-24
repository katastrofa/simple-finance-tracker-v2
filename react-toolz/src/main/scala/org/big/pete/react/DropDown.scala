package org.big.pete.react

import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, ReactKeyboardEventFromInput, ReactMouseEventFromHtml, Reusability, ScalaComponent}
import japgolly.scalajs.react.component.Scala.{BackendScope, Component}
import japgolly.scalajs.react.extra.{EventListener, OnUnmount}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html.Div

import scala.annotation.{nowarn, tailrec}


@nowarn
class DropDown[T: Reusability] {
  case class Props(
      id: String,
      label: String,
      items: List[T],
      display: T => String,
      itemKey: T => String,
      onSelect: T => Callback,
      selected: Option[T],
      tabIndex: Int = -1,
      classes: List[String] = List.empty
  )
  case class State(text: String, focus: Boolean, filteredItems: List[T], traversing: Option[T])

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("onSelect", "display", "itemKey")
  implicit val stateReuse: Reusability[State] = Reusability.derive[State]


  /// TODO: css
  class Backend($: BackendScope[Props, State]) extends OnUnmount {

    def isActive(state: State): Boolean =
      state.text.nonEmpty || state.focus

    def focusIn: Callback = $.modState(_.copy(focus = true))
    def focusOut: Callback = $.modState(_.copy(focus = false))

    def dropDownOpen(state: State): Boolean =
      state.focus

    def prepareSearchables(searchString: String): List[String] =
      searchString.trim
        .split("\\s").toList
        .filter(_.nonEmpty)
        .map(_.toLowerCase)

    def filterItems(props: Props, searchString: String): List[T] = {
      def search(searchables: List[String])(itemText: String): Boolean =
        searchables.forall(searchable => itemText.toLowerCase.contains(searchable))

      val searchFn = search(prepareSearchables(searchString)) _
      props.items
        .filter(item => searchFn(props.display(item)))
    }

    def textChange(e: ReactFormEventFromInput): Callback = $.props.flatMap { props =>
      $.modState(_.copy(text = e.target.value, filteredItems = filterItems(props, e.target.value)))
    }

    def onSelect(props: Props, item: T): Callback =
      props.onSelect(item) >> $.modState(_.copy(props.display(item), focus = false, traversing = Some(item)))

    def onItemClick(item: T)(e: ReactMouseEventFromHtml): Callback = $.props.flatMap { props =>
      e.preventDefaultCB >> onSelect(props, item)
    }

    def onCancel(props: Props): Callback = {
      val newText = props.selected.map(props.display).getOrElse("")
      $.modState(_.copy(newText, focus = false, props.items, None))
    }

    def getCurrentTraversal(state: State): Option[T] = {
      if (state.traversing.isDefined)
        state.traversing
      else if (state.filteredItems.length == 1)
        state.filteredItems.headOption
      else
        None
    }

    def findClosestFilteredItem(direction: Int, item: T, allItems: List[T], filteredItems: List[T]): Option[T] = {
      val itemIndex = allItems.indexOf(item)
      val filterCondition = if (direction < 0) (x: Int) => x < itemIndex else (x: Int) => x > itemIndex
      val validIndexes = filteredItems.map(it => allItems.indexOf(it))
        .filter(filterCondition)

      direction match {
        case d if d < 0 && validIndexes.nonEmpty =>
          Some(allItems(validIndexes.last))
        case d if d < 0 && validIndexes.isEmpty =>
          Some(filteredItems.last)
        case d if d > 0 && validIndexes.nonEmpty =>
          Some(allItems(validIndexes.head))
        case _ =>
          Some(filteredItems.head)
      }
    }

    def moveTraversal(direction: Int): Callback = $.props.flatMap( props => $.modState { state =>
      if (state.filteredItems.isEmpty)
        state.copy(traversing = None)
      else
        state.traversing match {
          case Some(item) =>
            val index = state.filteredItems.indexOf(item)
            if (index < 0)
              state.copy(traversing = findClosestFilteredItem(direction, item, props.items, state.filteredItems))
            else {
              (index, direction) match {
                case (0, d) if d < 0 =>
                  state.copy(traversing = Some(state.filteredItems.last))
                case (i, d) if i >= state.filteredItems.length - 1 && d > 0 =>
                  state.copy(traversing = Some(state.filteredItems.head))
                case _ =>
                  state.copy(traversing = Some(state.filteredItems(index + direction)))
              }
            }
          case None =>
            if (direction < 0)
              state.copy(traversing = Some(state.filteredItems.last))
            else
              state.copy(traversing = Some(state.filteredItems.head))
        }
    })

    def onInputKey(e: ReactKeyboardEventFromInput): Callback = {
      val logKeyAndSelected = for {
        s <- $.state
        p <- $.props
        _ <- Callback.log(s"${e.key} - ${s.traversing.map(i => p.display(i))}")
      } yield ()

      if (e.key == "Enter") {
        for {
          _ <- e.preventDefaultCB
          _ <- logKeyAndSelected
          props <- $.props
          state <- $.state
          selected = getCurrentTraversal(state)
          _ <- if (selected.isDefined) onSelect(props, selected.get) else onCancel(props)
        } yield ()
      } else if (e.key == "Escape")
        e.preventDefaultCB >> logKeyAndSelected >> $.props >>= onCancel
      else if (e.key == "ArrowUp")
        e.preventDefaultCB >> logKeyAndSelected >> moveTraversal(-1)
      else if (e.key == "ArrowDown")
        e.preventDefaultCB >> logKeyAndSelected >> moveTraversal(1)
      else
        logKeyAndSelected >> Callback.empty
    }

    def render(props: Props, state: State): VdomTagOf[Div] = {
      def search(text: String)(str: String): (Int, Int) = {
        val index = text.toLowerCase.indexOf(str)
        index -> (index + str.length)
      }

      @tailrec
      def mergeRanges(current: (Int, Int), rest: List[(Int, Int)], acc: List[(Int, Int)]): List[(Int, Int)] = {
        rest match {
          case Nil => acc ++ List(current)
          case head :: tail =>
            if (current._2 < head._1)
              mergeRanges(head, tail, acc ++ List(current))
            else
              mergeRanges((current._1, Math.max(current._2, head._2)), tail, acc)
        }
      }

      @tailrec
      def splitText(highlights: List[(Int, Int)], text: String, acc: List[(String, Boolean)]): List[(String, Boolean)] = {
        highlights match {
          case Nil => if (text.isEmpty) acc else acc ++ List(text -> false)
          case head :: tail =>
            val parts = if (head._1 > 0)
              List(text.slice(0, head._1) -> false, text.slice(head._1, head._2) -> true)
            else
              List(text.slice(0, head._2) -> true)
            splitText(tail.map(r => (r._1 - head._2) -> (r._2 - head._2)), text.slice(head._2, text.length), acc ++ parts)
        }
      }

      def showItem(item: T) = {
        val text = props.display(item)
        val ranges = prepareSearchables(state.text)
          .map(search(text))
          .sortBy(_._1)
        val highlights = if (ranges.nonEmpty) mergeRanges(ranges.head, ranges.tail, List.empty) else List.empty
        val textSplits = splitText(highlights, text, List.empty).map {
          case (str, false) => TagMod(str)
          case (str, true) => <.span(^.cls := "highlight", str)
        }

        <.li(
          ^.classSet("active" -> state.traversing.contains(item)),
          ^.key := props.itemKey(item),
          ^.onClick ==> onItemClick(item),
          <.span(textSplits: _*)
        )
      }

      val ulHeight = (if (dropDownOpen(state)) state.filteredItems.length else 0) * 38

      <.div(^.cls := (List("input-field") ++ props.classes).mkString(" "),
        <.input(^.id := props.id, ^.`type` := "text", ^.tabIndex := props.tabIndex, ^.value := state.text, ^.cls := "autocomplete",
          ^.onChange ==> textChange,
          ^.onKeyDown ==> onInputKey
        ),
        <.ul(
          ^.classSet("autocomplete-content" -> true, "dropdown-content" -> true, "visible" -> dropDownOpen(state), "test" -> true),
          ^.height := s"${ulHeight}px",
          ^.tabIndex := 0,
          state.filteredItems.toVdomArray(showItem)
        ),
        <.label(^.`for` := props.id, ^.classSet("active" -> isActive(state)), props.label)
      )
    }
  }

  def component: Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialStateFromProps(props => State(props.selected.map(props.display).getOrElse(""), focus = false, props.items, props.selected))
    .renderBackend[Backend]
    .configure(EventListener.install("focusin", _.backend.focusIn))
    .configure(EventListener.install("focusout", _.backend.focusOut))
    .configure(Reusability.shouldComponentUpdate)
    .build
}
