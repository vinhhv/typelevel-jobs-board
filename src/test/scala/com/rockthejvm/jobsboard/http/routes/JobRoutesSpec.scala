package com.rockthejvm.jobsboard.http.routes

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.domain.pagination.Pagination
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.fixtures.*
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.Uri.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID

class JobRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with JobFixture
    with SecuredRouteFixture {
  //////////////////////////////////////////////////////////////////////////////////////////////
  /// prep
  //////////////////////////////////////////////////////////////////////////////////////////////
  val jobs: Jobs[IO] = new Jobs[IO] {
    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] =
      IO.pure(NewJobUuid)

    override def all(): IO[List[Job]] =
      IO.pure(List(AwesomeJob))

    override def all(filter: JobFilter, pagination: Pagination): IO[List[Job]] =
      if (filter.remote) IO.pure(List())
      else IO.pure(List(AwesomeJob))

    override def find(id: UUID): IO[Option[Job]] =
      if (id == AwesomeJobUuid) IO.pure(Some(AwesomeJob))
      else IO.pure(None)

    override def update(id: UUID, jobInfo: JobInfo): IO[Option[Job]] =
      if (id == AwesomeJobUuid) IO.pure(Some(AwesomeJob))
      else IO.pure(None)

    override def delete(id: UUID): IO[Int] =
      if (id == AwesomeJobUuid) IO.pure(1)
      else IO.pure(0)
  }

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  // this is what we are testing
  val jobRoutes: HttpRoutes[IO] = JobRoutes[IO](jobs).routes

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// tests
  //////////////////////////////////////////////////////////////////////////////////////////////

  "JobRoutes" - {
    "should return a job with a given id" in {
      val uri       = uri"/jobs"
      val uriWithId = uri / AwesomeJobUuid.toString
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.GET, uri = uriWithId)
        )
        retrieved <- response.as[Job]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe AwesomeJob
      }
    }

    "should return all jobs" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs")
            .withEntity(JobFilter())
        )
        retrieved <- response.as[List[Job]]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List(AwesomeJob)
      }
    }

    "should return all jobs that satisfy a filter" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs")
            .withEntity(JobFilter(remote = true))
        )
        retrieved <- response.as[List[Job]]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List()
      }
    }

    "should create a new job" in {
      for {
        jwtToken <- mockedAuthenticator.create(vinhEmail)
        response <- jobRoutes.orNotFound.run(
          Request[IO](method = Method.POST, uri = uri"/jobs/create")
            .withEntity(AwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
        retrieved <- response.as[UUID]
      } yield {
        response.status shouldBe Status.Created
        retrieved shouldBe NewJobUuid
      }
    }

    "should only update a job that exists" in {
      val uri              = uri"/jobs"
      val uriWithId        = uri / AwesomeJobUuid.toString
      val uriWithInvalidId = uri / NotFoundJobUuid.toString
      for {
        jwtToken <- mockedAuthenticator.create(vinhEmail)
        responseOk <- jobRoutes.orNotFound.run(
          Request[IO](method = Method.PUT, uri = uriWithId)
            .withEntity(UpdatedAwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
        responseInvalid <- jobRoutes.orNotFound.run(
          Request[IO](method = Method.PUT, uri = uriWithInvalidId)
            .withEntity(UpdatedAwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }

    "should forbid the update of a job that the JWT token doesn't 'owns'" in {
      val uri              = uri"/jobs"
      val uriWithId        = uri / AwesomeJobUuid.toString
      val uriWithInvalidId = uri / NotFoundJobUuid.toString
      for {
        jwtToken <- mockedAuthenticator.create("somebody@gmail.com")
        responseOk <- jobRoutes.orNotFound.run(
          Request[IO](method = Method.PUT, uri = uriWithId)
            .withEntity(UpdatedAwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
      } yield {
        responseOk.status shouldBe Status.Unauthorized
      }
    }

    "should only delete a job that exists" in {
      val uri              = uri"/jobs"
      val uriWithId        = uri / AwesomeJobUuid.toString
      val uriWithInvalidId = uri / NotFoundJobUuid.toString
      for {
        jwtToken <- mockedAuthenticator.create(vinhEmail)
        responseOk <- jobRoutes.orNotFound.run(
          Request[IO](method = Method.DELETE, uri = uriWithId)
            .withBearerToken(jwtToken)
        )
        responseInvalid <- jobRoutes.orNotFound.run(
          Request[IO](method = Method.DELETE, uri = uriWithInvalidId)
            .withBearerToken(jwtToken)
        )
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }
  }
}
