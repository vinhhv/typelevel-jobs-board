package com.rockthejvm.jobsboard.pages

import cats.effect.IO
import tyrian.*
import tyrian.Html.*

import com.rockthejvm.jobsboard.*

final case class ForgotPasswordPage(email: String = "", status: Option[Page.Status] = None)
    extends FormPage("Reset Password", status) {
  import ForgotPasswordPage.*
  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case UpdateEmail(e) =>
      (this.copy(email = e), Cmd.None)
    case _ => (this, Cmd.None)
  }

  // render
  override protected def renderFormContent(): List[Html[App.Msg]] = List(
    renderInput("Email", "email", "text", true, UpdateEmail(_))
  )
}

object ForgotPasswordPage {
  trait Msg                             extends App.Msg
  case class UpdateEmail(email: String) extends Msg
}
