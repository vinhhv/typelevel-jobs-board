package com.rockthejvm.jobsboard.http.routes

import cats.effect.*
import cats.implicits.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.auth.*
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.http.responses.*
import com.rockthejvm.jobsboard.http.validation.syntax.*
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import tsec.authentication.{asAuthed, SecuredRequestHandler}

import scala.language.implicitConversions
class AuthRoutes[F[_]: Concurrent: Logger] private (auth: Auth[F]) extends HttpValidationDsl[F] {

  private val authenticator                     = auth.authenticator
  private val securedHandler: SecuredHandler[F] = SecuredRequestHandler(authenticator)

  // POST /auth/login { LoginInfo } => 200 OK with Authorization: Bearer {jwt}
  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    req.validate[LoginInfo] { loginInfo =>
      val maybeJwtToken = for {
        maybeToken <- auth.login(loginInfo.email, loginInfo.password)
        _          <- Logger[F].info(s"User loggin in : ${loginInfo.email}")
      } yield maybeToken

      maybeJwtToken.map {
        case Some(token) => authenticator.embed(Response(Status.Ok), token)
        case None        => Response(Status.Unauthorized)
      }
    }
  }

  // POST /auth/users { NewUserInfo } => 201 Created
  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "users" =>
    req.validate[NewUserInfo] { newUserInfo =>
      for {
        maybeNewUser <- auth.signUp(newUserInfo)
        resp <- maybeNewUser match {
          case Some(user) => Created(user.email)
          case None       => BadRequest(s"User with email ${newUserInfo.email} already exists.")
        }
      } yield resp
    }
  }

  // PUT /auth/users/password { NewPasswordInfo } { Authorization: Bearer {jwt} } => 200 OK
  private val changePasswordRoute: AuthRoute[F] = { case req @ PUT -> Root / "users" / "password" asAuthed user =>
    req.request.validate[NewPasswordInfo] { newPasswordInfo =>
      for {
        maybeUserOrError <- auth.changePassword(user.email, newPasswordInfo)
        resp <- maybeUserOrError match {
          case Right(Some(_)) => Ok()
          case Right(None)    => NotFound(FailureResponse(s"User ${user.email} not found."))
          case Left(_)        => Forbidden()
        }
      } yield resp
    }
  }

  // POST /auth/logout { Authorization: Bearer {jwt} } => 200 OK
  private val logoutRoute: AuthRoute[F] = { case req @ POST -> Root / "logout" asAuthed _ =>
    val token = req.authenticator
    for {
      _    <- authenticator.discard(token)
      resp <- Ok()
    } yield resp
  }

  // DELETE /auth/users/vinh@rockthejvm.com
  private val deleteUserRoute: AuthRoute[F] = { case req @ DELETE -> Root / "users" / email asAuthed user =>
    // auth - delete user
    auth.delete(email).flatMap {
      case true  => Ok()
      case false => NotFound()
    }
  }

  val unauthedRoutes = loginRoute <+> createUserRoute
  val authedRoutes = securedHandler.liftService(
    changePasswordRoute.restrictedTo(allRoles) |+|
      logoutRoute.restrictedTo(allRoles) |+|
      deleteUserRoute.restrictedTo(adminOnly)
  )

  val routes = Router(
    "/auth" -> (unauthedRoutes <+> authedRoutes)
  )
}

object AuthRoutes {
  def apply[F[_]: Concurrent: Logger](auth: Auth[F]) =
    new AuthRoutes[F](auth)
}
