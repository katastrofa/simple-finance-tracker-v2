package org.big.pete.sft.front.components.header

import cats.effect.SyncIO
import japgolly.scalajs.react.{CtorType, ReactFormEventFromInput, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.component.ScalaFn
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.MICheckbox
import org.big.pete.react.MICheckbox.Status
import org.big.pete.sft.domain.{EnhancedMoneyAccount, TransactionTracking, TransactionType}
import org.big.pete.sft.front.domain.CategoryTree


object SidenavFilters {
  case class Props(
      activeFilter: Option[FiltersOpen],
      onOpenFilter: FiltersOpen => SyncIO[Unit],
      transactions: TransactionsProps,
      categories: CategoriesProps,
      moneyAccounts: MoneyAccountProps
  )

  case class CollapsibleHeaderProps(text: String, section: FiltersOpen, onOpenFilter: FiltersOpen => SyncIO[Unit])
  case class TransactionsProps(
      transactionTypeActiveFilters: Set[TransactionType],
      onTransactionTypeChange: (Status, String) => SyncIO[Unit],
      trackingActiveFilters: Set[TransactionTracking],
      onTrackingChange: (Status, String) => SyncIO[Unit],
      contentFilter: String,
      onContentFilterChange: ReactFormEventFromInput => SyncIO[Unit]
  )
  case class CategoriesProps(
      categoriesActiveFilters: Set[Int],
      onCategoryFilterChange: (Status, String) => SyncIO[Unit],
      categoryTree: List[CategoryTree]
  )
  case class MoneyAccountProps(
      moneyAccountsActiveFilters: Set[Int],
      onMoneyAccountFilterChange: (Status, String) => SyncIO[Unit],
      moneyAccounts: List[EnhancedMoneyAccount]
  )

  sealed trait FiltersOpen
  case object TransactionsFiltersOpen extends FiltersOpen
  case object CategoriesFiltersOpen extends FiltersOpen
  case object MoneyAccountFiltersOpen extends FiltersOpen


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
    ScalaFnComponent.apply[CollapsibleHeaderProps] { props =>
      <.a(^.cls := "collapsible-header", ^.href := "#!",
        ^.onClick ==> (_.preventDefaultCB >> props.onOpenFilter(props.section)),
        props.text
      )
    }

  val transactionFiltersComponent: Component[(FiltersOpen => SyncIO[Unit], TransactionsProps), Unit, Unit, CtorType.Props] =
    ScalaComponent.builder[(FiltersOpen => SyncIO[Unit], TransactionsProps)]
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
          collapsibleHeaderComponent.apply(CollapsibleHeaderProps("Transactions", TransactionsFiltersOpen, onOpenFilter)),
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
                <.div(^.cls := "input-field",
                  <.input(^.`type` := "text",
                    ^.id := "transactions-filter",
                    ^.placeholder := "filter",
                    ^.value := props.contentFilter,
                    ^.onChange ==> props.onContentFilterChange
                  ),
                  <.label(^.`for` := "transactions-filter", "Content filter")
                )
              )
            )
          )
        )
      }.build

  val categoriesFilterComponent: Component[(FiltersOpen => SyncIO[Unit], CategoriesProps), Unit, Unit, CtorType.Props] =
    ScalaComponent.builder[(FiltersOpen => SyncIO[Unit], CategoriesProps)]
      .stateless
      .render_P { case (onOpenFilter, props) =>
        def generateName(cat: CategoryTree): String =
          "-".repeat(cat.treeLevel) + " " + cat.name

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

        def expandCategory(cat: CategoryTree) =
          MICheckbox.component.withKey(s"cf-${cat.id}").apply(MICheckbox.Props(
            <.li(_: _*),
            Map.empty,
            cat.id.toString,
            generateName(cat),
            getStatus(cat),
            props.onCategoryFilterChange
          ))

        <.div(
          collapsibleHeaderComponent.apply(CollapsibleHeaderProps("Categories", CategoriesFiltersOpen, onOpenFilter)),
          <.div(^.cls := "collapsible-body",
            <.ul(props.categoryTree.map(expandCategory).toVdomArray)
          )
        )
      }.build

  val moneyAccountsFilterComponent: Component[(FiltersOpen => SyncIO[Unit], MoneyAccountProps), Unit, Unit, CtorType.Props] =
    ScalaComponent.builder[(FiltersOpen => SyncIO[Unit], MoneyAccountProps)]
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
          collapsibleHeaderComponent.apply(CollapsibleHeaderProps("Money Account", MoneyAccountFiltersOpen, onOpenFilter)),
          <.div(^.cls := "collapsible-body",
            <.ul(props.moneyAccounts.map(expandMoneyAccount).toVdomArray)
          )
        )
      }.build
}
