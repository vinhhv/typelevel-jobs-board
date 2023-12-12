package com.rockthejvm.jobsboard.core

import cats.effect.*
import cats.implicits.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.auth.*
import com.rockthejvm.jobsboard.domain.user.*
import org.typelevel.log4cats.Logger
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import scala.concurrent.duration.*

trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[User]]
  def signUp(newUserInfo: NewUserInfo): F[Option[User]]
  def changePassword(email: String, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]]
  def delete(email: String): F[Boolean]

  def sendPasswordRecoveryToken(email: String): F[Unit]
  def recoverPasswordFromToken(email: String, token: String, newPassword: String): F[Boolean]
}

class LiveAuth[F[_]: Async: Logger] private (users: Users[F], tokens: Tokens[F], emails: Emails[F]) extends Auth[F] {
  override def login(email: String, password: String): F[Option[User]] =
    for {
      // find the user in the DB -> return None if no user
      maybeUser <- users.find(email)
      // check password
      // Option[User].filterA(User => G[Boolean]) => G[Option[User])
      maybeValidatedUser <- maybeUser.filterA { user =>
        BCrypt.checkpwBool[F](
          password,
          PasswordHash[BCrypt](user.hashedPassword)
        )
      }
    } yield maybeValidatedUser
  override def signUp(newUserInfo: NewUserInfo): F[Option[User]] =
    // find the user in the db, if we did => None
    users.find(newUserInfo.email).flatMap {
      case Some(_) => None.pure[F]
      case None =>
        for {
          // hash the new password
          hashedPassword <- BCrypt.hashpw[F](newUserInfo.password)
          user <- User(
            newUserInfo.email,
            hashedPassword,
            newUserInfo.firstName,
            newUserInfo.lastName,
            newUserInfo.company,
            Role.RECRUITER
          ).pure[F]
          // create a new user in the db
          _ <- users.create(user)
        } yield Some(user)
    }
  override def changePassword(email: String, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]] = {
    def checkAndUpdate(user: User, oldPassword: String, newPassword: String): F[Either[String, Option[User]]] =
      for {
        passCheck <- BCrypt
          .checkpwBool[F](
            newPasswordInfo.oldPassword,
            PasswordHash[BCrypt](user.hashedPassword)
          )
        updatedResult <-
          if (passCheck) updateUser(user, newPassword).map(Right(_))
          else Left("Invalid password").pure[F]
      } yield updatedResult

    users.find(email).flatMap {
      case None => Right(None).pure[F]
      case Some(user) =>
        val NewPasswordInfo(oldPw, newPw) = newPasswordInfo
        checkAndUpdate(user, oldPw, newPw)
    }
  }

  override def delete(email: String): F[Boolean] =
    users.delete(email)

  override def sendPasswordRecoveryToken(email: String): F[Unit] =
    tokens.getToken(email).flatMap {
      case Some(token) => emails.sendPasswordRecoveryEmail(email, token)
      case None        => ().pure[F]
    }

  override def recoverPasswordFromToken(email: String, token: String, newPassword: String): F[Boolean] =
    for {
      maybeUser    <- users.find(email)
      tokenIsValid <- tokens.checkToken(email, token)
      result <- (maybeUser, tokenIsValid) match {
        case (Some(user), true) => updateUser(user, newPassword).map(_.nonEmpty)
        case _                  => false.pure[F]
      }
    } yield result

  // private
  private def updateUser(user: User, newPassword: String): F[Option[User]] =
    for {
      newHashedPassword <- BCrypt.hashpw[F](newPassword)
      updatedUser       <- users.update(user.copy(hashedPassword = newHashedPassword))
    } yield updatedUser

}

object LiveAuth {
  def apply[F[_]: Async: Logger](users: Users[F], tokens: Tokens[F], emails: Emails[F]): F[LiveAuth[F]] =
    new LiveAuth[F](users, tokens, emails).pure[F]
}
