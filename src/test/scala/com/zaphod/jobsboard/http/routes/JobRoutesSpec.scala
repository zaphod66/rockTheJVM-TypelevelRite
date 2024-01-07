package com.zaphod.jobsboard.http.routes

import cats.implicits.*
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import com.zaphod.jobsboard.core.Jobs
import com.zaphod.jobsboard.domain.job.{Job, JobFilter, JobInfo}
import com.zaphod.jobsboard.domain.pagination.Pagination
import com.zaphod.jobsboard.domain.security.JwtToken
import com.zaphod.jobsboard.fixtures.{JobsFixture, SecuredFixture}
import com.zaphod.jobsboard.util.Syntax.*
import org.http4s.headers.Authorization
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256

import java.util.UUID

class JobRoutesSpec
  extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with Http4sDsl[IO]
  with SecuredFixture
  with JobsFixture
{

  ///////////////////////////////////////////
  // mocks
  ///////////////////////////////////////////

  private val jobs: Jobs[IO] = new Jobs[IO] {
    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] =
      IO.pure(NewJobUuid)

    override def all(): IO[List[Job]] =
      IO.pure(List(AwesomeJob))

    override def all(filter: JobFilter, pagination: Pagination): IO[List[Job]] =
      if (filter.remote) IO.pure(List.empty[Job])
      else IO.pure(List(AwesomeJob))

    override def find(id: UUID): IO[Option[Job]] =
      IO.pure( (id == AwesomeJobUuid).option(AwesomeJob) )

    override def update(id: UUID, jobInfo: JobInfo): IO[Option[Job]] =
      IO.pure( (id == AwesomeJobUuid).option(UpdatedAwesomeJob) )

    override def delete(id: UUID): IO[Int] =
      IO.pure( if (id == AwesomeJobUuid) 1 else 0 )
  }

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val jobRoutes: HttpRoutes[IO] = JobRoutes[IO](jobs, mockedAuthenticator).routes
  private val jobRoutesNF = jobRoutes.orNotFound

  ///////////////////////////////////////////
  // tests
  ///////////////////////////////////////////

  "JobRoutes" - {
    "should return a job for a given id" in {
      for {
        respOk <-jobRoutesNF.run(Request(method = Method.GET, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064"))
        jobOk  <- respOk.as[Job]
        respFail <-jobRoutesNF.run(Request(method = Method.GET, uri = uri"/jobs/6ea79557-3112-4c84-a8f5-1d1e2c300948"))
      } yield {
        respOk.status shouldBe Status.Ok
        jobOk shouldBe AwesomeJob
        respFail.status shouldBe Status.NotFound
      }
    }

    "should return all jobs" in {
      for {
        resp <-jobRoutesNF.run(Request(method = Method.POST, uri = uri"/jobs").withEntity(JobFilter()))
        jobsOk  <- resp.as[List[Job]]
      } yield {
        resp.status shouldBe Status.Ok
        jobsOk shouldBe List(AwesomeJob)
      }
    }

    "should return all jobs satisfying a filter 1" in {
      for {
        resp <-jobRoutesNF.run(Request(method = Method.POST, uri = uri"/jobs").withEntity(JobFilter(remote=true)))
        jobsOk  <- resp.as[List[Job]]
      } yield {
        resp.status shouldBe Status.Ok
        jobsOk shouldBe List()
      }
    }

    "should return all jobs satisfying a filter 2" in {
      for {
        resp <-jobRoutesNF.run(Request(method = Method.POST, uri = uri"/jobs").withEntity(JobFilter(remote=false)))
        jobsOk  <- resp.as[List[Job]]
      } yield {
        resp.status shouldBe Status.Ok
        jobsOk shouldBe List(AwesomeJob)
      }
    }

    // just tests, if parsing works. Functionality is not yet implemented in the mock
    "should return all jobs satisfying a filter 3" in {
      for {
        resp <-jobRoutesNF.run(Request(method = Method.POST, uri = uri"/jobs").withEntity(JobFilter(tags=List("scala", "zio"))))
        jobsOk  <- resp.as[List[Job]]
      } yield {
        resp.status shouldBe Status.Ok
        jobsOk shouldBe List(AwesomeJob)
      }
    }

    "should create a new job" in {
      for {
        jwtToken <- mockedAuthenticator.create(Norbert.email)
        resp <-jobRoutesNF.run(
          Request(method = Method.POST, uri = uri"/jobs/create")
            .withBearerToken(jwtToken)
            .withEntity(AwesomeJob.jobInfo)
          )

        uuid <- resp.as[UUID]
      } yield {
        resp.status shouldBe Status.Created
        uuid shouldBe NewJobUuid
      }
    }

    "should update a job that the JWTtoken 'owns'" in {
      for {
        jwtToken <- mockedAuthenticator.create(Norbert.email)
        respOk <-jobRoutesNF.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withBearerToken(jwtToken)
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )

        respFail <-jobRoutesNF.run(
          Request(method = Method.PUT, uri = uri"/jobs/6ea79557-3112-4c84-a8f5-1d1e2c300948")
            .withBearerToken(jwtToken)
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
      } yield {
        respOk.status shouldBe Status.Ok
        respFail.status shouldBe Status.NotFound
      }
    }

    "should forbid updating a job that the JWTtoken doesn't 'own'" in {
      for {
        jwtToken <- mockedAuthenticator.create(Jana.email)
        resp <-jobRoutesNF.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withBearerToken(jwtToken)
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
      } yield {
        resp.status shouldBe Status.Forbidden
      }
    }

    "should delete a job" in {
      for {
        jwtToken <- mockedAuthenticator.create(Norbert.email)
        respOk <-jobRoutesNF.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withBearerToken(jwtToken)
          )

        respFail <-jobRoutesNF.run(
          Request(method = Method.DELETE, uri = uri"/jobs/6ea79557-3112-4c84-a8f5-1d1e2c300948")
            .withBearerToken(jwtToken)
        )
      } yield {
        respOk.status shouldBe Status.Ok
        respFail.status shouldBe Status.NotFound
      }
    }
  }
}
