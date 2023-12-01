package com.rockthejvm.jobsboard.core

import cats.data.OptionT
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.rockthejvm.jobsboard.domain.auth.NewPasswordInfo
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.fixtures.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import scala.concurrent.duration.*

class AuthSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with UserFixture {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val mockUsers: Users[IO] = new Users[IO] {
    override def find(email: String): IO[Option[User]] =
      if (email == vinhEmail) IO.pure(Some(Vinh))
      else IO.pure(None)

    override def create(user: User): IO[String] = IO.pure(user.email)

    override def update(user: User): IO[Option[User]] = IO.pure(Some(user))

    override def delete(email: String): IO[Boolean] = IO.pure(true)
  }

  val mockedAuthenticator: Authenticator[IO] = {
    // key for hashing
    val key = HMACSHA256.unsafeGenerateKey
    // identity store for retrieve users
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == vinhEmail) OptionT.pure(Vinh)
      else if (email == joeEmail) OptionT.pure(Joe)
      else OptionT.none[IO, User]
    // jwt authenticator
    JWTAuthenticator.unbacked.inBearerToken(
      1.day,   // expiration of tokens
      None,    // max idle time (optional)
      idStore, // identity store
      key      // hash key
    )
  }

  "Auth 'algebra'" - {
    "login should return None if the user doesn't exist" in {
      val program = for {
        auth       <- LiveAuth[IO](mockUsers, mockedAuthenticator)
        maybeToken <- auth.login("user@rockthejvm.com", "password")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return None if the user exists but the password is incorrect" in {
      val program = for {
        auth       <- LiveAuth[IO](mockUsers, mockedAuthenticator)
        maybeToken <- auth.login(vinhEmail, "wrongpassword")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return a token if the user exists and the password is correct" in {
      val program = for {
        auth       <- LiveAuth[IO](mockUsers, mockedAuthenticator)
        maybeToken <- auth.login(vinhEmail, vinhPassword)
      } yield maybeToken

      program.asserting(_ shouldBe defined)
    }

    "signing up should not create a user with an existing email" in {
      val program = for {
        auth <- LiveAuth[IO](mockUsers, mockedAuthenticator)
        maybeUser <- auth.signUp(
          NewUserInfo(vinhEmail, "somePassword", Some("Vinh"), Some("Whatever"), Some("Other company"))
        )
      } yield maybeUser

      program.asserting(_ shouldBe None)
    }

    "signing up should create a new user" in {
      val program = for {
        auth <- LiveAuth[IO](mockUsers, mockedAuthenticator)
        maybeUser <- auth.signUp(
          NewUserInfo("bob@rockthejvm.com", "somePassword", Some("Bob"), Some("The Builder"), Some("Company"))
        )
      } yield maybeUser

      program.asserting {
        case Some(User(email, _, firstName, lastName, company, role)) =>
          email shouldBe "bob@rockthejvm.com"
          firstName shouldBe Some("Bob")
          lastName shouldBe Some("The Builder")
          company shouldBe Some("Company")
          role shouldBe Role.RECRUITER
        case _ => fail()
      }
    }

    "changePassword should return Right(None) if the user doesn't exist" in {
      val program = for {
        auth   <- LiveAuth[IO](mockUsers, mockedAuthenticator)
        result <- auth.changePassword("alice@rockthejvm.com", NewPasswordInfo("oldpw", "newpw"))
      } yield result

      program.asserting(_ shouldBe Right(None))
    }

    "changePassword should return Left with an error if the user exists and the password is incorrect" in {
      val program = for {
        auth   <- LiveAuth[IO](mockUsers, mockedAuthenticator)
        result <- auth.changePassword(vinhEmail, NewPasswordInfo("oldpw", "newpw"))
      } yield result

      program.asserting(_ shouldBe Left("Invalid password"))
    }

    "changePassword should correctly change password if all details are correct" in {
      val newPassword = "scalarocks"
      val program = for {
        auth   <- LiveAuth[IO](mockUsers, mockedAuthenticator)
        result <- auth.changePassword(vinhEmail, NewPasswordInfo(vinhPassword, newPassword))
        isNicePw <- result match {
          case Right(Some(user)) =>
            BCrypt.checkpwBool[IO](
              newPassword,
              PasswordHash[BCrypt](user.hashedPassword)
            )
          case _ => IO.pure(false)
        }
      } yield isNicePw

      program.asserting(_ shouldBe true)
    }
  }
}
