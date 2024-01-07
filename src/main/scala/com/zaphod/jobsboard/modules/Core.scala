package com.zaphod.jobsboard.modules

import cats.implicits.*
import cats.effect.{Async, Resource}
import com.zaphod.jobsboard.config.SecurityConfig
import com.zaphod.jobsboard.core.{Auth, Jobs, LiveAuth, LiveJobs, LiveUsers, Users}
import doobie.Transactor
import org.typelevel.log4cats.Logger

final class Core[F[_]: Async](val jobs: Jobs[F], val users: Users[F], val auth: Auth[F])

object Core {
  def apply[F[_]: Async: Logger](xa: Transactor[F]): Resource[F, Core[F]] = {
    val coreF = for {
      jobs <- LiveJobs[F](xa)
      users <- LiveUsers[F](xa)
      auth <- LiveAuth[F](users)
    } yield new Core(jobs, users, auth)

    Resource.eval(coreF)
  }
}
