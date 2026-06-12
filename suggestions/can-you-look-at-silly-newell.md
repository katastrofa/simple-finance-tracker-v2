# Tyrian Frontend — Refactoring Suggestions

## Context

The Tyrian (Scala.js / Elm-architecture) frontend in the `tyrianFront` module is still under
construction. You asked for cleanup/shortening suggestions since front-end isn't your strong area.
This document is **suggestions only — no code changes**. Each item lists where it is, why it helps,
and a concrete before/after sketch so you can implement at your own pace (or hand back to me later).

Honest framing: the codebase is actually in decent shape. Component reuse (`DropDown`, `DatePicker`,
`ICheckbox`, `InputsBase`) is good, and the `updateDD` / `mkPassThrough` helpers in
`transactions/Page.scala` are already a nice deduplication. The wins below are mostly **view-layer
boilerplate** and a little **dead code** — readability, not architecture. A few things other reviewers
flag (icon overloads, the API-definitions map, Circe cookie codecs) are idiomatic and **not** worth
touching.

The transactions module is unfinished (`???` in several `update` arms; `Main.scala` still renders
`"Bla"` for it), so all view cleanups below are cosmetic/structural and won't change behavior.

---

## 0. Bug found while reviewing (not a refactor)

**`parts/Sidebar.scala:38-40`** — all three sidebar sublinks navigate to `Page.Transactions`:

```scala
walletPermalink.map(permalink => sidenavItem(active, Page.Transactions(permalink), "Transactions", "receipt_long")),
walletPermalink.map(permalink => sidenavItem(active, Page.Transactions(permalink), "Categories",   "category")),   // wrong page
walletPermalink.map(permalink => sidenavItem(active, Page.Transactions(permalink), "Accounts",     "local_atm"))   // wrong page
```

Categories and Accounts should use `Page.Categories(permalink)` and `Page.Accounts(permalink)`.
Flagging it because it's a correctness bug, not style. (Also dedupable — see item 5.)

---

## Recommended (high value, low risk)

### 1. Delete dead lenses in `Main.scala:40-47`
Eight `editXL` lenses are declared but **never referenced** — `update` uses `.copy()` and `.focus()`
directly. Also dead: commented `//  import Givens.given` (line 38) and the commented `subscriptions`
line (122). Just delete all of it. Pure subtraction, ~10 lines gone.

### 2. `Views.formRow` helper for modal fields — biggest win
`EditModal.scala:44-97` wraps every one of 11 fields in `<.div(^.cls := "row")(…)`; `WalletsPage.scala`
repeats it. Add one helper to `Views`:

```scala
def formRow[T](content: Html[T]): Html[T] = <.div(^.cls := "row")(content)
```

Then each field drops from 3 lines to 1:

```scala
// before
<.div(^.cls := "row")(
  DropDown.view(m.editOp, "Operation", 402, List("col", "s12")).map(Msg.EditOp(_))
),
// after
Views.formRow(DropDown.view(m.editOp, "Operation", 402, List("col", "s12")).map(Msg.EditOp(_))),
```

**Bonus — the 3 Transfer-only rows** (`EditModal.scala:73-90`) repeat
`m.editOp.selected.filter(_ == Op.Transfer).map { _ => … }.orEmpty`. Extract a local helper:

```scala
def whenTransfer(content: => Html[Msg]): Html[Msg] =
  m.editOp.selected.filter(_ == Op.Transfer).map(_ => content).orEmpty
```

Together these turn ~55 lines of the modal form into a clean, scannable list.

### 3. `detailRow` helper in `transactions/Item.scala:52-79`
Seven near-identical label/value blocks:

```scala
<.div(^.cls := "details-item row")(
  <.strong(^.cls := "col l2 s3")("Date:"),
  <.span(^.cls := "col l10 s9")(t.date.format(DateFormat))
),
```

Extract:

```scala
private def detailRow(label: String, value: String, valueCls: String = ""): <[Msg] =
  <.div(^.cls := "details-item row")(
    <.strong(^.cls := "col l2 s3")(label),
    <.span(^.cls := s"col l10 s9 $valueCls")(value)
  )
```

The amount row passes `amountCls(t)` as `valueCls`. Cuts ~28 lines to ~7. (The final
Edit/Delete button row stays as-is — it's structurally different.)

### 4. Add/Edit button label helper
`m.editing.map(_ => "Update" -> MBIcon.Edit).getOrElse("Add" -> MBIcon.Add)` is duplicated in
`EditModal.scala:41` and `WalletsPage.scala:110`. Add to `Views`:

```scala
def addEditLabel(editing: Option[?]): (String, MBIcon) =
  editing.map(_ => "Update" -> MBIcon.Edit).getOrElse("Add" -> MBIcon.Add)
```

Minor, but removes a copy-pasted ternary and keeps the two modals consistent.

### 5. Dedupe the sidebar sublink construction (fixes bug 0 too)
`Sidebar.scala:37-41` builds three sublinks by hand. Drive from a list:

```scala
val sublinks = List(
  (Page.Transactions(_: String), "Transactions", "receipt_long"),
  (Page.Categories(_: String),   "Categories",   "category"),
  (Page.Accounts(_: String),     "Accounts",     "local_atm")
)
// ...
walletPermalink.toList.flatMap { p =>
  sublinks.map { case (mkPage, text, icon) => sidenavItem(active, mkPage(p), text, icon) }
}
```

This both removes the repetition and makes the bug impossible to reintroduce.

---

## Optional (more invasive — your call)

### 6. Consolidate the repeated `given catDropDownSupport`
The exact one-liner
`given catDropDownSupport(using categories: Map[Int, Category]): DropDownItem[Category] = new CatDropDownItem`
is redefined 5×: `transactions/Page.scala` (121, 157, 250, 267) and `EditModal.scala:18`. Define it
**once** at module/`Givens` level as a `using`-parameterized given and import it; delete the four
local copies. Removes noise but touches several call sites, so verify each still resolves the implicit.

### 7. Share input `update` logic in `InputsBase`
`TextInput.update` and `MoneyTextBox.update` share four identical arms (`NoOp`, `TextChange`,
`ReceivedFocus`, `LostFocus`). Could lift the common cases into a `protected def baseUpdate` in
`InputsBase`. **Honest take: marginal** — only two components, and `MoneyTextBox`'s `LostFocus`/
`EnterPress` differ enough that the shared version needs a hook. Low payoff; skip unless more input
types are coming.

---

## Explicitly NOT recommended
- **`icon` overloads (`Views.scala:67-97`)** — idiomatic convenience API; collapsing to default params
  is awkward with the generic `T`/`onClick` and gains little.
- **`ApiDefinitions` map + `parseApiResponseAndUpdate` router** — already DRY; the "one edit per
  endpoint" is inherent, and several arms are still `???` (unfinished, not bloated).
- **Circe cookie codecs (`CookieStorage.scala`)** — standard `deriveEncoder`/`deriveDecoder` for two
  types. A macro/factory would be over-engineering.

---

## If/when you implement

There are no frontend unit tests, so verification is a compile of the JS bundle:

```bash
sbt "tyrianFront/fastOptJS::webpack"
```

Items 1–5 are independent and can be done/verified one at a time. Item 6 should be compiled carefully
(implicit resolution across files). All changes are confined to:
`tyrian-front/.../Main.scala`, `parts/Sidebar.scala`, `parts/WalletsPage.scala`,
`parts/transactions/{EditModal,Item,Page}.scala`, `toolz/Views.scala`,
`component/inputs/{InputsBase,TextInput,MoneyTextBox}.scala`.
