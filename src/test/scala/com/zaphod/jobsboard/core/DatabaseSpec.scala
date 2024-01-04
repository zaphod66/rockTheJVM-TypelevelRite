package com.zaphod.jobsboard.core

import cats.effect.{IO, Resource}
import doobie.*
import doobie.implicits.*
import doobie.util.*
import doobie.hikari.HikariTransactor
import org.testcontainers.containers.PostgreSQLContainer

trait DatabaseSpec {

  val initScript: String

  val pgResource: Resource[IO, PostgreSQLContainer[Nothing]] = {
    val acquire = IO {
      val container: PostgreSQLContainer[Nothing] =
        new PostgreSQLContainer[Nothing]("postgres:16-alpine").withInitScript(initScript)
      container.start()
      container
    }

    val release = (container: PostgreSQLContainer[Nothing]) => IO(container.stop())

    Resource.make(acquire)(release)
  }

  val xaResource: Resource[IO, Transactor[IO]] =
    for {
      db <- pgResource
      ce <- ExecutionContexts.fixedThreadPool(1)
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        db.getJdbcUrl,
        db.getUsername,
        db.getPassword,
        ce
      )
    } yield xa
}
