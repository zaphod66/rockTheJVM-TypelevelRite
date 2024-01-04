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
import com.zaphod.jobsboard.config.{AppConfig, EmberConfig}
import com.zaphod.jobsboard.util.Syntax.*
import com.zaphod.jobsboard.modules.{Core, Database, HttpApi}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Application extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    ConfigSource.default.loadF[IO, AppConfig].flatMap {
      case AppConfig(dbConfig, emberConfig, securityConfig) =>
        val appServer = for {
          xa <- Database[IO](dbConfig)
          core <- Core[IO](xa)(securityConfig)
          http <- HttpApi[IO](core)
          server <- EmberServerBuilder
            .default[IO]
            .withHost(emberConfig.host)
            .withPort(emberConfig.port)
            .withHttpApp(http.routes.orNotFound)
            .build
        } yield server
  
        appServer.use(_ => IO.println("Server ready.") *> IO.never)
    }
}
