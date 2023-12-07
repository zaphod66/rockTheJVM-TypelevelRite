package com.zaphod.jobsboard.playground

import cats.*
import cats.implicits.*
import cats.effect.{IO, IOApp}
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.typelevel.ci.CIString
import org.http4s.circe.*
import org.http4s.headers.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router

import java.util.UUID
object Http4s extends IOApp.Simple {

  type Student = String
  case class Instructor(firstName: String, lastName: String)
  case class Course(id: String, title: String, year: Int, students: List[Student], instructorName: String)

  object CourseRepo {
    private val scalaCourse = Course(
      "ebf12c2b-da6c-4f7b-ae1b-2a17a4204b43",
      "Scala Course (Beginner)",
      2022,
      List("Alice", "Bob", "Eva"),
      "Martin Odersky"
    )

    private val courses: Map[String, Course] = Map(scalaCourse.id -> scalaCourse)

    def findById(id: String): Option[Course] = courses.get(id)

    def findByInstructor(name: String): List[Course] =
      courses.values.filter(_.instructorName == name).toList
  }

  // REST endpoints
  // localhost:8080/courses?instructor=Martin%20Odersky&year=2022
  // localhost:8080/courses/ebf12c2b-da6c-4f7b-ae1b-2a17a4204b43/students
  // http GET 'localhost:8080/courses?instructor=Martin%20Odersky&year=2022'


  object InstructorQueryParamMatcher extends QueryParamDecoderMatcher[String]("instructor")
  object YearQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")

  def courseRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "courses" :? InstructorQueryParamMatcher(name) +& YearQueryParamMatcher(yearM) =>
        val courses = CourseRepo.findByInstructor(name)
        yearM match {
          case Some(y) => y.fold(
            _ => BadRequest("Parameter 'year' is invalid."),
            year => Ok(courses.filter(_.year == year).asJson)
          )
          case None    => Ok(courses.asJson)
        }

      case GET -> Root / "courses" / UUIDVar(id) / "students" =>
        CourseRepo.findById(id.toString).map(_.students) match {
          case Some(students) => Ok(students.asJson, Header.Raw(CIString("My-Custom-Header"), "http4s demo"))
          case None => NotFound(s"No Course with id=$id found.")
        }
    }
  }

  def healthRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "health" => Ok(s"Ok! - ${System.currentTimeMillis()}")
    }
  }

//  def allRoutes[F[_]: Monad] = courseRoutes <+> healthRoutes

  def routerWithPrefixes[F[_]: Monad] = Router(
    "/api" -> courseRoutes,
    "/status" -> healthRoutes
  )

  override def run: IO[Unit] = EmberServerBuilder
    .default[IO]
    .withHttpApp(routerWithPrefixes[IO].orNotFound)
    .build
    .use(_ => IO.println("Server ready.") *> IO.never)
}
