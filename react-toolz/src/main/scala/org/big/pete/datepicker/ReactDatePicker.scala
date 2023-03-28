package org.big.pete.datepicker

import japgolly.scalajs.react.Ref.ToScalaComponent
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import japgolly.scalajs.react.extra.{EventListener, OnUnmount, StateSnapshot}
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ReactFormEventFromInput, ReactKeyboardEventFromInput, Ref, Reusability, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.datepicker.parts.CalendarTable
import org.big.pete.react.{HasFocus, MaterialIcon, Modal}
import org.scalajs.dom.{FocusEvent, document, html}
import org.scalajs.dom.html.Div

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try


object ReactDatePicker {
  case class KeyBinding(key: String, modifiers: List[String] = List.empty[String])
  case class KeyBindings(
      prevDay: KeyBinding,
      nextDay: KeyBinding,
      prevMonth: KeyBinding,
      nextMonth: KeyBinding,
      prevWeek: Option[KeyBinding] = None,
      nextWeek: Option[KeyBinding] = None,
      prevYear: Option[KeyBinding] = None,
      nextYear: Option[KeyBinding] = None
  )

  private final val PartialDateRegex = "([0-9]{4})[^0-9-]*".r
  final val DefaultKeyBindings = KeyBindings(
    KeyBinding("ArrowUp"),
    KeyBinding("ArrowDown"),
    KeyBinding("ArrowUp", List("Alt")),
    KeyBinding("ArrowDown", List("Alt"))
  )
  final val ExtendedKeyBindings = KeyBindings(
    KeyBinding("ArrowLeft"),
    KeyBinding("ArrowRight"),
    KeyBinding("ArrowLeft", List("Alt")),
    KeyBinding("ArrowRight", List("Alt")),
    Some(KeyBinding("ArrowUp")),
    Some(KeyBinding("ArrowDown")),
    Some(KeyBinding("ArrowUp", List("Alt"))),
    Some(KeyBinding("ArrowDown", List("Alt")))
  )
  final val DateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  final val AllModifiers = Set("Alt", "Control", "Fn", "Meta", "Shift")

  case class Props(
      id: String,
      date: StateSnapshot[LocalDate],
      classes: Set[String] = Set.empty,
      tabIndex: Int = -1,
      keyBindings: KeyBindings = ExtendedKeyBindings,
      onEnterHit: Callback = Callback.empty
  )
  case class State(
      isOpen: Boolean,
      editing: Option[String],
      selected: Option[LocalDate],
      insideFlag: Boolean
  )

  implicit val keyBindingReuse: Reusability[KeyBinding] = Reusability.derive[KeyBinding]
  implicit val keyBindingsReuse: Reusability[KeyBindings] = Reusability.derive[KeyBindings]
  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("onEnterHit")
  implicit val stateReuse: Reusability[State] = Reusability.derive[State]


  class Backend($: BackendScope[Props, State]) extends HasFocus with OnUnmount {
    private val inputRef = Ref[html.Input]

    override def focus: Callback =
      inputRef.foreach(_.select())

    def closeModal: Callback =
      $.modState(_.copy(isOpen = false))

    def openModal: Callback = {
      $.modState(_.copy(isOpen = true, insideFlag = true)) >> inputRef.foreach(_.select()) >>
        $.modStateAsync(_.copy(insideFlag = false)).delayMs(400).toCallback
    }

    def stopPropagation(evt: FocusEvent): Callback =
      Callback(evt.stopPropagation())

    def evtCancel: Callback = {
      $.state.flatMap { state =>
        if (state.insideFlag) $.modState(_.copy(insideFlag = false)) else cancel
      }.async.delayMs(100).toCallback
    }

    private def cancel: Callback = for {
      _ <- $.modState(_.copy(editing = None, selected = None))
      _ <- closeModal
    } yield ()

    def confirm(date: LocalDate): Callback = for {
      _ <- closeModal
      props <- $.props
      _ <- props.date.setState(date)
    } yield ()

    private def dateSelect(dateOpt: Option[LocalDate], fn: Callback): Callback =
      dateOpt.map(confirm).getOrElse(Callback.empty) >> fn

