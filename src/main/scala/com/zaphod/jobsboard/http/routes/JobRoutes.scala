package com.zaphod.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.effect.Concurrent
import cats.implicits.*
import com.zaphod.jobsboard.Application.logger
import com.zaphod.jobsboard.core.Jobs
import com.zaphod.jobsboard.modules.*
import com.zaphod.jobsboard.domain.job.*
import com.zaphod.jobsboard.domain.pagination.Pagination
import com.zaphod.jobsboard.logging.syntax.*
import com.zaphod.jobsboard.http.validation.syntax.*
import com.zaphod.jobsboard.http.responses.FailureResponse
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger

import java.util
import java.util.UUID

class JobRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F]) extends HttpValidationDsl[F] {

  private object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
  private object LimitQueryParam extends OptionalQueryParamDecoderMatcher[Int]("limit")


  // POST /jobs?limit=x&offset=y { filter }
  private val allJobs: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root :? LimitQueryParam(limitF) +& OffsetQueryParam(offsetF) =>
      for {
        filter <- req.as[JobFilter]
        all    <- jobs.all(filter, Pagination(limitF, offsetF))
        res    <- Ok(all)
      } yield res
  }

  // GET /jobs/uuid
  private val findJob: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(id) =>
      jobs.find(id).flatMap {
        case Some(job) => Ok(job)
        case None      => NotFound(FailureResponse(s"Job with $id not found."))
      }
  }

  // POST /jobs/create { jobInfo }
  private val createJob: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      req.validate[JobInfo] { jobInfo =>
        for {
          id <- jobs.create("cto@home.com", jobInfo)
          res <- Created(id)
        } yield res
      }
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJob: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(id) =>
      req.validate[JobInfo] { jobInfo =>
        for {
          jobsF <- jobs.update(id, jobInfo)
          res <- jobsF match {
            case Some(_) => Ok()
            case None => NotFound(FailureResponse(s"Cannot update job $id: not Found."))
          }
        } yield res
      }
  }

  // DELETE /jobs/uuid
  private val deleteJob: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(id) =>
      jobs.delete(id).flatMap {
        case 0 => NotFound(FailureResponse(s"Cannot delete job $id: not Found."))
        case _ => Ok()
      }
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (allJobs <+> findJob <+> createJob <+> updateJob <+> deleteJob)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger](jobs: Jobs[F]) = new JobRoutes[F](jobs)
}
