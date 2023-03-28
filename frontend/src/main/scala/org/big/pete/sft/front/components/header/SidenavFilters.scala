package org.big.pete.sft.front.components.header

import japgolly.scalajs.react.callback.Callback
import japgolly.scalajs.react.{CtorType, Reusability, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.component.ScalaFn
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.{MICheckbox, MaterialIcon, TextInput}
import org.big.pete.react.MICheckbox.Status
import org.big.pete.sft.domain.{EnhancedMoneyAccount, TransactionTracking, TransactionType}
import org.big.pete.sft.front.domain.CategoryTree


object SidenavFilters {
  case class Props(
      activeFilter: Option[FiltersOpen],
      onOpenFilter: FiltersOpen => Callback,
      transactions: TransactionsProps,
      categories: CategoriesProps,
      moneyAccounts: MoneyAccountProps
  )

  case class CollapsibleHeaderProps(hasActiveFilters: Boolean, text: String, section: FiltersOpen, onOpenFilter: FiltersOpen => Callback)
  case class TransactionsProps(
      transactionTypeActiveFilters: Set[TransactionType],
      onTransactionTypeChange: TransactionType => (Option[Status], Callback) => Callback,
      trackingActiveFilters: Set[TransactionTracking],
      onTrackingChange: TransactionTracking => (Option[Status], Callback) => Callback,
      contentFilter: StateSnapshot[String]
  )
  case class CategoriesProps(
      categoriesActiveFilters: Set[Int],
      onCategoryFilterChange: Int => (Option[Status], Callback) => Callback,
      categoryTree: List[CategoryTree]
  )
  case class MoneyAccountProps(
      moneyAccountsActiveFilters: Set[Int],
      onMoneyAccountFilterChange: Int => (Option[Status], Callback) => Callback,
      moneyAccounts: List[EnhancedMoneyAccount]
  )

  sealed trait FiltersOpen
  case object TransactionsFiltersOpen extends FiltersOpen
  case object CategoriesFiltersOpen extends FiltersOpen
  case object MoneyAccountFiltersOpen extends FiltersOpen

  implicit val filtersOpenReuse: Reusability[FiltersOpen] = Reusability.byRefOr_==[FiltersOpen]
  implicit val collapsibleHeaderPropsReuse: Reusability[CollapsibleHeaderProps] =
    Reusability.caseClassExcept("onOpenFilter")


  val component: Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      <.ul(^.cls := "collapsible",
        <.li(^.classSet("bold waves-effect" -> true, "active" -> props.activeFilter.contains(TransactionsFiltersOpen)),
          transactionFiltersComponent(props.onOpenFilter -> props.transactions)
        ),
        <.li(^.classSet("bold waves-effect" -> true, "active" -> props.activeFilter.contains(CategoriesFiltersOpen)),
          categoriesFilterComponent(props.onOpenFilter -> props.categories)
        ),
        <.li(^.classSet("bold waves-effect" -> true, "active" -> props.activeFilter.contains(MoneyAccountFiltersOpen)),
          moneyAccountsFilterComponent(props.onOpenFilter -> props.moneyAccounts)
        )
      )
    }.build

  private val collapsibleHeaderComponent: ScalaFn.Component[CollapsibleHeaderProps, CtorType.Props] =
    ScalaFnComponent.withReuse[CollapsibleHeaderProps] { props =>
      val icon = if (props.hasActiveFilters) "toggle_on" else "toggle_off"
      <.a(^.href := "#!", ^.classSet("collapsible-header" -> true, "has-active-items" -> props.hasActiveFilters),
        ^.onClick ==> (_.preventDefaultCB >> props.onOpenFilter(props.section)),
        MaterialIcon(icon),
        props.text
      )
    }

  private val transactionFiltersComponent: Component[(FiltersOpen => Callback, TransactionsProps), Unit, Unit, CtorType.Props] =
    ScalaComponent.builder[(FiltersOpen => Callback, TransactionsProps)]
      .stateless
      .render_P { case (onOpenFilter, props) =>

        def expandTransactionTypes(transactionType: TransactionType) = {
          val status = Status.fromBoolean(props.transactionTypeActiveFilters.contains(transactionType))
          MICheckbox.li.withKey(s"ttf-${transactionType.toString}")(MICheckbox.Props(
            transactionType.toString,
            transactionType.toString,
            StateSnapshot(status)(props.onTransactionTypeChange(transactionType))
          ))
        }

        def expandTracking(tracking: TransactionTracking) = {
          val status = Status.fromBoolean(props.trackingActiveFilters.contains(tracking))
          MICheckbox.li.withKey(s"ttf-${tracking.toString}")(MICheckbox.Props(
            tracking.toString,
            tracking.toString,
            StateSnapshot(status)(props.onTrackingChange(tracking))
          ))
        }

        <.div(
          collapsibleHeaderComponent.apply(CollapsibleHeaderProps(
            props.transactionTypeActiveFilters.nonEmpty || props.trackingActiveFilters.nonEmpty || props.contentFilter.value.nonEmpty,
            "Transactions",
            TransactionsFiltersOpen,
            onOpenFilter
          )),
          <.div(^.cls := "collapsible-body",
            <.ul(
              <.li(
                <.h6("Transaction types"),
                <.ul(TransactionType.values.map(expandTransactionTypes).toVdomArray)
              ),
              <.li(
                <.h6("Tracking"),
                <.ul(TransactionTracking.values.map(expandTracking).toVdomArray)
              ),
              <.li(
                <.h6("Content"),
                TextInput("transactions-filter", "Content filter", props.contentFilter)
              )
            )
          )
        )
      }.build

  private val categoriesFilterComponent: Component[(FiltersOpen => Callback, CategoriesProps), Unit, Unit, CtorType.Props] =
    ScalaComponent.builder[(FiltersOpen => Callback, CategoriesProps)]
      .stateless
      .render_P { case (onOpenFilter, props) =>
        def getStatus(cat: CategoryTree): Status = {
          def hasCheckedChild(catToCheck: CategoryTree): Boolean =
            catToCheck.children.exists { childCat =>
              props.categoriesActiveFilters.contains(childCat.id) || childCat.children.exists(hasCheckedChild)
            }

          if (props.categoriesActiveFilters.contains(cat.id))
            Status.checkedStatus
          else if (hasCheckedChild(cat))
            Status.indeterminate
          else
            Status.none
        }

        def mapCategory(cat: CategoryTree) =
          MICheckbox.li.withKey(s"cf-${cat.id}")(MICheckbox.Props(
            cat.id.toString,
            cat.shortDisplayName,
            StateSnapshot(getStatus(cat))(props.onCategoryFilterChange(cat.id))
          ))

        def expandCategory(cat: CategoryTree): List[CategoryTree] =
          cat :: cat.children.flatMap(expandCategory)

        <.div(
          collapsibleHeaderComponent.apply(CollapsibleHeaderProps(
            props.categoriesActiveFilters.nonEmpty,
            "Categories",
            CategoriesFiltersOpen,
            onOpenFilter
          )),
          <.div(^.cls := "collapsible-body",
            <.ul(props.categoryTree.flatMap(expandCategory).map(mapCategory).toVdomArray)
          )
        )
      }.build

  private val moneyAccountsFilterComponent: Component[(FiltersOpen => Callback, MoneyAccountProps), Unit, Unit, CtorType.Props] =
    ScalaComponent.builder[(FiltersOpen => Callback, MoneyAccountProps)]
      .stateless
      .render_P { case (onOpenFilter, props) =>
        def expandMoneyAccount(ma: EnhancedMoneyAccount) = {
          val status = Status.fromBoolean(props.moneyAccountsActiveFilters.contains(ma.id))
          MICheckbox.li.withKey(s"maf-${ma.id}")(MICheckbox.Props(
            ma.id.toString,
            ma.name,
            StateSnapshot(status)(props.onMoneyAccountFilterChange(ma.id))
          ))
        }

        <.div(
          collapsibleHeaderComponent.apply(CollapsibleHeaderProps(
            props.moneyAccountsActiveFilters.nonEmpty,
            "Money Account",
            MoneyAccountFiltersOpen,
            onOpenFilter
          )),
          <.div(^.cls := "collapsible-body",
            <.ul(props.moneyAccounts.map(expandMoneyAccount).toVdomArray)
          )
        )
      }.build
}
