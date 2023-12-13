package com.zaphod.jobsboard.modules

import cats.effect.{Async, Resource}
import com.zaphod.jobsboard.core.{Jobs, LiveJobs}
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor
final class Core[F[_]: Async](val jobs: Jobs[F])
object Core {
  def apply[F[_]: Async]: Resource[F, Core[F]] = {
    val pgResource: Resource[F, HikariTransactor[F]] = for {
      ec <- ExecutionContexts.fixedThreadPool(32)
      xa <- HikariTransactor.newHikariTransactor[F](
        "org.postgresql.Driver",
        "jdbc:postgresql:board",
        "docker",
        "docker",
        ec
      )
    } yield xa

    pgResource.evalMap{ xa => LiveJobs[F](xa) }.map(jobs => new Core[F](jobs))
  }
}
