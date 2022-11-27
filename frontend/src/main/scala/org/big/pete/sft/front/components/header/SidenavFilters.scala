package org.big.pete.sft.front.components.header

import japgolly.scalajs.react.callback.Callback
import japgolly.scalajs.react.{CtorType, ReactFormEventFromInput, Reusability, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.component.ScalaFn
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
      onTransactionTypeChange: (Status, String) => Callback,
      trackingActiveFilters: Set[TransactionTracking],
      onTrackingChange: (Status, String) => Callback,
      contentFilter: String,
      onContentFilterChange: ReactFormEventFromInput => Callback
  )
  case class CategoriesProps(
      categoriesActiveFilters: Set[Int],
      onCategoryFilterChange: (Status, String) => Callback,
      categoryTree: List[CategoryTree]
  )
  case class MoneyAccountProps(
      moneyAccountsActiveFilters: Set[Int],
      onMoneyAccountFilterChange: (Status, String) => Callback,
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

  val collapsibleHeaderComponent: ScalaFn.Component[CollapsibleHeaderProps, CtorType.Props] =
    ScalaFnComponent.withReuse[CollapsibleHeaderProps] { props =>
      val icon = if (props.hasActiveFilters) "toggle_on" else "toggle_off"
      <.a(^.href := "#!", ^.classSet("collapsible-header" -> true, "has-active-items" -> props.hasActiveFilters),
        ^.onClick ==> (_.preventDefaultCB >> props.onOpenFilter(props.section)),
        MaterialIcon(icon),
        props.text
      )
    }

  val transactionFiltersComponent: Component[(FiltersOpen => Callback, TransactionsProps), Unit, Unit, CtorType.Props] =
    ScalaComponent.builder[(FiltersOpen => Callback, TransactionsProps)]
      .stateless
      .render_P { case (onOpenFilter, props) =>

        def expandTransactionTypes(transactionType: TransactionType) =
          MICheckbox.component.withKey(s"ttf-${transactionType.toString}").apply(MICheckbox.Props(
            <.li(_: _*),
            Map.empty,
            transactionType.toString,
            transactionType.toString,
            Status.fromBoolean(props.transactionTypeActiveFilters.contains(transactionType)),
            props.onTransactionTypeChange
          ))

        def expandTracking(tracking: TransactionTracking) =
          MICheckbox.component.withKey(s"ttf-${tracking.toString}").apply(MICheckbox.Props(
            <.li(_: _*),
            Map.empty,
            tracking.toString,
            tracking.toString,
            Status.fromBoolean(props.trackingActiveFilters.contains(tracking)),
            props.onTrackingChange
          ))

        <.div(
          collapsibleHeaderComponent.apply(CollapsibleHeaderProps(
            props.transactionTypeActiveFilters.nonEmpty || props.trackingActiveFilters.nonEmpty || props.contentFilter.nonEmpty,
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
                TextInput("transactions-filter", "Content filter", props.contentFilter, props.onContentFilterChange)
              )
            )
          )
        )
      }.build

  val categoriesFilterComponent: Component[(FiltersOpen => Callback, CategoriesProps), Unit, Unit, CtorType.Props] =
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

        def mapCategory(cat: CategoryTree) = {
          MICheckbox.component.withKey(s"cf-${cat.id}").apply(MICheckbox.Props(
            <.li(_: _*),
            Map.empty,
            cat.id.toString,
            CategoryTree.name(cat),
            getStatus(cat),
            props.onCategoryFilterChange
          ))
        }

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

  val moneyAccountsFilterComponent: Component[(FiltersOpen => Callback, MoneyAccountProps), Unit, Unit, CtorType.Props] =
    ScalaComponent.builder[(FiltersOpen => Callback, MoneyAccountProps)]
      .stateless
      .render_P { case (onOpenFilter, props) =>
        def expandMoneyAccount(ma: EnhancedMoneyAccount) =
          MICheckbox.component.withKey(s"maf-${ma.id}").apply(MICheckbox.Props(
            <.li(_: _*),
            Map.empty,
            ma.id.toString,
            ma.name,
            Status.fromBoolean(props.moneyAccountsActiveFilters.contains(ma.id)),
            props.onMoneyAccountFilterChange
          ))

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
