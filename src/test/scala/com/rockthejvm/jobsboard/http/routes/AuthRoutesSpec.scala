package com.rockthejvm.jobsboard.http.routes

import cats.data.OptionT
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.auth.*
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.fixtures.*
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.Uri.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.*

class AuthRoutesSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with Http4sDsl[IO] with SecuredRouteFixture {

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// prep
  //////////////////////////////////////////////////////////////////////////////////////////////
  val mockedAuth: Auth[IO] = new Auth[IO] {
    // TODO: Make sure only Vinh already exists
    override def login(email: String, password: String): IO[Option[JwtToken]] =
      if (email == vinhEmail && password == vinhPassword)
        mockedAuthenticator.create(vinhEmail).map(Some(_))
      else IO.pure(None)

    override def signUp(newUserInfo: NewUserInfo): IO[Option[User]] =
      if (newUserInfo.email == joeEmail) IO.pure(Some(Joe))
      else IO.pure(None)

    override def changePassword(email: String, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] =
      if (email == vinhEmail)
        if (newPasswordInfo.oldPassword == vinhPassword) IO.pure(Right(Some(Vinh)))
        else IO.pure(Left("Invalid password"))
      else IO.pure(Right(None))

    override def delete(email: Fragment): IO[Boolean] = IO.pure(true)

    override def authenticator: Authenticator[IO] = mockedAuthenticator
  }

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val authRoutes: HttpRoutes[IO] = AuthRoutes[IO](mockedAuth).routes

  //////////////////////////////////////////////////////////////////////////////////////////////
  /// tests
  //////////////////////////////////////////////////////////////////////////////////////////////
  "AuthRoles" - {
    "should return a 401 - unauthorized if login fails" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(vinhEmail, "wrongpassword"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - OK + a JWT if login is successful" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(vinhEmail, vinhPassword))
        )
      } yield {
        response.status shouldBe Status.Ok
        response.headers.get(ci"Authorization") shouldBe defined
      }
    }

    "should return a 400 - Bad Request if the user to create already exists" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserVinh)
        )
      } yield response.status shouldBe Status.BadRequest
    }

    "should return a 201 - Created if the user creation succeeds" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserJoe)
        )
      } yield {
        response.status shouldBe Status.Created
      }
    }

    "should return a 200 - OK if logging out with a valid JWT token" in {
      for {
        jwtToken <- mockedAuthenticator.create(vinhEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return a 401 - Unauthorized if logging out without a valid JWT token" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    // change password - user doesn't exist => 404 Not Found
    // change password - invalid old password => 403 Forbidden
    // change password - user JWT is invalid => 401 Unauthorized
    // change password - happy path 200
    "should return a 404 - Not Found if changing password for a user that doesn't exist" in {
      for {
        jwtToken <- mockedAuthenticator.create(joeEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(joePassword, "newpassword"))
        )
      } yield {
        response.status shouldBe Status.NotFound
      }
    }

    "should return a 403 - Forbidden if old password is incorrect" in {
      for {
        jwtToken <- mockedAuthenticator.create(vinhEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo("wrongpassword", "newpassword"))
        )
      } yield {
        response.status shouldBe Status.Forbidden
      }
    }

    "should return a 401 - Unauthorized if changing password without a JWT" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withEntity(NewPasswordInfo(vinhPassword, "newpassword"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - OK if changing password for a user with a valid JWT and password" in {
      for {
        jwtToken <- mockedAuthenticator.create(vinhEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(vinhPassword, "newpassword"))
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return a 401 - Unauthorized if a non-admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(joeEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/vinh@rockthejvm.com")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - Ok if an admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(vinhEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/joe@rockthejvm.com")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

  }
}
