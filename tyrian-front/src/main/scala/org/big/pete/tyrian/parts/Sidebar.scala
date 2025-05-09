package org.big.pete.tyrian.parts

import org.big.pete.sft.domain.User
import org.big.pete.tyrian.domain.{Msg, Page}
import org.big.pete.tyrian.toolz.Views
import tyrian.Html as h


final case class SidebarModel(
    isOpen: Boolean
)

object Sidebar {

  type Model = SidebarModel

  def init(): Model =
    SidebarModel(false)
  
  def view(m: Model, page: Page, user: User): h[Msg] = {
    val sidenavClasses = List("sidenav", "sidenav-fixed") ++ (if (m.isOpen) List("open") else List.empty)
    val walletPermalink = Page.walletPermalink(page)

    h.ul(h.id := "sidenav-left", h.cls := sidenavClasses.mkString(" "))(
      List(
        Some(topNavigation(page, user.displayName, walletPermalink)),
        if (walletPermalink.isDefined) Some(FiltersLi) else None,
        if (walletPermalink.isDefined) Some(filters()) else None
      ).flatten
    )
  }

  private def topNavigation(active: Page, displayName: String, walletPermalink: Option[String]): h[Msg] = {
    h.li(h.id := "top-navigation")(
      h.ul(h.cls := "collection with-header")(
        List(
          Some(sidenavItem(active, Page.Wallets, displayName, "account_balance")),
          walletPermalink.map(permalink => sidenavItem(active, Page.Transactions(permalink), "Transactions", "receipt_long")),
          walletPermalink.map(permalink => sidenavItem(active, Page.Transactions(permalink), "Categories", "category")),
          walletPermalink.map(permalink => sidenavItem(active, Page.Transactions(permalink), "Accounts", "local_atm"))
        ).flatten
      )
    )
  }
  
  private def sidenavItem(active: Page, gen: Page, text: String, icon: String): h[Msg] = {
    val classes = List(
      Some(if (gen == Page.Wallets) "collection-header" else "collection-item"),
      if (active == gen) Some("active") else None
    )

    h.li(h.cls := classes.mkString(" "))(
      h.a(h.href := gen.permalink)(
        h.h5(h.text(text), Views.icon(icon, List("left")))
      )
    )
  }
  
  private final val FiltersLi = h.li(
    h.h6("Filters"),
    Views.icon("filter_list", List("left"))
  )

  private def filters(): h[Msg] = {
    h.li(h.id := "filters")("TODO")
  }
  
}
