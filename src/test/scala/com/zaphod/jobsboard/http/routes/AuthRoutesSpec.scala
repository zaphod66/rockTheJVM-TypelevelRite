package com.zaphod.jobsboard.http.routes

import cats.data.OptionT
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.*
import com.zaphod.jobsboard.core.Auth
import com.zaphod.jobsboard.domain.auth.{LoginInfo, NewPasswordInfo}
import com.zaphod.jobsboard.domain.security.{Authenticator, JwtToken}
import com.zaphod.jobsboard.domain.user.{NewUserInfo, User}
import com.zaphod.jobsboard.fixtures.{SecuredFixture, UsersFixture}
import com.zaphod.jobsboard.util.Syntax.*
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.headers.Authorization
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256

import scala.concurrent.duration.*

class AuthRoutesSpec
  extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with Http4sDsl[IO]
  with SecuredFixture
  with UsersFixture
{
  ///////////////////////////////////////////
  // mocks
  ///////////////////////////////////////////


  val mockedAuth: Auth[IO] = new Auth[IO] {
    override def login(email: String, password: String): IO[Option[JwtToken]] =
      if (email == Norbert.email && password == PlainPasswordNorbert)
        mockedAuthenticator.create(email).map(Option(_))
      else
        IO.pure(None)

    override def signUp(newUser: NewUserInfo): IO[Option[User]] =
      if (newUser.email == Jana.email)
        IO.pure(Option(Jana))
      else
        IO.pure(None)

    override def delete(email: String): IO[Boolean] =
      if (email == Norbert.email) IO.pure(true)
      else IO.pure(false)

    override def changePassword(
                        email: String,
                        newPasswordInfo: NewPasswordInfo
                      ): IO[Either[String, Option[User]]] =
      if (email == Norbert.email)
        if (newPasswordInfo.oldPassword == PlainPasswordNorbert)
          IO.pure(Right(Option(Norbert)))
        else
          IO.pure(Left("invalid password"))
      else
        IO.pure(Right(None))

    override def authenticator: Authenticator[IO] = mockedAuthenticator
  }

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val authRoutes: HttpRoutes[IO] = AuthRoutes[IO](mockedAuth).routes
  private val authRoutesNF               = authRoutes.orNotFound

  ///////////////////////////////////////////
  // tests
  ///////////////////////////////////////////

  "AuthRoutes" - {
    "login should return a 401 if login fails" in {
      for {
        resp <- authRoutesNF.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(Norbert.email, "wrongPassword"))
        )
      } yield {
        // assertions here
        resp.status shouldBe Status.Unauthorized
      }
    }

    "login should return a 200 and a JWT if login is successful" in {
      for {
        resp <- authRoutesNF.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(Norbert.email, "password1"))
        )
      } yield {
        // assertions here
        resp.status shouldBe Status.Ok
        resp.headers.get(ci"Authorization") shouldBe defined
      }
    }

    "createUser should return a 400 if the user already exists" in {
      for {
        resp <- authRoutesNF.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserInfoNorbert)
        )
      } yield {
        // assertions here
        resp.status shouldBe Status.BadRequest
      }
    }

    "createUser should return a 201 if the user was created" in {
      for {
        resp <- authRoutesNF.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserInfoJana)
        )
      } yield {
        // assertions here
        resp.status shouldBe Status.Created
      }
    }

    "logout should return a 401 if logging out w/o a valid JWT token" in {
      for {
        resp <- authRoutesNF.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
        )
      } yield {
        // assertions here
        resp.status shouldBe Status.Unauthorized
      }
    }

    "logout should return a 200 if logging out with valid JWT token" in {
      for {
        jwtToken <- mockedAuthenticator.create(Norbert.email)
        resp <- authRoutesNF.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
            .withBearerToken(jwtToken)
        )
      } yield {
        // assertions here
        resp.status shouldBe Status.Ok
      }
    }

    // change password - user doesn't exist => 404 - Not Found
    "changePassword should return a 404 for nonexistent user" in {
      for {
        jwtToken <- mockedAuthenticator.create(Jana.email)
        resp <- authRoutesNF.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(PlainPasswordJana, "newPassword"))
        )
      } yield {
        // assertions here
        resp.status shouldBe Status.NotFound
      }
    }

    // change password - invalid old password => 403 - forbidden
    "changePassword should return a 403 for invalid old password" in {
      for {
        jwtToken <- mockedAuthenticator.create(Norbert.email)
        resp <- authRoutesNF.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo("wrongPassword" , "newPassword"))
        )
      } yield {
        // assertions here
        resp.status shouldBe Status.Forbidden
      }
    }

    // change password - JWT is invalid => 401
    "changePassword should return a 401 for invalid JWT token" in {
      for {
        resp <- authRoutesNF.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withEntity(NewPasswordInfo("wrongPassword" , "newPassword"))
        )
      } yield {
        // assertions here
        resp.status shouldBe Status.Unauthorized
      }
    }

    // change password - happy path => 200
    "changePassword should return a 200 on happy path" in {
      for {
        jwtToken <- mockedAuthenticator.create(Norbert.email)
        resp <- authRoutesNF.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(PlainPasswordNorbert , "newPassword"))
        )
      } yield {
        // assertions here
        resp.status shouldBe Status.Ok
      }
    }

    "delete should return a 401 if non-admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(Jana.email)
        resp <- authRoutesNF.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/jana@home.com")
            .withBearerToken(jwtToken)
        )
      } yield {
        // assertions here
        resp.status shouldBe Status.Unauthorized
      }
    }

    "delete should return a 200 if an admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(Norbert.email)
        resp <- authRoutesNF.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/norbert@home.com")
            .withBearerToken(jwtToken)
        )
      } yield {
        // assertions here
        resp.status shouldBe Status.Ok
      }
    }
  }
}
