package com.zaphod.jobsboard.playground

import cats.effect.{IO, IOApp, Resource}
import com.zaphod.jobsboard.core.LiveJobs
import com.zaphod.jobsboard.domain.job.JobInfo
import doobie.*
import doobie.implicits.*
import doobie.util.*
import doobie.hikari.HikariTransactor

import scala.io.StdIn

object Jobs extends IOApp.Simple {

  val pgResource: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool(32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:board",
      "docker",
      "docker",
      ec
    )
  } yield xa

  val jobInfo1 = JobInfo.minimal("Home Company", "Engineer", "best job", "home.com", true, "Germany")
  val jobInfo2 = JobInfo.minimal("Home Company", "Cook", "best cook", "home.com", false, "Germany, Hamburg")

  // connect from cmdline "> docker exec -it rockthejvm-typelevelrite-db-1 psql -U docker"
  override def run: IO[Unit] = pgResource.use { xa =>
    for {
      jobs <- LiveJobs[IO](xa)
      _ <- IO.println("Ready.")
      id1 <- jobs.create("cto@home.com", jobInfo1)
      _ <- IO.println(s"Job id = $id1.")
      id2 <- jobs.create("cto@home.com", jobInfo2)
      _ <- IO.println(s"Job id = $id2.")
      list1 <- jobs.all()
      _ <- IO.println(s"All jobs = $list1.")
      _ <- jobs.update(id1, jobInfo1.copy(remote = false))
      job1 <- jobs.find(id1)
      _ <- IO.println(s"Updated job = $job1.")
      n <- jobs.delete(id2)
      _ <- IO.println(s"Deleted jobs = $n.")
      list2 <- jobs.all()
      _ <- IO.println(s"All jobs = $list2.")
    } yield ()
  }
}
