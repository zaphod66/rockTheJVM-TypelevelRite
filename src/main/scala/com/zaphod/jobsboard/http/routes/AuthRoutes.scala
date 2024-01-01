package com.zaphod.jobsboard.http.routes

import cats.implicits.*
import cats.effect.Concurrent
import com.zaphod.jobsboard.core.Auth
import com.zaphod.jobsboard.http.validation.syntax.HttpValidationDsl
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

import java.time.Instant

class AuthRoutes[F[_]: Concurrent: Logger] private (auth: Auth[F]) extends HttpValidationDsl[F] {

  // POST /auth/login { LoginInfo } => 200 Ok with Authorization: Bearer { jwt }
  private val login: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root / "login" => Ok(s"ToDo login - ${Instant.now().toString}")
  }

  // POST /auth/users { NewUserInfo } => 201 Created
  private val createUser: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root / "users" => Ok(s"ToDo createUser - ${Instant.now().toString}")
  }

  // PUT /auth/users/password { NewPasswordInfo } { Authorization: Bearer { jwt } } => 200 Ok
  private val changePassword: HttpRoutes[F] = HttpRoutes.of[F] {
    case PUT -> Root / "users" / "password" => Ok(s"ToDo changePassword - ${Instant.now().toString}")
  }

  // POST /auth/logout { Authorization: Bearer { jwt } } => 200 Ok
  private val logout: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root / "logout" => Ok(s"ToDo logout - ${Instant.now().toString}")
  }

  val routes: HttpRoutes[F] = Router(
    "/auth" -> (login <+> createUser <+> changePassword <+> logout)
  )
}

object AuthRoutes {
  def apply[F[_]: Concurrent: Logger](auth: Auth[F]) = new AuthRoutes[F](auth)
}
