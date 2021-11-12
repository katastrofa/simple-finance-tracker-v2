package org.big.pete.sft.server.auth

import io.circe.generic.JsonCodec
import org.big.pete.sft.db.domain.{Login, User}
import org.http4s.Uri

object domain {
  case class AuthUser(dbUser: User, login: Login)
  case class LoginRedirect(uri: Uri)
  case class AuthCookieData(id: Int, authCode: String) {
    override def toString: String = s"$id$AuthCookieSeparator$authCode"
  }

  @JsonCodec
  case class GoogleTokenResponse(access_token: String, expires_in: Int, refresh_token: String, scope: String, token_type: String)
  @JsonCodec
  case class NameObject(displayName: String)
  @JsonCodec
  case class EmailObject(value: String)
  @JsonCodec
  case class PersonResponse(names: List[NameObject], emails: List[EmailObject])
}
