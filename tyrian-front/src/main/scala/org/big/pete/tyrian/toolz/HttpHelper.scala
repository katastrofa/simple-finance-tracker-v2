package org.big.pete.tyrian.toolz

import cats.effect.IO
import io.circe.Decoder
import org.big.pete.BPJson
import org.big.pete.sft.domain.{ApiRequest, ApiResponse, Wallet}
import org.big.pete.sft.domain.Givens.given
import org.big.pete.tyrian.domain.Msg
import tyrian.Cmd
import tyrian.http.{Body, Http, HttpError, Method, Request, Response}


object HttpHelper {
  enum ApiCalls {
    case AddWallet
    case EditWallet
  }

  private case class ApiDef(
      method: Method,
      path: String,
      onResponse: Response => Msg
  )

  private final val ApiDefinitions: Map[ApiCalls, ApiDef] = Map(
    ApiCalls.AddWallet -> ApiDef(Method.Put, "wallets", onResponse[Wallet]),
    ApiCalls.EditWallet -> ApiDef(Method.Post, "wallets", onResponse[Wallet])
  )

  def apiCall(apiBase: String, apiCall: ApiCalls, data: Option[ApiRequest]): Cmd[IO, Msg] = {
    val apiDef = ApiDefinitions(apiCall)
    val request = Request.apply(apiDef.method, s"$apiBase/${apiDef.path}", makeBody(data))
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
