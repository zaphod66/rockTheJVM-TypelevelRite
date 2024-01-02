package com.zaphod.jobsboard.http.validation

import cats.*
import cats.data.Validated.*
import cats.data.ValidatedNel
import cats.syntax.all.*

import com.zaphod.jobsboard.domain.auth.*
import com.zaphod.jobsboard.domain.job.JobInfo
import com.zaphod.jobsboard.domain.user.*

import java.net.URL

object validators {

  sealed trait ValidationFailure(val errorMsg: String)

  private case class EmptyField(field: String) extends ValidationFailure(s"'$field' is empty")
  private case class InvalidUrl(field: String) extends ValidationFailure(s"'$field' is not a valid URL")
  private case class InvalidEmail(email: String) extends ValidationFailure(s"'$email' is not a valid email")
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

  private val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  private def validateEmail(email: String): ValidationResult[String] =
    if (emailRegex.findFirstMatchIn(email).isDefined) email.validNel
    else InvalidEmail(email).invalidNel

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

  given loginInfoValidator: Validator[LoginInfo] = (loginInfo: LoginInfo) => {
    val validEmail = validateRequired(loginInfo.email, "email")(_.nonEmpty) andThen validateEmail
    val validPassword = validateRequired(loginInfo.password, "password")(_.nonEmpty)
    (validEmail, validPassword).mapN(LoginInfo.apply)
  }

  given newUserInfoValidator: Validator[NewUserInfo] = (newUserInfo: NewUserInfo) => {
    val validEmail = validateRequired(newUserInfo.email, "email")(_.nonEmpty) andThen validateEmail
    val validPassword = validateRequired(newUserInfo.password, "password")(_.nonEmpty)

    (
      validEmail,
      validPassword,
      newUserInfo.firstName.validNel,
      newUserInfo.lastName.validNel,
      newUserInfo.company.validNel
    ).mapN(NewUserInfo.apply)
  }

  given newPasswordInfoValidator: Validator[NewPasswordInfo] = (newPasswordInfo: NewPasswordInfo) => {
    val validOld = validateRequired(newPasswordInfo.oldPassword, "oldPassword")(_.nonEmpty)
    val validNew = validateRequired(newPasswordInfo.newPassword, "newPassword")(_.nonEmpty)

    (validOld, validNew).mapN(NewPasswordInfo.apply)
  }
}
