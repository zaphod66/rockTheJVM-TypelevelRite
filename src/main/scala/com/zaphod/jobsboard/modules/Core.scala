package com.zaphod.jobsboard.modules

import cats.implicits.*
import cats.effect.{Async, Resource}
import com.zaphod.jobsboard.config.SecurityConfig
import com.zaphod.jobsboard.core.{Auth, Jobs, LiveAuth, LiveJobs, LiveUsers}
import doobie.Transactor
import org.typelevel.log4cats.Logger

final class Core[F[_]: Async](val jobs: Jobs[F], val auth: Auth[F])

object Core {
  def apply[F[_]: Async: Logger](xa: Transactor[F])(securityConfig: SecurityConfig): Resource[F, Core[F]] = {
    val coreF = for {
      jobs <- LiveJobs[F](xa)
      users <- LiveUsers[F](xa)
      auth <- LiveAuth[F](users)(securityConfig)
    } yield new Core(jobs, auth)

    Resource.eval(coreF)
  }
}
