package com.zaphod.jobsboard.modules

import cats.effect
import cats.effect.{ Concurrent, Resource }
import cats.implicits.*
import com.zaphod.jobsboard.http.routes.*
import com.zaphod.jobsboard.modules.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger

class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F]) {
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes = JobRoutes[F](core.jobs).routes

  val routes: HttpRoutes[F] = Router(
    "/api" -> (healthRoutes <+> jobRoutes)
  )
}

object HttpApi {
  def apply[F[_]: Concurrent: Logger](core: Core[F]): Resource[F, HttpApi[F]] =
    Resource.pure(new HttpApi[F](core))
}
