package com.zaphod.jobsboard.modules

import cats.effect.{Async, Resource}
import com.zaphod.jobsboard.core.{Jobs, LiveJobs}
import doobie.Transactor

final class Core[F[_]: Async](val jobs: Jobs[F])
object Core {
  def apply[F[_]: Async](xa: Transactor[F]): Resource[F, Core[F]] =
    Resource.eval(LiveJobs[F](xa)).map(jobs => new Core[F](jobs))
}
