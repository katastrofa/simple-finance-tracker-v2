package org.big.pete.datepicker

import cats.effect.SyncIO
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import japgolly.scalajs.react.{BackendScope, CtorType, ReactFormEventFromInput, ReactKeyboardEventFromInput, ReactMouseEventFromHtml, Ref, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.datepicker.parts.CalendarTable.{DayAttr, MonthAttr, YearAttr}
import org.big.pete.datepicker.parts.CalendarTable
import org.big.pete.react.MaterialIcon
import org.big.pete.react.Modal.{ModalState, ModalSupport, isOpenPath}
import org.scalajs.dom.html
import org.scalajs.dom.html.Div

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try


object ReactDatePicker {
  final val PartialDateRegex = "([0-9]{4})[^0-9-]*".r
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
      onSelect: LocalDate => SyncIO[Unit],
      initialDate: Option[LocalDate],
      isOpened: Boolean,
      keyBindings: KeyBindings
  )
  case class State(isOpen: Boolean, editing: Option[String], selected: LocalDate) extends ModalState

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
  case class KeyBinding(key: String, modifiers: List[String] = List.empty[String])

  class Backend(override val $: BackendScope[Props, State])(implicit val isOpenL: isOpenPath.Lens[State, Boolean])
    extends ModalSupport[Props, State]
  {
    private val inputRef = Ref[html.Input]

    def onSelectDate(onSelect: LocalDate => SyncIO[Unit])(e: ReactMouseEventFromHtml): SyncIO[Unit] = {
      val newSelected = LocalDate.of(
        e.target.getAttribute(YearAttr.attrName).toInt,
        e.target.getAttribute(MonthAttr.attrName).toInt,
        e.target.getAttribute(DayAttr.attrName).toInt
      )
      $.modState(_.copy(editing = None, selected = newSelected)) >> closeModal >> onSelect(newSelected)
    }

    def parseInputChange(e: ReactFormEventFromInput): SyncIO[Unit] = {
      $.modState { oldState =>
        var cleanDate = cleanText(e.target.value)
        val newSelected = if (PartialDateRegex.matches(cleanDate))
            preParseDate(cleanDate, oldState.selected)
          else
            oldState.selected

        if (cleanDate.length == 4 && "[0-9]{4}".r.matches(cleanDate)) cleanDate = cleanDate + "-"
        if (cleanDate.length == 7 && "[0-9]{4}-[0-9]{2}".r.matches(cleanDate)) cleanDate = cleanDate + "-"
        oldState.copy(editing = Some(cleanDate), selected = newSelected)
      }
    }

    private def cleanText(date: String): String =
      date.replaceAll("[^0-9-]", "")

    private def preParseDate(dateString: String, date: LocalDate): LocalDate = {
      val splits = dateString.split("-", 3)
      var newDate = Try(date.withYear(splits.head.toInt)).getOrElse(date)
      if (splits.length > 1) newDate = Try(newDate.withMonth(splits(1).toInt)).getOrElse(newDate)
      if (splits.length > 2) newDate = Try(newDate.withDayOfMonth(splits(2).toInt)).getOrElse(newDate)
      newDate
    }

    def fillInput(state: State): String =
      state.editing.getOrElse(formatDate(state.selected))

    def formatDate(date: LocalDate): String =
      date.format(DateFormat)

    def moveSelected(months: Int, days: Int): SyncIO[Unit] =
      inputRef.foreach(_.select()) >> $.modState {state =>
        state.copy(editing = None, selected = state.selected.plusMonths(months.toLong).plusDays(days.toLong))
      }

    def backspaceHandling(e: ReactKeyboardEventFromInput): SyncIO[Unit] = {
      e.key match {
        case "Backspace" =>
          val text = e.target.value
          if (text.length == 5 && "[0-9]{4}-".r.matches(text))
            e.preventDefaultCB >> e.stopPropagationCB >> $.modState(_.copy(editing = Some(text.substring(0, 3))))
          else if (text.length == 8 && "[0-9]{4}-[0-9]{2}-".r.matches(text))
            e.preventDefaultCB >> e.stopPropagationCB >> $.modState(_.copy(editing = Some(text.substring(0, 6))))
          else
            SyncIO.unit
        case _ =>
          SyncIO.unit
      }
    }

    def validateModifiers(e: ReactKeyboardEventFromInput, modifiers: List[String]): Boolean = {
      val unusedModifiers = AllModifiers -- modifiers
      modifiers.forall(e.getModifierState) && unusedModifiers.forall(str => !e.getModifierState(str))
    }

    def onInputKey(selected: LocalDate, initial: LocalDate, keys: KeyBindings, onSelect: LocalDate => SyncIO[Unit])(e: ReactKeyboardEventFromInput): SyncIO[Unit] = {
      if (e.key == "Enter")
        e.preventDefaultCB >> $.modState(_.copy(editing = None)) >> closeModal >> onSelect(selected)
      else if (e.key == "Escape")
        e.preventDefaultCB >> $.modState(_.copy(editing = None, selected = initial)) >> closeModal
      else if (e.key == keys.prevDay.key && validateModifiers(e, keys.prevDay.modifiers))
        e.stopPropagationCB >> moveSelected(0, -1)
      else if (e.key == keys.nextDay.key && validateModifiers(e, keys.nextDay.modifiers))
        e.stopPropagationCB >> moveSelected(0, 1)
      else if (e.key == keys.prevMonth.key && validateModifiers(e, keys.prevMonth.modifiers))
        e.stopPropagationCB >> moveSelected(-1, 0)
      else if (e.key == keys.nextMonth.key && validateModifiers(e, keys.nextMonth.modifiers))
        e.stopPropagationCB >> moveSelected(1, 0)
      else if (keys.prevWeek.exists(key => e.key == key.key && validateModifiers(e, key.modifiers)))
        e.stopPropagationCB >> moveSelected(0, -7)
      else if (keys.nextWeek.exists(key => e.key == key.key && validateModifiers(e, key.modifiers)))
        e.stopPropagationCB >> moveSelected(0, 7)
      else if (keys.prevYear.exists(key => e.key == key.key && validateModifiers(e, key.modifiers)))
        e.stopPropagationCB >> moveSelected(-12, 0)
      else if (keys.nextYear.exists(key => e.key == key.key && validateModifiers(e, key.modifiers)))
        e.stopPropagationCB >> moveSelected(12, 0)
      else
        SyncIO.unit
    }

    def render(prop: Props, state: State): VdomTagOf[Div] = {
      <.div(^.cls := "input-field",
        wrapContent(state, prop.id, Array("datepicker-modal"), Array("datepicker-container")) {
          <.div(^.cls := "datepicker-calendar-container",
            <.div(^.cls := "datepicker-calendar",
              <.div(^.id := s"datepicker-title-${prop.id}", ^.cls := "datepicker-controls", ^.role.heading, ^.aria.live.assertive,
                <.button(^.cls := "year-prev month-prev", ^.`type` := "button", ^.onClick --> moveSelected(-12, 0), MaterialIcon("keyboard_double_arrow_left")),
                <.button(^.cls := "month-prev", ^.`type` := "button", ^.onClick --> moveSelected(-1, 0), MaterialIcon("keyboard_arrow_left")),
                <.div(^.cls := "selects-container", <.h5(s"${state.selected.getYear}-${state.selected.getMonthValue}")),
                <.button(^.cls := "month-next", ^.`type` := "button", ^.onClick --> moveSelected(1, 0), MaterialIcon("keyboard_arrow_right")),
                <.button(^.cls := "year-next month-next", ^.`type` := "button", ^.onClick --> moveSelected(12, 0), MaterialIcon("keyboard_double_arrow_right"))
              ),
              CalendarTable.CalendarTable(CalendarTable.Props(prop.id, state.selected, onSelectDate(prop.onSelect)))
            )
          )
        },
        <.input(^.id := prop.id, ^.`type` := "text", ^.cls := "datepicker",
          ^.value := fillInput(state),
          ^.onFocus --> openModal,
          ^.onChange ==> parseInputChange,
          ^.onKeyDown ==> backspaceHandling,
          ^.onKeyUp ==> onInputKey(state.selected, prop.initialDate.getOrElse(LocalDate.now()), prop.keyBindings, prop.onSelect)
        ).withRef(inputRef)
      )
    }
  }

  val DatePicker: Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialStateFromProps(p => State(p.isOpened, None, p.initialDate.getOrElse(LocalDate.now())))
    .backend(new Backend(_))
    .renderBackend
    .build


  def apply(id: String, onSelect: LocalDate => SyncIO[Unit]): Unmounted[Props, State, Backend] =
    apply(id, onSelect, None, isOpened = false, DefaultKeyBindings)
  def apply(id: String, onSelect: LocalDate => SyncIO[Unit], initialDate: Option[LocalDate], isOpened: Boolean, keyBindings: KeyBindings): Unmounted[Props, State, Backend] =
    DatePicker.apply(Props(id, onSelect, initialDate, isOpened, keyBindings))
}
