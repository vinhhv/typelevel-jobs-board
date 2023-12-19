package com.rockthejvm.jobsboard.pages

import cats.effect.IO
import io.circe.generic.auto.*
import io.circe.parser.*
import tyrian.*
import tyrian.Html.*
import tyrian.http.*

import com.rockthejvm.jobsboard.*
import com.rockthejvm.jobsboard.common.*
import com.rockthejvm.jobsboard.domain.auth.RecoverPasswordInfo

// email, token, new password + button
final case class ResetPasswordPage(
    email: String = "",
    token: String = "",
    password: String = "",
    status: Option[Page.Status] = None
) extends FormPage("Reset Password", status) {
  import ResetPasswordPage.*

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case UpdateEmail(email) =>
      (this.copy(email = email), Cmd.None)
    case UpdateToken(token) =>
      (this.copy(token = token), Cmd.None)
    case UpdatePassword(password) =>
      (this.copy(password = password), Cmd.None)
    case AttemptResetPassword =>
      if (!Constants.emailRegex.matches(email))
        (setErrorStatus("Please insert a valid email."), Cmd.None)
      else if (token.isEmpty)
        (setErrorStatus("Please add a token."), Cmd.None)
      else if (password.isEmpty)
        (setErrorStatus("Please add a password."), Cmd.None)
      else
        (this, Commands.resetPassword(email, token, password))
    case ResetPasswordFailure(error) =>
      (setErrorStatus(error), Cmd.None)
    case ResetPasswordSuccess =>
      (setSuccessStatus("Success! You can log in now."), Cmd.None)

    case _ => (this, Cmd.None) // TODO
  }

  override protected def renderFormContent(): List[Html[App.Msg]] = List(
    renderInput("Email", "email", "text", true, UpdateEmail(_)),
    renderInput("Token", "token", "text", true, UpdateToken(_)),
    renderInput("Password", "password", "password", true, UpdatePassword(_)),
    button(`type` := "button", onClick(AttemptResetPassword))("Set Password"),
    renderAuxLink(Page.Urls.FORGOT_PASSWORD, "Don't have a token yet?")
  )

  // util
  def setErrorStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))
  def setSuccessStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))
}

object ResetPasswordPage {
  trait Msg                                      extends App.Msg
  case class UpdateEmail(email: String)          extends Msg
  case class UpdateToken(token: String)          extends Msg
  case class UpdatePassword(password: String)    extends Msg
  case object AttemptResetPassword               extends Msg
  case class ResetPasswordFailure(error: String) extends Msg
  case object ResetPasswordSuccess               extends Msg

  object Endpoints {
    val resetPassword = new Endpoint[Msg] {
      override val location: String          = Constants.endpoints.resetPassword
      override val method: Method            = Method.Post
      override val onError: HttpError => Msg = e => ResetPasswordFailure(e.toString)
      override val onResponse: Response => Msg = response =>
        response.status match {
          case Status(200, _) => ResetPasswordSuccess
          case Status(s, _) if s >= 400 && s < 500 =>
            val json   = response.body
            val parsed = parse(json).flatMap(_.hcursor.get[String]("error"))
            parsed match {
              case Left(e)          => ResetPasswordFailure(s"Response error: ${e.getMessage}")
              case Right(niceError) => ResetPasswordFailure(niceError)
            }
        }
    }
  }

  object Commands {
    def resetPassword(email: String, token: String, password: String): Cmd[IO, Msg] =
      Endpoints.resetPassword.call(RecoverPasswordInfo(email, token, password))
  }
}
