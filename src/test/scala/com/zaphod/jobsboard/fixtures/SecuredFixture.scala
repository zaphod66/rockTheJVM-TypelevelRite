package com.zaphod.jobsboard.fixtures

import cats.data.OptionT
import cats.effect.IO
import com.zaphod.jobsboard.domain.security.{Authenticator, JwtToken, SecuredHandler}
import com.zaphod.jobsboard.domain.user.User
import org.http4s.{AuthScheme, Credentials, Request}
import org.http4s.headers.Authorization
import tsec.authentication.{IdentityStore, JWTAuthenticator, SecuredRequestHandler}
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256

import scala.concurrent.duration.*

trait SecuredFixture extends UsersFixture {
  val mockedAuthenticator: Authenticator[IO] = {
    // key for hashing
    val key = HMACSHA256.unsafeGenerateKey
    // identity store to retrieve users
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == Norbert.email) OptionT.pure(Norbert)
      else if (email == Jana.email) OptionT.pure(Jana)
      else OptionT.none[IO, User]
    // jwt authenticator
    JWTAuthenticator.unbacked.inBearerToken(
      1.day,    // expiration of tokens
      None,     // max idle time (optional)
      idStore,  // identity store
      key       // hash key
    )
  }

  extension (req: Request[IO])
    def withBearerToken(a: JwtToken): Request[IO] =
      req.putHeaders {
        val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](a.jwt)
        // Authorization: Bearer {jwt}
        Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
      }
      
  given securedHandler: SecuredHandler[IO] = SecuredRequestHandler(mockedAuthenticator)
}
