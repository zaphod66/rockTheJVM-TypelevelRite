package com.zaphod.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.effect.Concurrent
import cats.implicits.*
import com.zaphod.jobsboard.domain.job.*
import com.zaphod.jobsboard.http.responses.FailureResponse
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*

import org.typelevel.log4cats.Logger

import java.util
import java.util.UUID

class JobRoutes[F[_]: Concurrent: Logger] private extends Http4sDsl[F] {


  import scala.collection.mutable

  private val database = mutable.Map[UUID, Job]()

  // POST /jobs
  private val allJobs: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root => Ok(database.values)
  }

  // GET /jobs/uuid
  private val findJob: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(job) => Ok(job)
        case None      => NotFound(FailureResponse(s"Job with $id not found."))
      }
  }

  // POST /jobs/create { jobInfo }
  private def makeJob(jobInfo: JobInfo): F[Job] =
    Job(
      id = UUID.randomUUID(),
      date = System.currentTimeMillis(),
      ownerEmail = "TODO@gmail.com",
      jobInfo = jobInfo,
      active = true
    ).pure[F]

  import com.zaphod.jobsboard.logging.syntax.*

  private val createJob: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      for {
        jobInfo <- req.as[JobInfo].logError(e => s"Parsing payload failed: $e")
        job <- makeJob(jobInfo)
        _ <- database.put(job.id, job).pure[F]
        res <- Created(job.id)
      } yield res
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJob: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(job) =>
          for {
            jobInfo <- req.as[JobInfo].logError(e => s"Parsing payload failed: $e")
            _ <- database.put(id, job.copy(jobInfo = jobInfo)).pure[F]
            res <- Ok()
          } yield res
        case None       => NotFound(FailureResponse(s"Cannot update job $id: not Found."))
      }
  }

  // DELETE /jobs/uuid
  private val deleteJob: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ DELETE -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(_) =>
          for {
            _ <- database.remove(id).pure[F]
            res <- Ok()
          } yield res
        case None       => NotFound(FailureResponse(s"Cannot delete job $id: not Found."))
      }
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (allJobs <+> findJob <+> createJob <+> updateJob <+> deleteJob)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger] = new JobRoutes[F]
}
