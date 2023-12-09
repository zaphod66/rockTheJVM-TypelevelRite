package com.zaphod.jobsboard.http.routes

import cats.Monad
import cats.implicits.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*

class JobRoutes[F[_]: Monad] private extends Http4sDsl[F] {
  // POST /jobs
  private val allJobs: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root => Ok("ToDo!")
  }

  // GET /jobs/uuid
  private val findJob: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(id) =>
      Ok(s"ToDo find job $id.")
  }

  // POST /jobs/create { jobsInfo }
  private val createJob: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root / "create" =>
      Ok("ToDo create.")
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJob: HttpRoutes[F] = HttpRoutes.of[F] {
    case PUT -> Root / UUIDVar(id) =>
      Ok(s"ToDo update for $id.")
  }

  // DELETE /jobs/uuid
  private val deleteJob: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(id) =>
      Ok(s"ToDo DELETE for $id.")
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (allJobs <+> findJob <+> createJob <+> updateJob <+> deleteJob)
  )
}

object JobRoutes {
  def apply[F[_]: Monad] = new JobRoutes[F]
}
