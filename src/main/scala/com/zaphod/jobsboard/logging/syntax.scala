package com.zaphod.jobsboard.logging

import cats.MonadError
import cats.implicits.*

import org.typelevel.log4cats.Logger

object syntax {
  extension [F[_], E, A](fa: F[A])(using me: MonadError[F, E], logger: Logger[F]) {
    def log(succ: A => String, err: E => String): F[A] = fa.attemptTap {
      case Left(e) => logger.error(err(e))
      case Right(a) => logger.info(succ(a))
    }

    def logError(err: E => String): F[A] = fa.attemptTap {
      case Left(e) => logger.error(err(e))
      case Right(_) => ().pure[F]
    }
  }
}