    private def parseInputChange(e: ReactFormEventFromInput): Callback = $.props.flatMap { props =>
      $.modState { oldState =>
        var cleanDate = cleanText(e.target.value)
        val date = oldState.selected.getOrElse(props.date.value)
        val newSelected = if (PartialDateRegex.matches(cleanDate)) preParseDate(cleanDate, date) else None

        if (cleanDate.length == 4 && "[0-9]{4}".r.matches(cleanDate)) cleanDate = cleanDate + "-"
        if (cleanDate.length == 7 && "[0-9]{4}-[0-9]{2}".r.matches(cleanDate)) cleanDate = cleanDate + "-"
        oldState.copy(editing = Some(cleanDate), selected = newSelected)
      }
    }

    private def cleanText(date: String): String =
      date.replaceAll("[^0-9-]", "")

    private def preParseDate(dateString: String, date: LocalDate): Option[LocalDate] = {
      val splits = dateString.split("-", 3)
      var newDate = Try(date.withYear(splits.head.toInt)).toOption
      if (splits.length > 1) newDate = newDate.flatMap(d => Try(d.withMonth(splits(1).toInt)).toOption)
      if (splits.length > 2) newDate = newDate.flatMap(d => Try(d.withDayOfMonth(splits(2).toInt)).toOption)
      newDate
    }

    private def formatDate(date: LocalDate): String =
      date.format(DateFormat)

    private def moveSelected(props: Props, months: Int, days: Int): Callback =
      inputRef.foreach(_.select()) >> $.modState {state =>
        state.copy(
          editing = None,
          selected = Some(state.selected.getOrElse(props.date.value).plusMonths(months.toLong).plusDays(days.toLong))
        )
      }

    private def backspaceHandling(e: ReactKeyboardEventFromInput): Callback = {
      e.key match {
        case "Backspace" =>
          val text = e.target.value
          if (text.length == 5 && "[0-9]{4}-".r.matches(text))
            e.preventDefaultCB >> e.stopPropagationCB >> $.modState(_.copy(editing = Some(text.substring(0, 3))))
          else if (text.length == 8 && "[0-9]{4}-[0-9]{2}-".r.matches(text))
            e.preventDefaultCB >> e.stopPropagationCB >> $.modState(_.copy(editing = Some(text.substring(0, 6))))
          else
            Callback.empty
        case _ =>
          Callback.empty
      }
    }

    private def validateModifiers(e: ReactKeyboardEventFromInput, modifiers: List[String]): Boolean = {
      val unusedModifiers = AllModifiers -- modifiers
      modifiers.forall(e.getModifierState) && unusedModifiers.forall(str => !e.getModifierState(str))
    }

    def onInputKey(e: ReactKeyboardEventFromInput): Callback = for {
      props <- $.props
      state <- $.state
      _ <- {
        if (e.key == "Enter")
          confirm(state.selected.getOrElse(props.date.value)) >> props.onEnterHit
        else if (e.key == "Escape")
          e.preventDefaultCB >> cancel
        else if (e.key == props.keyBindings.prevDay.key && validateModifiers(e, props.keyBindings.prevDay.modifiers))
          e.stopPropagationCB >> moveSelected(props, 0, -1)
        else if (e.key == props.keyBindings.nextDay.key && validateModifiers(e, props.keyBindings.nextDay.modifiers))
          e.stopPropagationCB >> moveSelected(props, 0, 1)
        else if (e.key == props.keyBindings.prevMonth.key && validateModifiers(e, props.keyBindings.prevMonth.modifiers))
          e.stopPropagationCB >> moveSelected(props, -1, 0)
        else if (e.key == props.keyBindings.nextMonth.key && validateModifiers(e, props.keyBindings.nextMonth.modifiers))
          e.stopPropagationCB >> moveSelected(props, 1, 0)
        else if (props.keyBindings.prevWeek.exists(key => e.key == key.key && validateModifiers(e, key.modifiers)))
          e.stopPropagationCB >> moveSelected(props, 0, -7)
        else if (props.keyBindings.nextWeek.exists(key => e.key == key.key && validateModifiers(e, key.modifiers)))
          e.stopPropagationCB >> moveSelected(props, 0, 7)
        else if (props.keyBindings.prevYear.exists(key => e.key == key.key && validateModifiers(e, key.modifiers)))
          e.stopPropagationCB >> moveSelected(props, -12, 0)
        else if (props.keyBindings.nextYear.exists(key => e.key == key.key && validateModifiers(e, key.modifiers)))
          e.stopPropagationCB >> moveSelected(props, 12, 0)
        else
          Callback.empty
      }
    } yield ()

