package com.zaphod.jobsboard

import cats.Monad
import cats.implicits.*
import cats.effect.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.*
import pureconfig.ConfigSource
import com.zaphod.jobsboard.config.EmberConfig
import com.zaphod.jobsboard.config.Syntax.*
import com.zaphod.jobsboard.http.HttpApi

object Application extends IOApp.Simple {
  override def run: IO[Unit] =
    ConfigSource.default.loadF[IO, EmberConfig].flatMap { config =>
      EmberServerBuilder
        .default[IO]
        .withHost(config.host)
        .withPort(config.port)
        .withHttpApp(HttpApi[IO].routes.orNotFound)
        .build
        .use(_ => IO.println("Server ready.") *> IO.never)
    }
}
