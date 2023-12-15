package com.zaphod.jobsboard.fixtures

import cats.syntax.all.*

import com.zaphod.jobsboard.domain.job.*
import com.zaphod.jobsboard.domain.job

import java.util.UUID

trait JobsFixture {

  val NotFoundJobUuid: UUID = UUID.fromString("6ea79557-3112-4c84-a8f5-1d1e2c300948")

  val AwesomeJobUuid: UUID = UUID.fromString("843df718-ec6e-4d49-9289-f799c0f40064")

  val AwesomeJob: Job = Job(
    AwesomeJobUuid,
    1659186086L,
    "me@home.com",
    JobInfo(
      "Awesome Company",
      "Tech Lead",
      "An awesome job in Berlin",
      "https://home.com/awesomejob",
      false,
      "Berlin",
      2000.some,
      3000.some,
      "EUR".some,
      "Germany".some,
      Some(List("scala", "scala-3", "cats")),
      None,
      "Senior".some,
      None
    )
  )

  val InvalidJob: Job = Job(
    null,
    42L,
    "nothing@gmail.com",
    JobInfo.empty
  )

  val UpdatedAwesomeJob: Job = Job(
    AwesomeJobUuid,
    1659186086L,
    "me@home.com",
    JobInfo(
      "Awesome Company (Spain Branch)",
      "Engineering Manager",
      "An awesome job in Barcelona",
      "http://www.awesome.com",
      false,
      "Barcelona",
      2200.some,
      3200.some,
      "USD".some,
      "Spain".some,
      Some(List("scala", "scala-3", "zio")),
      "http://www.awesome.com/logo.png".some,
      "Highest".some,
      "Some additional info".some
    )
  )

  val NewJobInfo: JobInfo = JobInfo(
    "Holistic Creators",
    "Technical Author",
    "For the glory of the Holistics!",
    "https://holisticcreators.com/",
    true,
    "From remote",
    2000.some,
    3500.some,
    "EUR".some,
    "Romania".some,
    Some(List("scala", "scala-3", "cats", "akka", "spark", "flink", "zio")),
    None,
    "High".some,
    None
  )

  val AwesomeJobWithNotFoundId: Job = AwesomeJob.copy(id = NotFoundJobUuid)

  val AnotherAwesomeJobUuid: UUID = UUID.fromString("19a941d0-aa19-477b-9ab0-a7033ae65c2b")
  val AnotherAwesomeJob: Job = AwesomeJob.copy(id = AnotherAwesomeJobUuid)

  val RockTheJvmAwesomeJob: Job =
    AwesomeJob.copy(jobInfo = AwesomeJob.jobInfo.copy(company = "RockTheJvm"))

  val NewJobUuid: UUID = UUID.fromString("efcd2a64-4463-453a-ada8-b1bae1db4377")
  val AwesomeNewJob: JobInfo = JobInfo(
    "Awesome Company",
    "Tech Lead",
    "An awesome job in Berlin",
    "https://example.com",
    false,
    "Berlin",
    2000.some,
    3000.some,
    "EUR".some,
    "Germany".some,
    Some(List("scala", "scala-3", "cats")),
    None,
    "High".some,
    None
  )
}