    def render(props: Props, state: State): VdomTagOf[Div] = {
      val selected = state.selected.getOrElse(props.date.value)
      <.div(
        ^.classSet1M("input-field", props.classes.map(s => s -> true).toMap),
        Modal.modalDiv(state.isOpen, props.id, Array("datepicker-modal"), Array("datepicker-container")) {
          <.div(^.cls := "datepicker-calendar-container", ^.id := s"date-picker-container-div-${props.id}",
            <.div(^.cls := "datepicker-calendar",
              <.div(^.id := s"datepicker-title-${props.id}", ^.cls := "datepicker-controls", ^.role.heading, ^.aria.live.assertive,
                <.button(^.cls := "year-prev month-prev", ^.`type` := "button", ^.onClick --> moveSelected(props, -12, 0), MaterialIcon("keyboard_double_arrow_left")),
                <.button(^.cls := "month-prev", ^.`type` := "button", ^.onClick --> moveSelected(props, -1, 0), MaterialIcon("keyboard_arrow_left")),
                <.div(^.cls := "selects-container", <.h5(s"${selected.getYear}-${selected.getMonthValue}")),
                <.button(^.cls := "month-next", ^.`type` := "button", ^.onClick --> moveSelected(props, 1, 0), MaterialIcon("keyboard_arrow_right")),
                <.button(^.cls := "year-next month-next", ^.`type` := "button", ^.onClick --> moveSelected(props, 12, 0), MaterialIcon("keyboard_double_arrow_right"))
              ),
              CalendarTable(props.id, StateSnapshot.withReuse.prepare[LocalDate](dateSelect)(selected))
            )
          )
        },
        <.input(
          ^.id := props.id,
          ^.`type` := "text",
          ^.cls := "datepicker",
          ^.tabIndex := props.tabIndex,
          ^.value := state.editing.getOrElse(formatDate(selected)),
          ^.onChange ==> parseInputChange,
          ^.onKeyDown ==> backspaceHandling,
          ^.onKeyUp ==> onInputKey
        ).withRef(inputRef)
      )
    }
  }

  val DatePicker: Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialState(State(isOpen = false, None, None, insideFlag = false))
    .backend(new Backend(_))
    .renderBackend
    .configure(EventListener.install("focusin", _.backend.openModal))
    .configure(EventListener[FocusEvent].install(
      "focusout",
      _.backend.stopPropagation,
      comp => document.getElementById(s"date-picker-container-div-${comp.props.id}"))
    ).configure(EventListener.install("focusout", _.backend.evtCancel))
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(
      id: String,
      date: StateSnapshot[LocalDate],
      classes: Set[String] = Set.empty,
      tabIndex: Int = -1,
      keyBindings: KeyBindings = ExtendedKeyBindings,
      onEnterHit: Callback = Callback.empty
  ): Unmounted[Props, State, Backend] =
    DatePicker.apply(Props(id, date, classes, tabIndex, keyBindings, onEnterHit))

  def withRef(ref: ToScalaComponent[Props, State, Backend])(
      id: String,
      date: StateSnapshot[LocalDate],
      classes: Set[String] = Set.empty,
      tabIndex: Int = -1,
      keyBindings: KeyBindings = ExtendedKeyBindings,
      onEnterHit: Callback = Callback.empty
  ) =
    DatePicker.withRef(ref).apply(Props(id, date, classes, tabIndex, keyBindings, onEnterHit))

}
