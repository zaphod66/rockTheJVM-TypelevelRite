package com.zaphod.jobsboard.core

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.zaphod.jobsboard.domain.job.JobFilter
import com.zaphod.jobsboard.domain.pagination.Pagination
import org.http4s.dsl.Http4sDsl
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import com.zaphod.jobsboard.fixtures.JobsFixture
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class JobsSpec
  extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with JobsFixture
    with DatabaseSpec
{
  val initScript = "sql/jobs.sql"

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Jobs algebra" - {
    "find should return None, if UUID does not exist" in {
      xaResource.use { xa =>
        val prog = for {
          jobs <- LiveJobs[IO](xa)
          retr <- jobs.find(NotFoundJobUuid)
        } yield retr

        prog.asserting(_ shouldBe None)
      }
    }


    "find should return a job, if UUID exits" in {
      xaResource.use { xa =>
        val prog = for {
          jobs <- LiveJobs[IO](xa)
          retr <- jobs.find(AwesomeJobUuid)
        } yield retr

        prog.asserting(_ shouldBe Some(AwesomeJob))
      }
    }

    "all should return a List[Job]" in {
      xaResource.use { xa =>
        val prog = for {
          jobs <- LiveJobs[IO](xa)
          retr <- jobs.all()
        } yield retr

        prog.asserting(_ shouldBe List(AwesomeJob))
      }
    }

    "create should create a new job" in {
      xaResource.use { xa =>
        val prog = for {
          jobs <- LiveJobs[IO](xa)
          id <- jobs.create("nobody@gmail.com", NewJobInfo)
          jobF <- jobs.find(id)
        } yield jobF

        prog.asserting(_.map(_.jobInfo) shouldBe Some(NewJobInfo))
      }
    }

    "update should return a None, if UUID does not exists" in {
      xaResource.use { xa =>
        val prog = for {
          jobs <- LiveJobs[IO](xa)
          jobF <- jobs.update(NotFoundJobUuid, UpdatedAwesomeJob.jobInfo)
        } yield jobF

        prog.asserting(_ shouldBe None)
      }
    }

    "update should return an updated job, if UUID exists" in {
      xaResource.use { xa =>
        val prog = for {
          jobs <- LiveJobs[IO](xa)
          jobF <- jobs.update(AwesomeJobUuid, UpdatedAwesomeJob.jobInfo)
        } yield jobF

        prog.asserting(_ shouldBe Some(UpdatedAwesomeJob))
      }
    }

    "delete should delete a job" in {
      xaResource.use { xa =>
        val prog = for {
          jobs <- LiveJobs[IO](xa)
          c1 <- sql"SELECT COUNT(*) FROM jobs WHERE id=$AwesomeJobUuid".query[Int].unique.transact(xa)
          num <- jobs.delete(AwesomeJobUuid)
          c2 <- sql"SELECT COUNT(*) FROM jobs WHERE id=$AwesomeJobUuid".query[Int].unique.transact(xa)
        } yield (c1, num, c2)

        prog.asserting {
          case (c1, n, c2) =>
            (c1, n, c2) shouldBe(1, 1, 0)
        }
      }
    }

    "delete should return 0, if UUID exists" in {
      xaResource.use { xa =>
        val prog = for {
          jobs <- LiveJobs[IO](xa)
          num <- jobs.delete(NotFoundJobUuid)
        } yield num

        prog.asserting(_ shouldBe 0)
      }
    }

    "all should filter remote jobs 1" in {
      xaResource.use { xa =>
        val prog = for {
          jobs <- LiveJobs[IO](xa)
          filteredJobs <- jobs.all(JobFilter(remote = true), Pagination.default)
        } yield filteredJobs

        prog.asserting(_ shouldBe List())
      }
    }

    "all should filter remote jobs 2" in {
      xaResource.use { xa =>
        val prog = for {
          jobs <- LiveJobs[IO](xa)
          filteredJobs <- jobs.all(JobFilter(remote = false), Pagination.default)
        } yield filteredJobs

        prog.asserting(_ shouldBe List(AwesomeJob))
      }
    }

    "all should filter jobs by tags" in {
      xaResource.use { xa =>
        val prog = for {
          jobs <- LiveJobs[IO](xa)
          filteredJobs <- jobs.all(JobFilter(tags = List("scala", "cats", "zio")), Pagination.default)
        } yield filteredJobs

        prog.asserting(_ shouldBe List(AwesomeJob))
      }
    }
  }
}
