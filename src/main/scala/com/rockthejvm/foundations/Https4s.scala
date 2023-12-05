package com.rockthejvm.foundations

import cats.*
import cats.effect.{IO, IOApp}
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{
  OptionalValidatingQueryParamDecoderMatcher,
  QueryParamDecoderMatcher
}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.*
import org.http4s.server.Router
import org.http4s.{Header, HttpRoutes}
import org.typelevel.ci.CIString

import java.util.UUID

object Https4s extends IOApp.Simple {

  // simulate an HTTP server with "students" and "courses"
  type Student = String
  case class Instructor(firstName: String, lastName: String)
  case class Course(
      id: String,
      title: String,
      year: Int,
      students: List[Student],
      instructorName: String
  )

  object CourseRepository {
    // a "database"
    val catsEffectCourse = Course(
      "a501041f-0ee4-4520-bbf2-43ad8f294cd3",
      "Rock the JVM Ultimate Scala course",
      2022,
      List("Vinh", "Master Yoda"),
      "Martin Odersky"
    )

    private val courses: Map[String, Course] = Map(
      catsEffectCourse.id -> catsEffectCourse
    )

    // API
    def findCoursesById(courseId: UUID) = courses.get(courseId.toString)

    def findCoursesByInstructor(name: String): List[Course] =
      courses.values.filter(_.instructorName == name).toList
  }

  // essential REST endpoints
  // GET localhost:8080/courses?instructor=Martin%20Odersky&year=2022
  // GET localhost:8080/courses/a501041f-0ee4-4520-bbf2-43ad8f294cd3/students

  object InstructorQueryParamMatcher
      extends QueryParamDecoderMatcher[String]("instructor")

  object YearQueryParamMatcher
      extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")

  def courseRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "courses" :? InstructorQueryParamMatcher(
            instructor
          ) +& YearQueryParamMatcher(maybeYear) =>
        val courses = CourseRepository.findCoursesByInstructor(instructor)
        maybeYear match {
          case Some(y) =>
            y.fold(
              _ => BadRequest("Parameter 'year' is invalid"),
              year => Ok(courses.filter(_.year == year).asJson)
            )
          case None => Ok(courses.asJson)
        }
      case GET -> Root / "courses" / UUIDVar(courseId) / "students" =>
        CourseRepository.findCoursesById(courseId).map(_.students) match {
          case Some(students) =>
            Ok(
              students.asJson,
              Header.Raw(CIString("My-custom-header"), "rockthejvm")
            )
          case None => NotFound(s"No course with $courseId was found")
        }
    }
  }

  def healthEndpoint[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] { case GET -> Root / "health" =>
      Ok("Alive!")
    }
  }

  def allRoutes[F[_]: Monad]: HttpRoutes[F] =
    courseRoutes[F] <+> healthEndpoint[F]

  def routerWithPathPrefixes = Router(
    "/api"     -> courseRoutes[IO],
    "/private" -> healthEndpoint[IO]
  ).orNotFound

  override def run: IO[Unit] = EmberServerBuilder
    .default[IO]
    .withHttpApp(routerWithPathPrefixes)
    .build
    .use(_ => IO.println("Server ready!") *> IO.never)
}
