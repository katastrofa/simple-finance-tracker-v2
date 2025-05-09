package org.big.pete.sft.server.auth

import io.circe.Decoder
import io.circe.generic.semiauto.*
import org.big.pete.sft.db.domain.Login
import org.big.pete.sft.domain.User
import org.http4s.Uri


object domain {
  case class AuthUser(db: User, login: Login)
  case class LoginRedirect(uri: Uri)
  case class AuthCookieData(id: Int, authCode: String) {
    override def toString: String = s"$id$AuthCookieSeparator$authCode"
  }
  
  case class GoogleTokenResponse(access_token: String, expires_in: Int, id_token: String, scope: String, token_type: String)
  case class NameObject(displayName: String, familyName: String, givenName: String)
  case class EmailObject(value: String)
  case class PersonResponse(names: List[NameObject], emailAddresses: List[EmailObject])

  object Implicits {
    given googleTokenResponseDecoder: Decoder[GoogleTokenResponse] = deriveDecoder
    given nameObjectDecoder: Decoder[NameObject] = deriveDecoder
    given emailObjectDecoder: Decoder[EmailObject] = deriveDecoder
    given personResponseDecoder: Decoder[PersonResponse] = deriveDecoder
  }
}
