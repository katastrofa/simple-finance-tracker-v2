package org.big.pete.sft.domain

import org.scalatest.funsuite.AnyFunSuite
import io.circe.jawn.parse


class DomainTest extends AnyFunSuite {
  test("Test json decomposition") {
    import org.big.pete.sft.domain.Givens.given
    
    val json =
      """
        |{
        |  "global": ["Basic", "DeleteWallet"],
        |  "default": ["Basic", "ModifyOwnCategory", "DeleteCategory", "DeleteAccount", "DeleteTransactions"],
        |  "perWallet": {"1": ["Basic", "ModifyTransactions", "DeleteCategory", "DeleteAccount", "DeleteTransactions"]}
        |}
        |""".stripMargin
    val result = parse(json).flatMap(_.as[UserPermissions])
    println(result)
  }
}
