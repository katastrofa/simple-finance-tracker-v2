package org.big.pete.tyrian.sample

import org.big.pete.sft.domain.{ApiAction, User, UserPermissions}


object Data {
  val allApiActions = ApiAction.values.toSet
  
  val user = User(1, "my@example.com", "Big Pete", userPermissions)
  val userPermissions = UserPermissions(allApiActions, Map(1 -> allApiActions), allApiActions)
}
