package com.rockthejvm.jobsboard.core

import cats.effect.*
import cats.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.rockthejvm.jobsboard.config.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.fixtures.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.*

class TokensSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with DoobieSpec with UserFixture {
  override val initScript: String = "sql/recoverytokens.sql"

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Tokens 'algebra'" - {

    "should not create a new token for a non-existing user" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockUsers)(xa, TokenConfig(10000000L))
          token  <- tokens.getToken("somebody@email.com")
        } yield token

        program.asserting(_ shouldBe None)
      }
    }

    "should create a new token for an existing user" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockUsers)(xa, TokenConfig(10000000L))
          token  <- tokens.getToken(vinhEmail)
        } yield token

        program.asserting(_ shouldBe defined)
      }
    }

    "should not validate expired tokens" in {
      transactor.use { xa =>
        val program = for {
          tokens     <- LiveTokens[IO](mockUsers)(xa, TokenConfig(100L))
          maybeToken <- tokens.getToken(vinhEmail)
          _          <- IO.sleep(500.millis)
          isTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken(vinhEmail, token)
            case None        => IO.pure(false)
          }
        } yield isTokenValid

        program.asserting(_ shouldBe false)
      }
    }

    "should validate live tokens" in {
      transactor.use { xa =>
        val program = for {
          tokens     <- LiveTokens[IO](mockUsers)(xa, TokenConfig(10000000L))
          maybeToken <- tokens.getToken(vinhEmail)
          isTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken(vinhEmail, token)
            case None        => IO.pure(false)
          }
        } yield isTokenValid

        program.asserting(_ shouldBe true)
      }
    }

    "should only validate tokens for the user that generated them" in {
      transactor.use { xa =>
        val program = for {
          tokens     <- LiveTokens[IO](mockUsers)(xa, TokenConfig(10000000L))
          maybeToken <- tokens.getToken(vinhEmail)
          isVinhTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken(vinhEmail, token)
            case None        => IO.pure(false)
          }
          isOtherTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken("someone@gmail.com", token)
            case None        => IO.pure(false)
          }
        } yield (isVinhTokenValid, isOtherTokenValid)

        program.asserting { case (isVinhTokenValid, isOtherTokenValid) =>
          isVinhTokenValid shouldBe true
          isOtherTokenValid shouldBe false
        }
      }
    }

  }
}
