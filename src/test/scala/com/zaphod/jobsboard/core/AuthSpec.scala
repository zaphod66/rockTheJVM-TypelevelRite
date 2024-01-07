package com.zaphod.jobsboard.core

import cats.data.OptionT
import cats.effect.IO
import cats.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.zaphod.jobsboard.config.SecurityConfig
import com.zaphod.jobsboard.domain.auth.NewPasswordInfo
import com.zaphod.jobsboard.domain.user.{NewUserInfo, Role, User}
import com.zaphod.jobsboard.domain.security.Authenticator
import com.zaphod.jobsboard.fixtures.UsersFixture
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash

import scala.concurrent.duration.*

class AuthSpec
  extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with UsersFixture {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val mockedUsers: Users[IO] = new Users[IO] {
    override def find(email: String): IO[Option[User]] =
      if (email == Norbert.email) IO.pure(Some(Norbert))
      else IO.pure(Option.empty[User])
    override def create(user: User): IO[String] = IO.pure(user.email)
    override def update(user: User): IO[Option[User]] = IO.pure(Some(user))
    override def delete(email: String): IO[Boolean] = IO.pure(true)
  }

  private val mockedAuthF = LiveAuth[IO](mockedUsers)
  "Auth 'algebra'" - {
    "login should return None, if user doesn't exist" in {
      val prog = for {
        auth <- mockedAuthF
        token <- auth.login("none@home.com", "password")
      } yield token

      prog.asserting(_ shouldBe None)
    }

    "login should return None, if user exists but password is wrong" in {
      val prog = for {
        auth  <- mockedAuthF
        token <- auth.login(Norbert.email, "wrong password")
      } yield token

      prog.asserting(_ shouldBe None)
    }

    "login should return token, if user exists" in {
      val prog = for {
        auth  <- mockedAuthF
        token <- auth.login(Norbert.email, "password1")
      } yield token

      prog.asserting(_ shouldBe defined)
    }

    "signup should return none, if user exists" in {
      val prog = for {
        auth <- mockedAuthF
        user <- auth.signUp(NewUserInfo(Norbert.email, Norbert.hashedPassword, None, None, None))
      } yield user

      prog.asserting(_ shouldBe None)
    }

    "signup should return a user , if user doesn't exist" in {
      val prog = for {
        auth <- mockedAuthF
        user <- auth.signUp(NewUserInfo(NewUser.email, NewUser.hashedPassword, NewUser.firstName, NewUser.lastName, NewUser.company))
      } yield user

      prog.asserting {
        case Some(user) =>
          user.email     shouldBe NewUser.email
          user.firstName shouldBe NewUser.firstName
          user.lastName  shouldBe NewUser.lastName
          user.company   shouldBe NewUser.company
          user.role      shouldBe Role.RECRUITER
        case _ => fail()
      }
    }

    "changePassword should return Right(None), if user doesn't exist" in {
      val prog = for {
        auth   <- mockedAuthF
        result <- auth.changePassword(NewUser.email, NewPasswordInfo("oldpw", "newpw"))
      } yield result

      prog.asserting(_ shouldBe Right(None))
    }

    "changePassword should return Left(Error), if user exists but password is wrong" in {
      val prog = for {
        auth   <- mockedAuthF
        result <- auth.changePassword(Norbert.email, NewPasswordInfo("oldpw", "newpw"))
      } yield result

      prog.asserting(_ shouldBe Left("Invalid password"))
    }

    "changePassword should change password, if details are correct" in {
      val plainPassword = "scalarocks"
      val prog = for {
        auth    <- mockedAuthF
        result  <- auth.changePassword(Norbert.email, NewPasswordInfo("password1", plainPassword))
        changed <- result match {
          case Right(Some(user)) =>
            BCrypt.checkpwBool[IO](
              plainPassword,
              PasswordHash[BCrypt](user.hashedPassword)
            )
          case _ => IO.pure(false)
        }
      } yield changed

      prog.asserting(_ shouldBe true)
    }
  }
}
