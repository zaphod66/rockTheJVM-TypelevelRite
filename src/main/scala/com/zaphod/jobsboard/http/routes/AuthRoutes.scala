package com.zaphod.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.implicits.*
import cats.effect.Concurrent
import org.http4s.{HttpRoutes, Request, Response, Status}
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import tsec.authentication.{SecuredRequestHandler, TSecAuthService, asAuthed}
import com.zaphod.jobsboard.core.Auth
import com.zaphod.jobsboard.domain.auth.*
import com.zaphod.jobsboard.domain.security.*
import com.zaphod.jobsboard.domain.user.{NewUserInfo, User}
import com.zaphod.jobsboard.http.responses.FailureResponse
import com.zaphod.jobsboard.http.validation.syntax.HttpValidationDsl

import java.time.Instant

class AuthRoutes[F[_]: Concurrent: Logger] private (auth: Auth[F]) extends HttpValidationDsl[F] {

  private val securedHandler: SecuredRequestHandler[F, String, User, JwtToken] = SecuredRequestHandler(auth.authenticator)

  // POST /auth/login { LoginInfo } => 200 Ok with Authorization: Bearer { jwt }
  private val login: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      val jwtTokenM = for {
        loginInfo <- req.as[LoginInfo]
        tokenM    <- auth.login(loginInfo.email, loginInfo.password)
        _         <- Logger[F].info(s"User logging in: ${loginInfo.email}")
      } yield tokenM

      jwtTokenM.map {
        case Some(token) => auth.authenticator.embed(Response(Status.Ok), token)  // Authorization: Bearer { jwt }
        case None        => Response(Status.Unauthorized)
      }
  }

  // POST /auth/users { NewUserInfo } => 201 Created
  private val createUser: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "users" =>
      for {
        newUserInfo <- req.as[NewUserInfo]
        newUserM    <- auth.signUp(newUserInfo)
        resp        <- newUserM match {
          case Some(user) => Created(user.email)
          case None       => BadRequest(s"User with email ${newUserInfo.email} already exists.")
        }
      } yield resp
  }

  // PUT /auth/users/password { NewPasswordInfo } { Authorization: Bearer { jwt } } => 200 Ok
  private val changePassword: AuthRoute[F] = {
    case req @ PUT -> Root / "users" / "password"  asAuthed user =>
      for {
        newPasswordInfo <- req.request.as[NewPasswordInfo]
        result <- auth.changePassword(user.email, newPasswordInfo)
        resp <- result match {
          case Right(Some(_)) => Ok()
          case Right(None)    => NotFound(FailureResponse(s"User ${user.email} not found."))
          case Left(err)      => Forbidden(err)
        }
      } yield resp
  }

  private val authenticator = auth.authenticator
  
  // POST /auth/logout { Authorization: Bearer { jwt } } => 200 Ok
  private val logout: AuthRoute[F] = {
    case req @ POST -> Root / "logout" asAuthed _ =>
      val token = req.authenticator
      for {
        _ <- authenticator.discard(token)
        resp <- Ok()
      } yield resp
  }

  private val unauthedRoutes = login <+> createUser
  private val authedRoutes   = securedHandler.liftService(
    TSecAuthService(changePassword.orElse(logout))
  )
  val routes: HttpRoutes[F] = Router(
    "/auth" -> (unauthedRoutes <+> authedRoutes)
  )
}

object AuthRoutes {
  def apply[F[_]: Concurrent: Logger](auth: Auth[F]) = new AuthRoutes[F](auth)
}
