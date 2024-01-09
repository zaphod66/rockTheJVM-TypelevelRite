package com.zaphod.jobsboard.core

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.*

import com.zaphod.jobsboard.config.TokenConfig
import com.zaphod.jobsboard.domain.user.*
import com.zaphod.jobsboard.fixtures.UsersFixture

class TokensSpec
  extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with UsersFixture
    with DatabaseSpec
{
  val initScript = "sql/recoverytokens.sql"
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val tokenConfig = TokenConfig(10000000L)

  "Tokens algebra" - {
    "should not create a token for a non-existing users" in {
      xaResource.use { xa =>
        val prog = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, tokenConfig)
          tokenM <- tokens.getToken("somebody@gmail.com")
        } yield tokenM

        prog.asserting(_ shouldBe None)
      }
    }

    "should create a token for an existing users" in {
      xaResource.use { xa =>
        val prog = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, tokenConfig)
          tokenM <- tokens.getToken(Norbert.email)
        } yield tokenM

        prog.asserting(_ shouldBe defined)
      }
    }

    "should not check for a token that is expired" in {
      xaResource.use { xa =>
        val prog = for {
          tokens  <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(1L))
          tokenM  <- tokens.getToken(Norbert.email)
          _       <- IO.sleep(10.millis)
          isValid <- tokenM match {
            case Some(token) => tokens.checkToken(Norbert.email, token)
            case None => IO.pure(false)
          }
        } yield isValid

        prog.asserting(_ shouldBe false)
      }
    }

    "should check for a token that is not expired" in {
      xaResource.use { xa =>
        val prog = for {
          tokens  <- LiveTokens[IO](mockedUsers)(xa, tokenConfig)
          tokenM  <- tokens.getToken(Norbert.email)
          isValid <- tokenM match {
            case Some(token) => tokens.checkToken(Norbert.email, token)
            case None => IO.pure(false)
          }
        } yield isValid

        prog.asserting(_ shouldBe true)
      }
    }

    "should only check a token for a user that generated them" in {
      xaResource.use { xa =>
        val prog = for {
          tokens  <- LiveTokens[IO](mockedUsers)(xa, tokenConfig)
          tokenM  <- tokens.getToken(Norbert.email)
          isValidNorbert <- tokenM match {
            case Some(token) => tokens.checkToken(Norbert.email, token)
            case None => IO.pure(false)
          }
          isValidNOther <- tokenM match {
            case Some(token) => tokens.checkToken("somebody@gmail.com", token)
            case None => IO.pure(false)
          }
        } yield (isValidNorbert, isValidNOther)

        prog.asserting(_ shouldBe (true, false))
      }
    }

  }
}
