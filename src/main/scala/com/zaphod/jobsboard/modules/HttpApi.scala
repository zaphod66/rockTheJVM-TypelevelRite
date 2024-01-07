package com.zaphod.jobsboard.modules

import cats.data.OptionT
import cats.effect
import cats.effect.kernel.Async
import cats.effect.{Concurrent, Ref, Resource, Sync}
import cats.implicits.*
import com.zaphod.jobsboard.config.SecurityConfig
import com.zaphod.jobsboard.core.Users
import com.zaphod.jobsboard.domain.security.{Authenticator, JwtToken, SecuredHandler}
import com.zaphod.jobsboard.domain.user.User
import com.zaphod.jobsboard.http.routes.*
import com.zaphod.jobsboard.modules.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import tsec.authentication.{BackingStore, IdentityStore, JWTAuthenticator, SecuredRequestHandler}
import tsec.common.SecureRandomId
import tsec.mac.jca.HMACSHA256

class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F], authenticator: Authenticator[F]) {
  given securedHandler: SecuredHandler[F] = SecuredRequestHandler(authenticator)
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes = JobRoutes[F](core.jobs).routes
  private val authRoutes = AuthRoutes[F](core.auth, authenticator).routes

  val routes: HttpRoutes[F] = Router(
    "/api" -> (healthRoutes <+> jobRoutes <+> authRoutes)
  )
}

object HttpApi {

  def createAuthenticator[F[_]: Sync](users: Users[F], securityConfig: SecurityConfig): F[Authenticator[F]] = {

    // 1. identity store
    val idStore: IdentityStore[F, String, User] = (email: String) =>
      OptionT[F, User](users.find(email))

    // 2. backing store for JWT tokens: BackingStore[F, id, JwtToken]
    val tokenStoreF = Ref.of[F, Map[SecureRandomId, JwtToken]](Map.empty).map { ref =>
      new BackingStore[F, SecureRandomId, JwtToken] {
        override def get(id: SecureRandomId): OptionT[F, JwtToken] =
          OptionT(ref.get.map(_.get(id)))

        override def put(elem: JwtToken): F[JwtToken] =
          ref.modify(store => (store + (elem.id -> elem), elem))

        override def update(v: JwtToken): F[JwtToken] =
          put(v)

        override def delete(id: SecureRandomId): F[Unit] =
          ref.modify(store => (store - id, ()))
      }
    }

    // 3. key for hashing
    val keyF = HMACSHA256.buildKey[F](securityConfig.secret.getBytes("UTF-8"))

    // 4. authenticator
    for {
      key <- keyF
      tokenStore <- tokenStoreF
      authenticator = JWTAuthenticator.backed.inBearerToken(
        expiryDuration = securityConfig.jwtExpiryDuration, // expiration of tokens
        maxIdle = None, // max idle time (optional)
        tokenStore = tokenStore,
        identityStore = idStore, // identity store
        signingKey = key // hash key
      )
    } yield authenticator

  }
  def apply[F[_]: Async: Logger](core: Core[F], securityConfig: SecurityConfig): Resource[F, HttpApi[F]] =
    Resource
      .eval(createAuthenticator(core.users, securityConfig))
      .map(authenticator => new HttpApi[F](core, authenticator))
}
