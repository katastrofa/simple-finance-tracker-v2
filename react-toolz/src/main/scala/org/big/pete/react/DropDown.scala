package org.big.pete.react

import japgolly.scalajs.react.Ref.ToScalaComponent
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, ReactKeyboardEventFromInput, ReactMouseEventFromHtml, Reusability, ScalaComponent}
import japgolly.scalajs.react.component.Scala.{BackendScope, Component, Unmounted}
import japgolly.scalajs.react.extra.{EventListener, OnUnmount, StateSnapshot}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.DelayedInvocation
import org.big.pete.domain.DDItem
import org.scalajs.dom.html.Div

import scala.annotation.tailrec


object DropDown {
  case class Props(
      id: String,
      label: String,
      items: List[DDItem],
      selected: StateSnapshot[Option[DDItem]],
      tabIndex: Int = -1,
      classes: List[String] = List.empty,
      onEnterHit: Callback = Callback.empty
  )
  case class State(text: String, focus: Boolean, filteredItems: List[DDItem], traversing: Option[DDItem])

  implicit val ddItemReuse: Reusability[DDItem] = Reusability[DDItem] { (a: DDItem, b: DDItem) =>
    a.ddId == b.ddId && a.ddDisplayName == b.ddDisplayName
  }
  implicit val dropdownPropsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("onEnterHit")
  implicit val stateReuse: Reusability[State] = Reusability.derive[State]


  class Backend($: BackendScope[Props, State]) extends OnUnmount with WithInputFocus with DelayedInvocation {
    def focusIn: Callback =
      inputRef.foreach(_.select()) >> $.modState(_.copy(focus = true))
    def focusOut: Callback =
      executeWithDelay("focusout", 50, $.modState(_.copy(focus = false)))

    private def prepareSearchables(searchString: String): List[String] =
      searchString.trim
        .split("\\s").toList
        .filter(_.nonEmpty)
        .map(_.toLowerCase)

    private def filterItems(props: Props, searchString: String): List[DDItem] = {
      def search(searchables: List[String])(itemText: String): Boolean =
        searchables.forall(searchable => itemText.toLowerCase.contains(searchable))

      val searchFn = search(prepareSearchables(searchString)) _
      props.items
        .filter(item => searchFn(item.ddDisplayName))
    }

    private def textChange(e: ReactFormEventFromInput): Callback = $.props.flatMap { props =>
      $.modState(_.copy(text = e.target.value, filteredItems = filterItems(props, e.target.value)))
    }

    private def onSelect(props: Props, item: DDItem): Callback =
      cancelExecution("focusout") >>
        $.modState(_.copy(item.ddDisplayName, focus = false, traversing = None)) >>
        props.selected.setState(Some(item))

    private def onItemClick(item: DDItem)(e: ReactMouseEventFromHtml): Callback = $.props.flatMap { props =>
      e.preventDefaultCB >> onSelect(props, item)
    }

    private def onCancel(props: Props): Callback = {
      val newText = props.selected.value.map(_.ddDisplayName).getOrElse("")
      cancelExecution("focusout") >> $.modState(_.copy(newText, focus = false, props.items, None))
    }

    private def getCurrentTraversal(state: State): Option[DDItem] = {
      if (state.traversing.isDefined)
        state.traversing
      else if (state.filteredItems.length == 1)
        state.filteredItems.headOption
      else
        None
    }

    private def findClosestFilteredItem(
        direction: Int,
        item: DDItem,
        allItems: List[DDItem],
        filteredItems: List[DDItem]
    ): Option[DDItem] = {
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

    private def moveTraversal(direction: Int): Callback = $.props.flatMap(
      props => $.modState { state =>
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
      }
    )

    def onInputKey(e: ReactKeyboardEventFromInput): Callback = {
      if (e.key == "Enter") {
        for {
          props <- $.props
          state <- $.state
          selected = getCurrentTraversal(state)
          _ <- if (selected.isDefined) onSelect(props, selected.get) else onCancel(props)
          _ <- props.onEnterHit
        } yield ()
      } else if (e.key == "Escape")
        e.preventDefaultCB >> $.props >>= onCancel
      else if (e.key == "ArrowUp")
        e.preventDefaultCB >> moveTraversal(-1)
      else if (e.key == "ArrowDown")
        e.preventDefaultCB >> moveTraversal(1)
      else
        Callback.empty
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

      def showItem(item: DDItem) = {
        val text = item.ddDisplayName
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
          ^.key := s"${props.id}-${item.ddId}",
          ^.onClick ==> onItemClick(item),
          <.span(textSplits: _*)
        )
      }

      val ulHeight = (if (state.focus) state.filteredItems.length else 0) * 38

      <.div(
        ^.cls := (List("input-field") ++ props.classes).mkString(" "),
        <.input(
          ^.id := props.id, ^.`type` := "text", ^.tabIndex := props.tabIndex, ^.value := state.text, ^.cls := "autocomplete",
          ^.onChange ==> textChange,
          ^.onKeyDown ==> onInputKey
        ).withRef(inputRef),
        <.ul(
          ^.classSet(
            "autocomplete-content" -> true, "dropdown-content" -> true, "visible" -> state.focus, "test" -> true
          ),
          ^.height := s"${ulHeight}px",
          ^.tabIndex := 0,
          state.filteredItems.toVdomArray(showItem)
        ),
        <.label(^.`for` := props.id, ^.classSet("active" -> (state.text.nonEmpty || state.focus)), props.label)
      )
    }
  }

  def component: Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialStateFromProps(props => State(props.selected.value.map(_.ddDisplayName).getOrElse(""), focus = false, props.items, props.selected.value))
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.cancelExecutions())
    .configure(EventListener.install("focusin", _.backend.focusIn))
    .configure(EventListener.install("focusout", _.backend.focusOut))
    .configure(Reusability.shouldComponentUpdate)
    .build

  private def transitionSetState[T <: DDItem](selected: StateSnapshot[Option[T]])(item: Option[Option[DDItem]], fn: Callback): Callback =
    selected.setState(item.flatMap(_.map(_.asInstanceOf[T])), fn)


  def apply[T <: DDItem](
      id: String,
      label: String,
      items: List[T],
      selected: StateSnapshot[Option[T]],
      tabIndex: Int = -1,
      classes: List[String] = List.empty,
      onEnterHit: Callback = Callback.empty
  ): Unmounted[Props, State, Backend] = {
    val newSnapshot = StateSnapshot.withReuse.prepare[Option[DDItem]](transitionSetState(selected)).apply(selected.value)
    component.apply(Props(id, label, items, newSnapshot, tabIndex, classes, onEnterHit))
  }

  def withRef[T <: DDItem](ref: ToScalaComponent[Props, State, Backend])(
      id: String,
      label: String,
      items: List[T],
      selected: StateSnapshot[Option[T]],
      tabIndex: Int = -1,
      classes: List[String] = List.empty,
      onEnterHit: Callback = Callback.empty
  ): Unmounted[Props, State, Backend] = {
    val newSnapshot = StateSnapshot.withReuse.prepare[Option[DDItem]](transitionSetState(selected)).apply(selected.value)
    component.withRef(ref).apply(Props(id, label, items, newSnapshot, tabIndex, classes, onEnterHit))
  }
}
