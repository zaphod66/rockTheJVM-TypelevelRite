package com.zaphod.jobsboard.playground

import cats.effect.{IO, IOApp}

import doobie.util.transactor.Transactor
import doobie.implicits.{toConnectionIOOps, toSqlInterpolator}
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor
import doobie.util.log.LogHandler

object Doobie extends IOApp.Simple {

  case class Student(id: Int, name: String)

  def getAllStudentNames: IO[List[String]] = {
    val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver", // JDBC connector
      "jdbc:postgresql://localhost:5432/demo", // database URL
      "docker",
      "docker"
    )

    val query = sql"SELECT name FROM Students".queryWithLogHandler[String](LogHandler.jdkLogHandler)
    val action = query.to[List]

    action.transact(xa)
  }

  override def run: IO[Unit] = ???
}
