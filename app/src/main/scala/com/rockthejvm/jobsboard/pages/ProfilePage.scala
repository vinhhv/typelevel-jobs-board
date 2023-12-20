package com.rockthejvm.jobsboard.pages

import cats.effect.IO
import io.circe.generic.auto.*
import io.circe.parser.*
import tyrian.*
import tyrian.Html.*
import tyrian.http.*

import com.rockthejvm.jobsboard.*
import com.rockthejvm.jobsboard.common.*
import com.rockthejvm.jobsboard.core.Session
import com.rockthejvm.jobsboard.domain.auth.*

final case class ProfilePage(
    oldPassword: String = "",
    newPassword: String = "",
    status: Option[Page.Status] = None
) extends FormPage("Profile", status) {
  import ProfilePage.*

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case UpdateOldPassword(password) =>
      (this.copy(oldPassword = password), Cmd.None)
    case UpdateNewPassword(password) =>
      (this.copy(newPassword = password), Cmd.None)
    case AttemptChangePassword =>
      if (oldPassword.isEmpty)
        (setErrorStatus("Please enter your old password"), Cmd.None)
      else if (newPassword.isEmpty)
        (setErrorStatus("Please enter a new password"), Cmd.None)
      else
        (this, Commands.changePassword(oldPassword, newPassword))
    case ChangePasswordFailure(e) =>
      (setErrorStatus(e), Cmd.None)
    case ChangePasswordSuccess =>
      (setSuccessStatus("Success! Password updated."), Cmd.None)
    case _ => (this, Cmd.None)
  }

  override def view(): Html[App.Msg] =
    if (Session.isActive) super.view()
    else renderInvalidPage

  override protected def renderFormContent(): List[Html[App.Msg]] = List(
    renderInput("Old Password", "oldpassword", "password", true, UpdateOldPassword(_)),
    renderInput("New Password", "newpassword", "password", true, UpdateNewPassword(_)),
    button(`type` := "button", onClick(AttemptChangePassword))("Change Password")
  )

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // private
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // UI
  private def renderInvalidPage =
    div(
      h1("Profile"),
      div("Ouch! It seems you're not logged in yet.")
    )

  // util
  def setErrorStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))
  def setSuccessStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))
}

object ProfilePage {
  trait Msg                                      extends App.Msg
  case class UpdateOldPassword(password: String) extends Msg
  case class UpdateNewPassword(password: String) extends Msg
  // actions
  case object AttemptChangePassword               extends Msg
  case class ChangePasswordFailure(error: String) extends Msg
  case object ChangePasswordSuccess               extends Msg

  object Endpoints {
    val changePassword = new Endpoint[Msg] {
      override val location: String          = Constants.endpoints.changePassword
      override val method: Method            = Method.Put
      override val onError: HttpError => Msg = e => ChangePasswordFailure(e.toString)
      override val onResponse: Response => Msg = response =>
        response.status match {
          case Status(200, _) => ChangePasswordSuccess
          case Status(404, _) => ChangePasswordFailure("Invalid token. Please try logging out and back in.")
          case Status(s, _) if s >= 400 && s < 500 => ChangePasswordFailure("Invalid credentials.")
          case _ => ChangePasswordFailure("Unknown reply from server. Please contact admin.")
        }
    }
  }

  object Commands {
    def changePassword(oldPassword: String, newPassword: String): Cmd[IO, Msg] =
      Endpoints.changePassword.callAuthorized(NewPasswordInfo(oldPassword, newPassword))
  }
}
