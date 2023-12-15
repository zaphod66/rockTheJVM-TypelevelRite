package com.zaphod.jobsboard.http.validation

import cats.*
import cats.data.Validated.*
import cats.data.ValidatedNel
import cats.syntax.all.*
import com.zaphod.jobsboard.domain.job.JobInfo

import java.net.URL

object validators {

  sealed trait ValidationFailure(val errorMsg: String)

  case class EmptyField(field: String) extends ValidationFailure(s"'$field' is empty")
  case class InvalidUrl(field: String) extends ValidationFailure(s"'$field' is not a valid URL")
//  case object SimpleFailure extends ValidationFailure("Not implemented")

  type ValidationResult[A] = ValidatedNel[ValidationFailure, A]

  trait Validator[A] {
    def validate(value: A): ValidationResult[A]
  }

  private def validateRequired[A](field: A, fieldName: String)(required: A => Boolean): ValidationResult[A] =
    if (required(field)) field.validNel
    else EmptyField(fieldName).invalidNel

  import scala.util.{Failure, Success, Try}
  private def validateUrl(field: String, fieldName: String): ValidationResult[String] =
    Try(URL(field).toURI) match {
      case Success(_) => field.validNel
      case Failure(_) => InvalidUrl(fieldName).invalidNel
    }

  given jobInfoValidator: Validator[JobInfo] = (jobInfo: JobInfo) => {
    val JobInfo(
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
      other
    ) = jobInfo

    val validCompany = validateRequired(company, "company")(_.nonEmpty)
    val validTitle = validateRequired(title, "title")(_.nonEmpty)
    val validDescription = validateRequired(description, "description")(_.nonEmpty)
    val validLocation = validateRequired(location, "location")(_.nonEmpty)

    val validUrl = validateUrl(externalURL, "externalURL")

    (
      validCompany, // company,
      validTitle, // title,
      validDescription, // description,
      validUrl, // externalURL,
      remote.validNel,  // remote,
      validLocation,  // location,
      salaryLo.validNel,  // salaryLo,
      salaryHi.validNel,  // salaryHi,
      currency.validNel,  // currency,
      country.validNel, // country,
      tags.validNel,  // tags,
      image.validNel, // image,
      seniority.validNel, // seniority,
      other.validNel  // other
    ).mapN(JobInfo.apply)
  }
}
