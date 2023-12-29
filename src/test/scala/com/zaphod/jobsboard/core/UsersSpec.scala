package com.zaphod.jobsboard.core

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import com.zaphod.jobsboard.domain.user.*
import com.zaphod.jobsboard.fixtures.UsersFixture
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import doobie.implicits.*
import org.postgresql.util.PSQLException
import org.scalatest.Inside

class UsersSpec
  extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Inside
    with UsersFixture
    with DatabaseSpec
{
  override val initScript: String = "sql/users.sql"

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Users algebra" - {
    "should find a user by email" in {
      xaResource.use { xa =>
        val prog = for {
          users <- LiveUsers[IO](xa)
          found <- users.find("jana@home.com")
        } yield found

        //        prog.asserting(_ => succeed)
        prog.asserting(_ shouldBe Some(Jana))
      }
    }

    "should return None if email doesn't exist" in {
      xaResource.use { xa =>
        val prog = for {
          users <- LiveUsers[IO](xa)
          found <- users.find("unknown@home.com")
        } yield found

        prog.asserting(_ shouldBe None)
      }
    }

    "should create a new user" in {
      xaResource.use { xa =>
        val prog = for {
          users  <- LiveUsers[IO](xa)
          userId <- users.create(NewUser)
          user   <- sql"SELECT * FROM users WHERE email=${NewUser.email}"
            .query[User]
            .option
            .transact(xa)
        } yield (userId, user)

        prog.asserting {
          case (userId, user) =>
            userId shouldBe NewUser.email
            user shouldBe Some(NewUser)
        }
      }
    }

    "should fail creating a user if it already exists" in {
      xaResource.use { xa =>
        val prog = for {
          users <- LiveUsers[IO](xa)
          userId <- users.create(Norbert).attempt // IO[Either[Throwable, String]]
        } yield userId

        prog.asserting { outcome =>
          inside(outcome) {
            case Left(e) => e shouldBe a[PSQLException]
            case _ => fail()
          }
        }
      }
    }

    "should return a None when updating a nonexistend user" in {
      xaResource.use { xa =>
        val prog = for {
          users <- LiveUsers[IO](xa)
          userId <- users.update(NewUser)
        } yield userId

        prog.asserting(_ shouldBe None)
      }
    }

    "should update an existend user" in {
      xaResource.use { xa =>
        val prog = for {
          users <- LiveUsers[IO](xa)
          userId <- users.update(NewJana)
        } yield userId

        prog.asserting(_ shouldBe Some(NewJana))
      }
    }
  }

  "should delete a user" in {
    xaResource.use { xa =>
      val prog = for {
        users  <- LiveUsers[IO](xa)
        result <- users.delete(Norbert.email)
        user   <- sql"SELECT * FROM users WHERE email=${Norbert.email}"
          .query[User]
          .option
          .transact(xa)
      } yield (result, user)

      prog.asserting {
        case (result, user) =>
          result shouldBe true
          user shouldBe None
      }
    }
  }

  "should not delete a nonexistend user" in {
    xaResource.use { xa =>
      val prog = for {
        users <- LiveUsers[IO](xa)
        userId <- users.delete(Norbert.email)
      } yield userId

      prog.asserting(_ shouldBe true)
    }
  }
}
