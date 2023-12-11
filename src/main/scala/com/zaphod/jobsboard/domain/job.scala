package com.zaphod.jobsboard.domain

import java.util.UUID

object job {
  case class Job(
    id: UUID,
    date: Long,
    ownerEmail: String,
    jobInfo: JobInfo,
    active: Boolean = false
  )

  case class JobInfo(
    company: String,
    title: String,
    description: String,
    externalURL: String,
    remote: Boolean,
    location: String,
    salaryLo: Option[Int],
    salaryHi: Option[Int],
    currency: Option[String],
    country: Option[String],
    tags: Option[List[String]],
    image: Option[String],
    seniority: Option[String],
    other: Option[String]
  )

  object JobInfo {
    val empty: JobInfo =
      JobInfo("", "", "", "", false, "", None, None, None, None, None, None, None, None)

    def minimal(company: String,
                title: String,
                description: String,
                externalURL: String,
                remote: Boolean,
                location: String) =
      JobInfo(
        company = company,
        title = title,
        description = description,
        externalURL = externalURL,
        remote = remote,
        location = location,
        None, None, None, None, None, None, None, None
      )
  }
}
