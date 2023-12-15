package com.zaphod.jobsboard.http.validation

import cats.MonadThrow
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.*
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.http4s.Status.BadRequest
import org.typelevel.log4cats.Logger
import com.zaphod.jobsboard.logging.syntax.*
import com.zaphod.jobsboard.http.responses.*
import validators.*
object syntax {

  private def validateEntity[A](entity: A)(using validator: Validator[A]): ValidationResult[A] =
    validator.validate(entity)

  trait HttpValidationDsl[F[_]: MonadThrow: Logger] extends Http4sDsl[F] {
    extension (req: Request[F])
      def validate[A: Validator](serverContinuation: A => F[Response[F]])(using EntityDecoder[F, A]): F[Response[F]] =
        req
          .as[A]
          .logError(e => s"Parsing failed: $e")
          .map(validateEntity)
          .flatMap {
            case Valid(entity) => serverContinuation(entity)
            case Invalid(e) => BadRequest(FailureResponse(e.toList.mkString(", ")))
          }
  }
}
