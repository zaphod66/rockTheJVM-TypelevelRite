package com.zaphod.jobsboard.config

import cats.MonadThrow
import cats.effect.IO
import pureconfig.error.ConfigReaderException
import pureconfig.{ConfigReader, ConfigSource}

import scala.reflect.ClassTag

object Syntax {
  extension (source: ConfigSource)
    def loadF[F[_], A](using reader: ConfigReader[A], F: MonadThrow[F], tag: ClassTag[A]): F[A] =
      source.load[A].fold(
        errors => F.raiseError(ConfigReaderException(errors)),
        value => F.pure(value)
      )
}
