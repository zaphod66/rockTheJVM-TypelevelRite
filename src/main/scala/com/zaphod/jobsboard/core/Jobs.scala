package com.zaphod.jobsboard.core

import cats.effect.kernel.MonadCancelThrow
import cats.implicits.*
import org.typelevel.log4cats.Logger

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*

import java.util.UUID
import com.zaphod.jobsboard.domain.job.{Job, JobFilter, JobInfo}
import com.zaphod.jobsboard.domain.pagination.Pagination
import com.zaphod.jobsboard.logging.syntax.*

trait Jobs[F[_]] {
  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]
  def all(): F[List[Job]]
  def all(filter: JobFilter, pagination: Pagination): F[List[Job]]
  def find(id: UUID): F[Option[Job]]
  def update(id: UUID, jobInfo: JobInfo): F[Option[Job]]
  def delete(id: UUID): F[Int]
}

class LiveJobs[F[_]: MonadCancelThrow: Logger] private(xa: Transactor[F]) extends Jobs[F] {

  override def create(ownerEmail: String, jobInfo: JobInfo): F[UUID] =
    sql"""
      INSERT INTO jobs(
        date,
        ownerEmail,
        company,
        title,
        description,
        externalURL,
        remote,
        location,
        salaryLo,
        salaryHi,
        currency,
        country,
        tags,
        image,
        seniority,
        other,
        active
      ) VALUES (
        ${System.currentTimeMillis()},
        $ownerEmail,
        ${jobInfo.company},
        ${jobInfo.title},
        ${jobInfo.description},
        ${jobInfo.externalURL},
        ${jobInfo.remote},
        ${jobInfo.location},
        ${jobInfo.salaryLo},
        ${jobInfo.salaryHi},
        ${jobInfo.currency},
        ${jobInfo.country},
        ${jobInfo.tags},
        ${jobInfo.image},
        ${jobInfo.seniority},
        ${jobInfo.other},
        false
      )
    """
      .update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(xa)

  override def all(): F[List[Job]] =
    sql"""
      SELECT
        id,
        date,
        ownerEmail,
        company,
        title,
        description,
        externalURL,
        remote,
        location,
        salaryLo,
        salaryHi,
        currency,
        country,
        tags,
        image,
        seniority,
        other,
        active
      FROM jobs
    """
      .query[Job]
      .to[List]
      .transact(xa)

  override def all(filter: JobFilter, pagination: Pagination): F[List[Job]] =
    val selectFrag: Fragment =
      fr"""
        SELECT
          id,
          date,
          ownerEmail,
          company,
          title,
          description,
          externalURL,
          remote,
          location,
          salaryLo,
          salaryHi,
          currency,
          country,
          tags,
          image,
          seniority,
          other,
          active
      """
    val fromFrag: Fragment =
      fr"FROM jobs"

    /*
    WHERE company in [filter.companies]
    AND location in [filter.locations]
    AND country in [filter.countries]
    AND seniority in [filter.seniorities]
    AND (
      tag1=ANY(tags)
      OR tag2=ANY(tags)
      OR ... for all tags in filter.tags
    )
    AND salaryHi > [filter.salaryMax]
    AND remote = [filter.remote]
     */
    val whereFrag: Fragment = Fragments.whereAndOpt(
      filter.companies.toNel.map(cs => Fragments.in(fr"company", cs)),  // Option[NonEmptyList] => Option[Fragment]
      filter.locations.toNel.map(ls => Fragments.in(fr"location", ls)),
      filter.countries.toNel.map(cs => Fragments.in(fr"company", cs)),
      filter.senorities.toNel.map(ss => Fragments.in(fr"seniority", ss)),
      filter.tags.toNel.map(tags =>
        Fragments.or(tags.map(tag => fr"$tag=ANY(tags)").toList: _*)
      ),
      filter.maxSalary.map(salary => fr"salaryHi >= $salary"),
      filter.remote.some.map(remote => fr"remote = $remote")
    )

    val paginationFrag: Fragment =
      fr"ORDER By id LIMIT ${pagination.limit} OFFSET ${pagination.offset}"

    val statement = selectFrag |+| fromFrag |+| whereFrag |+| paginationFrag

//    Logger[F].info(statement.toString) *>
    statement
      .query[Job]
      .to[List]
      .transact(xa)
      .logError(e => s"Failed query: ${e.getMessage}")

  override def find(id: UUID): F[Option[Job]] =
    sql"""
    SELECT
      id,
      date,
      ownerEmail,
      company,
      title,
      description,
      externalURL,
      remote,
      location,
      salaryLo,
      salaryHi,
      currency,
      country,
      tags,
      image,
      seniority,
      other,
      active
      FROM jobs
      WHERE id=$id
    """
      .query[Job]
      .option
      .transact(xa)

  override def update(id: UUID, jobInfo: JobInfo): F[Option[Job]] =
    sql"""
      UPDATE jobs
      SET
        company = ${jobInfo.company},
        title = ${jobInfo.title},
        description = ${jobInfo.description},
        externalURL = ${jobInfo.externalURL},
        remote = ${jobInfo.remote},
        location = ${jobInfo.location},
        salaryLo = ${jobInfo.salaryLo},
        salaryHi = ${jobInfo.salaryHi},
        currency = ${jobInfo.currency},
        country = ${jobInfo.country},
        tags = ${jobInfo.tags},
        image = ${jobInfo.image},
        seniority = ${jobInfo.seniority},
        other = ${jobInfo.other}
      WHERE id = $id
    """
      .update
      .run
      .transact(xa)
      .flatMap(_ => find(id))

  override def delete(id: UUID): F[Int] =
    sql"""
      DELETE FROM jobs
      WHERE id = $id
    """
      .update
      .run
      .transact(xa)
}

object LiveJobs {
  given jobRead: Read[Job] = Read[(
    UUID, // id
    Long, // date
    String, // ownerEmail
    String, // company
    String, // title
    String, // description
    String, // externalURL
    Boolean, // remote
    String, // location
    Option[Int], // salaryLo
    Option[Int], // salaryHi
    Option[String], // currency
    Option[String], // country
    Option[List[String]], // tags
    Option[String], // image
    Option[String], // seniority
    Option[String], // other
    Boolean // active
  )].map {
    case (
      id: UUID,
      date: Long,
      ownerEmail: String,
      company: String,
      title: String,
      description: String,
      externalURL: String,
      remote: Boolean,
      location: String,
      salaryLo: Option[Int] @unchecked,
      salaryHi: Option[Int] @unchecked,
      currency: Option[String] @unchecked,
      country: Option[String] @unchecked,
      tags: Option[List[String]] @unchecked,
      image: Option[String] @unchecked,
      seniority: Option[String] @unchecked,
      other: Option[String] @unchecked,
      active: Boolean
    ) => Job(
      id = id,
      date = date,
      ownerEmail = ownerEmail,
      JobInfo (
        company = company,
        title = title,
        description = description,
        externalURL = externalURL,
        remote = remote,
        location = location,
        salaryLo = salaryLo,
        salaryHi = salaryHi,
        currency = currency,
        country = country,
        tags = tags,
        image = image,
        seniority = seniority,
        other = other
      ),
      active = active
    )
  }

  def apply[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): F[LiveJobs[F]] = new LiveJobs[F](xa).pure[F]
}
