package com.zaphod.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import cats.effect.Concurrent
import cats.implicits.*
import tsec.authentication.{SecuredRequestHandler, TSecAuthService, asAuthed}
import com.zaphod.jobsboard.Application.logger
import com.zaphod.jobsboard.core.Jobs
import com.zaphod.jobsboard.modules.*
import com.zaphod.jobsboard.domain.job.*
import com.zaphod.jobsboard.domain.security.*
import com.zaphod.jobsboard.domain.pagination.Pagination
import com.zaphod.jobsboard.domain.user.User
import com.zaphod.jobsboard.logging.syntax.*
import com.zaphod.jobsboard.http.validation.syntax.*
import com.zaphod.jobsboard.http.responses.FailureResponse
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger

import scala.language.implicitConversions

import java.util
import java.util.UUID

class JobRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F], authenticator: Authenticator[F]) extends HttpValidationDsl[F] {

  private val securedHandler: SecuredHandler[F] = SecuredRequestHandler(authenticator)

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
  private val createJob: AuthRoute[F] = {
    case req @ POST -> Root / "create" asAuthed _ =>
      req.request.validate[JobInfo] { jobInfo =>
        for {
          id <- jobs.create("cto@home.com", jobInfo)
          res <- Created(id)
        } yield res
      }
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJob: AuthRoute[F] = {
    case req @ PUT -> Root / UUIDVar(id) asAuthed user =>
      req.request.validate[JobInfo] { jobInfo =>
        jobs.find(id).flatMap {
          case None =>
            NotFound(FailureResponse(s"Cannot update job $id: not found"))
          case Some(job) if user.owns(job) || user.isAdmin =>
            jobs.update(id, jobInfo) *> Ok()
          case _ =>
            Forbidden(FailureResponse("You can only update jobs you own"))
        }
      }
  }

  // DELETE /jobs/uuid
  private val deleteJob: AuthRoute[F] = {
    case DELETE -> Root / UUIDVar(id) asAuthed user =>
      jobs.find(id).flatMap {
        case None =>
          NotFound(FailureResponse(s"Cannot delete job $id: not found"))
        case Some(job) if user.owns(job) || user.isAdmin =>
          jobs.delete(id) *> Ok()
        case _ =>
          Forbidden(FailureResponse("You can only delete jobs you own"))
      }
  }

  private val unauthedRoutes = allJobs <+> findJob
  private val authedRoutes = securedHandler.liftService(
    createJob.restrictedTo(allRoles) |+|
    deleteJob.restrictedTo(allRoles) |+|
    updateJob.restrictedTo(allRoles)
  )

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (unauthedRoutes <+> authedRoutes)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger](jobs: Jobs[F], authenticator: Authenticator[F]) = new JobRoutes[F](jobs, authenticator)
}
