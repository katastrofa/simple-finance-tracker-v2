package org.big.pete.tyrian.toolz

import cats.effect.IO
import io.circe.Decoder
import org.big.pete.BPJson
import org.big.pete.sft.domain.{Accounts, ApiRequest, ApiResponse, Categories, Currencies, Transactions, User, Wallet, Wallets}
import org.big.pete.sft.domain.Givens.given
import org.big.pete.tyrian.domain.Msg
import org.scalajs.dom.console
import tyrian.Cmd
import tyrian.http.{Body, Http, HttpError, Method, Request, Response}


object HttpHelper {
  enum ApiCalls {
    case ListWallets
    case AddWallet
    case EditWallet

    case ListTransactions
    
    case ListAccounts
    
    case ListCategories
    
    case ListCurrencies
    case Me
  }

  private case class ApiDef(
      method: Method,
      path: String,
      onResponse: Response => Msg
  )

  private final val ApiDefinitions: Map[ApiCalls, ApiDef] = Map(
    ApiCalls.ListWallets -> ApiDef(Method.Get, "wallets", onResponse[Wallets]),
    ApiCalls.AddWallet -> ApiDef(Method.Put, "wallets", onResponse[Wallet]),
    ApiCalls.EditWallet -> ApiDef(Method.Post, "wallets", onResponse[Wallet]),

    ApiCalls.ListTransactions -> ApiDef(Method.Get, "{permalink}/transactions?start={from}&end={to}", onResponse[Transactions]),
    
    ApiCalls.ListAccounts -> ApiDef(Method.Get, "{permalink}/accounts", onResponse[Accounts]),
    
    ApiCalls.ListCategories -> ApiDef(Method.Get, "{permalink}/categories", onResponse[Categories]),
    
    ApiCalls.ListCurrencies -> ApiDef(Method.Get, "currencies", onResponse[Currencies]),
    ApiCalls.Me -> ApiDef(Method.Get, "me", onResponse[User])
  )

  def apiCall(
      apiBase: String,
      apiCall: ApiCalls,
      data: Option[ApiRequest] = None,
      urlData: Map[String, String] = Map.empty
  ): Cmd[IO, Msg] = {
    console.log(s"apiCall: $apiCall")
    val apiDef = ApiDefinitions(apiCall)
    val path = urlData.foldLeft(apiDef.path) { case (acc, (key, value)) => acc.replace(s"{$key}", value) }
    val request = Request.apply(apiDef.method, s"$apiBase/$path", makeBody(data))

    Http.send(request, tyrian.http.Decoder[Msg](apiDef.onResponse, onError))
  }

  private val onError: HttpError => Msg =
    e => Msg.HttpError(e.toString)

  private def makeBody(data: Option[ApiRequest]): Body =
    data.map(obj => Body.PlainText(BPJson.write(obj), "application/json")).getOrElse(Body.Empty)


  private def onResponse[T <: ApiResponse : Decoder](response: Response): Msg = {
    BPJson.extract[T](response.body) match {
      case Left(err) =>
        Msg.HttpError(err)
      case Right(value) =>
        Msg.HttpSuccess(value)
    }
  }
}
