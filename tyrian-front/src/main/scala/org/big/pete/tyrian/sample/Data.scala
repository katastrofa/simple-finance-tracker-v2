package org.big.pete.tyrian.sample

import org.big.pete.sft.domain.{ApiAction, User, UserPermissions, Wallet}


object Data {
  private val allApiActions = ApiAction.values.toSet

  val userPermissions: UserPermissions = UserPermissions(allApiActions, Map(1 -> allApiActions), allApiActions)
  val user: User = User(1, "my@example.com", "Big Pete", userPermissions)
  
  val wallets: List[Wallet] = List(
    Wallet(1, "Personal", "personal", Some(1)),
    Wallet(2, "Joint", "joint", Some(1)),
  )
  
}
