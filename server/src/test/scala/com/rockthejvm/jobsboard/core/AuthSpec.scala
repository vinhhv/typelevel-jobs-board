package com.rockthejvm.jobsboard.core

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.rockthejvm.jobsboard.config.SecurityConfig
import com.rockthejvm.jobsboard.domain.auth.*
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.fixtures.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import scala.concurrent.duration.*

class AuthSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with UserFixture {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val mockedConfig = SecurityConfig("secret", 1.day)

  private val _token = "abc123"

  private val mockedTokens: Tokens[IO] = new Tokens[IO] {
    override def getToken(email: String): IO[Option[String]] =
      if (email == vinhEmail) IO.pure(Some(_token))
      else IO.pure(None)

    override def checkToken(email: String, token: String): IO[Boolean] =
      IO.pure(token == _token)
  }

  private val mockedEmails: Emails[IO] = new Emails[IO] {
    override def sendEmail(to: String, subject: String, content: String): IO[Unit] = IO.unit

    override def sendPasswordRecoveryEmail(to: String, token: String): IO[Unit] = IO.unit
  }

  def probedEmails(users: Ref[IO, Set[String]]): Emails[IO] = new Emails[IO] {
    override def sendEmail(to: String, subject: String, content: String): IO[Unit] =
      users.modify(set => (set + to, ()))

    override def sendPasswordRecoveryEmail(to: String, token: String): IO[Unit] =
      sendEmail(to, "your token", "token")
  }

  "Auth 'algebra'" - {
    "login should return None if the user doesn't exist" in {
      val program = for {
        auth       <- LiveAuth[IO](mockUsers, mockedTokens, mockedEmails)
        maybeToken <- auth.login("user@rockthejvm.com", "password")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return None if the user exists but the password is incorrect" in {
      val program = for {
        auth       <- LiveAuth[IO](mockUsers, mockedTokens, mockedEmails)
        maybeToken <- auth.login(vinhEmail, "wrongpassword")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return a token if the user exists and the password is correct" in {
      val program = for {
        auth       <- LiveAuth[IO](mockUsers, mockedTokens, mockedEmails)
        maybeToken <- auth.login(vinhEmail, vinhPassword)
      } yield maybeToken

      program.asserting(_ shouldBe defined)
    }

    "signing up should not create a user with an existing email" in {
      val program = for {
        auth <- LiveAuth[IO](mockUsers, mockedTokens, mockedEmails)
        maybeUser <- auth.signUp(
          NewUserInfo(vinhEmail, "somePassword", Some("Vinh"), Some("Whatever"), Some("Other company"))
        )
      } yield maybeUser

      program.asserting(_ shouldBe None)
    }

    "signing up should create a new user" in {
      val program = for {
        auth <- LiveAuth[IO](mockUsers, mockedTokens, mockedEmails)
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
        auth   <- LiveAuth[IO](mockUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword("alice@rockthejvm.com", NewPasswordInfo("oldpw", "newpw"))
      } yield result

      program.asserting(_ shouldBe Right(None))
    }

    "changePassword should return Left with an error if the user exists and the password is incorrect" in {
      val program = for {
        auth   <- LiveAuth[IO](mockUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword(vinhEmail, NewPasswordInfo("oldpw", "newpw"))
      } yield result

      program.asserting(_ shouldBe Left("Invalid password"))
    }

    "changePassword should correctly change password if all details are correct" in {
      val newPassword = "scalarocks"
      val program = for {
        auth   <- LiveAuth[IO](mockUsers, mockedTokens, mockedEmails)
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

    "recoverPassword should fail for a user that does not exist, even if the token is correct" in {
      val program = for {
        auth    <- LiveAuth[IO](mockUsers, mockedTokens, mockedEmails)
        result1 <- auth.recoverPasswordFromToken("someone@gmail.com", _token, "igotya")
        result2 <- auth.recoverPasswordFromToken("someone@gmail.com", "wrongtoken", "igotya")
      } yield (result1, result2)

      program.asserting(_ shouldBe (false, false))
    }

    "recoverPassword should fail for a user that does exists, but token is wrong" in {
      val program = for {
        auth   <- LiveAuth[IO](mockUsers, mockedTokens, mockedEmails)
        result <- auth.recoverPasswordFromToken(vinhEmail, "wrongtoken", "h4cked")
      } yield result

      program.asserting(_ shouldBe false)
    }

    "recoverPassword should succeed for a correct combination of user/token" in {
      val program = for {
        auth   <- LiveAuth[IO](mockUsers, mockedTokens, mockedEmails)
        result <- auth.recoverPasswordFromToken(vinhEmail, _token, "rockstar")
      } yield result

      program.asserting(_ shouldBe true)
    }

    "sending recovery passwords should fail for a user that doesn't exist" in {
      val program = for {
        set                  <- Ref.of[IO, Set[String]](Set())
        emails               <- IO(probedEmails(set))
        auth                 <- LiveAuth[IO](mockUsers, mockedTokens, emails)
        result               <- auth.sendPasswordRecoveryToken("someone@whatever.com")
        usersBeingSentEmails <- set.get
      } yield usersBeingSentEmails

      program.asserting(_ shouldBe empty)
    }

    "sending recovery passwords should succeed for a user that exists" in {
      val program = for {
        set                  <- Ref.of[IO, Set[String]](Set())
        emails               <- IO(probedEmails(set))
        auth                 <- LiveAuth[IO](mockUsers, mockedTokens, emails)
        result               <- auth.sendPasswordRecoveryToken(vinhEmail)
        usersBeingSentEmails <- set.get
      } yield usersBeingSentEmails

      program.asserting(_ should contain(vinhEmail))
    }

  }
}
