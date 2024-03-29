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

  case class Props(
      categories: List[CategoryTree],
      save: (Option[Int], String, String, Option[Int]) => Callback,
      delete: (Int, Option[Int], Option[Int]) => Callback
  )
  case class State(
      isOpen: Boolean,
      deleteIsOpen: Boolean,
      id: Option[Int],
      name: String,
      description: String,
      parent: CategoryTree,
      deleteId: Option[Int],
      shiftSubCatsTo: Int,
      shiftTransactionsTo: Int
  )
  case class CategoryProps(category: CategoryTree, openEditModal: CategoryTree => Callback, openDeleteModal: CategoryTree => Callback)
  final private val TopLevelCat: CategoryTree = CategoryTree(-42, "Top Level", None, 0, None, List.empty)
  final private val NoShiftTransactions: CategoryTree = CategoryTree(-50, "Delete Transactions", None, 0, None, List.empty)

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
  case class DeleteFormProps(
      categories: List[CategoryTree],
      deleteId: Int,
      shiftSubCatsTo: Int,
      shiftTransactionsTo: Int,
      changeShiftSubCats: CategoryTree => Callback,
      changeShiftTransactions: CategoryTree => Callback,
      deleteCategory: Callback,
      closeDeleteModal: Callback
  )

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("save", "delete")
  implicit val stateReuse: Reusability[State] = Reusability.derive[State]
  implicit val formPropsReuse: Reusability[FormProps] =
    Reusability.caseClassExcept[FormProps]("changeName", "changeDescription", "changeParent", "save", "close")
  implicit val deleteFormPropsReuse: Reusability[DeleteFormProps] =
    Reusability.caseClassExcept[DeleteFormProps]("changeShiftSubCats", "changeShiftTransactions", "deleteCategory", "closeDeleteModal")


  class Backend($: BackendScope[Props, State]) {

    def changeName(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(name = e.target.value))

    private def changeDescription(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(description = e.target.value))

    private def changeParent(cat: CategoryTree): Callback =
      $.modState(_.copy(parent = cat))

    private def changeShiftSubCats(cat: CategoryTree): Callback =
      $.modState(_.copy(shiftSubCatsTo = cat.id))

    def changeShiftTransactions(cat: CategoryTree): Callback =
      $.modState(_.copy(shiftTransactionsTo = cat.id))

    private def saveEdit: Callback = for {
      state <- $.state
      props <- $.props
      _ <- closeModal
      _ <- props.save(state.id, state.name, state.description, Some(state.parent.id))
    } yield ()

    def deleteCategory(): Callback = for {
      state <- $.state
      props <- $.props
      subCatsShift = if (state.shiftSubCatsTo == TopLevelCat.id) None else Some(state.shiftSubCatsTo)
      transactionsShift = if (state.shiftTransactionsTo == NoShiftTransactions.id) None else Some(state.shiftTransactionsTo)
      _ <- props.delete(state.deleteId.get, subCatsShift, transactionsShift)
      _ <- closeDeleteModal
    } yield ()

    def closeModal: Callback =
      $.modState(_.copy(isOpen = false))

    def closeDeleteModal: Callback =
      $.modState(_.copy(deleteIsOpen = false))

    def openAddNew: Callback = $.modState { state =>
      state.copy(isOpen = true, deleteIsOpen = false, None, "", "", TopLevelCat)
    }

    def openEditModal(cat: CategoryTree): Callback = $.props.flatMap { props =>
      val linearCats = CategoryTree.linearize(props.categories)
      val parent = cat.parent.flatMap(id => linearCats.find(_.id == id))
      $.modState(_.copy(isOpen = true, deleteIsOpen = false, Some(cat.id), cat.name, cat.description.getOrElse(""), parent.getOrElse(TopLevelCat)))
    }

    def openDeleteModal(cat: CategoryTree): Callback = $.modState { state =>
      state.copy(deleteIsOpen = true, deleteId = Some(cat.id), shiftSubCatsTo = TopLevelCat.id, shiftTransactionsTo = NoShiftTransactions.id)
    }

    private def expandCategories(excludeId: Option[Int])(cat: CategoryTree): List[ScalaFn.Unmounted[CategoryProps]] = {
      if (!excludeId.contains(cat.id))
        categoryComponent.withKey(s"cat-${cat.id}").apply(CategoryProps(cat, openEditModal, openDeleteModal)) ::
          cat.children.flatMap(expandCategories(excludeId))
      else
        Nil
    }

    def render(props: Props, state: State): html_<^.VdomTagOf[Element] = {
      val categoryLines = props.categories.flatMap(expandCategories(None)).toVdomArray

      tableWrap(
        "categories-table",
        List(
          AddModal.component(AddModal.Props("add-category-modal")) {
            addCategoryModal.apply(FormProps(
              CategoryTree.linearize(props.categories),
              state.id, state.name, state.description, state.parent,
              changeName, changeDescription, changeParent, saveEdit, closeModal
            ))
          }.when(state.isOpen),
          AddModal.component(AddModal.Props("delete-category-modal")) {
            deleteCategoryModal.apply(DeleteFormProps(
              props.categories, state.deleteId.getOrElse(-1), state.shiftSubCatsTo, state.shiftTransactionsTo,
              changeShiftSubCats, changeShiftTransactions, deleteCategory(), closeDeleteModal
            ))
          }.when(state.deleteIsOpen)
        ).toTagMod,
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
    .initialState[State](State(isOpen = false, deleteIsOpen = false, None, "", "", TopLevelCat, None, TopLevelCat.id, NoShiftTransactions.id))
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  val headerComponent: Component[Unit, CtorType.Nullary] = ScalaFnComponent.apply[Unit] { _ =>
    <.tr(
      <.th(^.cls := "id hide-on-small-only center-align", "ID"),
      <.th(^.cls := "name", "Name"),
      <.th(^.cls := "description hide-on-med-and-down", "Description"),
      <.th(^.cls := "delete", "")
    )
  }

  private val categoryComponent: Component[CategoryProps, CtorType.Props] = ScalaFnComponent.apply[CategoryProps] { props =>
    <.tr(
      <.td(^.cls := "id hide-on-small-only right-align", props.category.id.toString),
      <.td(
        ^.cls := "name pointer",
        ^.onClick --> props.openEditModal(props.category),
        CategoryTree.name(props.category)
      ),
      <.td(^.cls := "description hide-on-med-and-down", props.category.description.getOrElse("").toString),
      <.td(^.cls := "delete",
        MaterialIcon(MaterialIcon.i, MaterialIcon.small, "delete", props.openDeleteModal(props.category))
      )
    )
  }


  private val addCategoryModal: Component[FormProps, CtorType.Props] = ScalaFnComponent.withReuse[FormProps] { props =>
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

  private val deleteCategoryModal: Component[DeleteFormProps, CtorType.Props] = ScalaFnComponent.withReuse[DeleteFormProps] { props =>
    def shiftSubCatsExpand(cat: CategoryTree): List[CategoryTree] = {
      if (cat.id != props.deleteId)
        cat :: cat.children.flatMap(shiftSubCatsExpand)
      else
        List.empty
    }

    def shiftTransactionsExpand(cat: CategoryTree): List[CategoryTree] = {
      if (cat.id != props.deleteId)
        cat :: cat.children.flatMap(shiftTransactionsExpand)
      else
        cat.children.flatMap(shiftTransactionsExpand)
    }

    val shiftSubCatsList = (TopLevelCat :: props.categories).flatMap(shiftSubCatsExpand)
    val shiftTransactionsList = (NoShiftTransactions :: props.categories).flatMap(shiftTransactionsExpand)

    <.form(
      <.div(
        ^.cls := "row",
        SftMain.dropDownCategoryTree.component(
          SftMain.dropDownCategoryTree.Props(
            "del-shift-sub-cats",
            "Shift SubCats to:",
            shiftSubCatsList,
            cat => CategoryTree.name(cat),
            cat => s"del-subcat-${cat.id}",
            props.changeShiftSubCats,
            shiftSubCatsList.find(_.id == props.shiftSubCatsTo),
            150,
            List("col", "s12")
          )
        )
      ),
      <.div(
        ^.cls := "row",
        SftMain.dropDownCategoryTree.component(
          SftMain.dropDownCategoryTree.Props(
            "del-cat-shift-transactions",
            "Shift Transactions to:",
            shiftTransactionsList,
            cat => CategoryTree.name(cat),
            cat => s"del-transaction-shift-${cat.id}",
            props.changeShiftTransactions,
            shiftTransactionsList.find(_.id == props.shiftTransactionsTo),
            151,
            List("col", "s12")
          )
        )
      ),
      ModalButtons("Delete", 152, props.deleteCategory, props.closeDeleteModal)
    )
  }
}
