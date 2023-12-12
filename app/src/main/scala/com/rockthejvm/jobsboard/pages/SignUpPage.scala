package com.rockthejvm.jobsboard.pages

import cats.effect.IO
import tyrian.*
import tyrian.Html.*
import tyrian.cmds.Logger

import com.rockthejvm.jobsboard.common.*

// form
/*
  input
    - email
    - password
    - confirm password
    - first name
    - last name
    - company
  button - trigger sign up

 */
final case class SignUpPage(
    email: String = "",
    password: String = "",
    confirmPassword: String = "",
    firstName: String = "",
    lastName: String = "",
    company: String = "",
    status: Option[Page.Status] = None
) extends Page {
  import SignUpPage.*

  override def initCmd: Cmd[IO, Page.Msg] =
    Cmd.None // TODO

      // update
  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match {
    case UpdateEmail(e)            => (this.copy(email = e), Cmd.None)
    case UpdatePassword(p)         => (this.copy(password = p), Cmd.None)
    case UpdateConfirmPassword(cp) => (this.copy(confirmPassword = cp), Cmd.None)
    case UpdateFirstName(fn)       => (this.copy(firstName = fn), Cmd.None)
    case UpdateLastName(ln)        => (this.copy(lastName = ln), Cmd.None)
    case UpdateCompany(c)          => (this.copy(company = c), Cmd.None)
    case AttemptSignUp =>
      if (!Constants.emailRegex.matches(email))
        (setErrorStatus("Email is invalid"), Cmd.None)
      else if (password.isEmpty)
        (setErrorStatus("Please enter a password"), Cmd.None)
      else if (password != confirmPassword)
        (setErrorStatus("Password fields do not match"), Cmd.None)
      else (this, Logger.consoleLog[IO]("Signing up!", email, password, firstName, lastName, company))

    case _ => (this, Cmd.None)
  }

  // render
  override def view(): Html[Page.Msg] =
    div(`class` := "form-section")(
      // title: sign up
      div(`class` := "top-section")(
        h1("Sign up")
      ),
      // form
      form(
        name    := "signin",
        `class` := "form",
        onEvent(
          "submit",
          e => {
            e.preventDefault()
            NoOp
          }
        )
      )(
        // 6 inputs
        // button
        renderInput("Email", "email", "text", true, UpdateEmail(_)),
        renderInput("Password", "password", "password", true, UpdatePassword(_)),
        renderInput("Confirm password", "cPassword", "password", true, UpdateConfirmPassword(_)),
        renderInput("First name", "firstName", "text", false, UpdateFirstName(_)),
        renderInput("Last name", "lastName", "text", false, UpdateLastName(_)),
        renderInput("Company", "company", "text", false, UpdateCompany(_)),
        // button
        button(`type` := "button", onClick(AttemptSignUp))("Sign up")
      ),
      status.map(s => div(s.message)).getOrElse(div())
    )

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // private
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // UI
  private def renderInput(name: String, uid: String, kind: String, isRequired: Boolean, onChange: String => Msg) =
    div(`class` := "form-input")(
      label(`for` := name, `class` := "form-label")(
        if (isRequired) span("*") else span(),
        text(name)
      ),
      input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
    )

  // util
  def setErrorStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))
}

object SignUpPage {
  trait Msg                                                 extends Page.Msg
  case class UpdateEmail(email: String)                     extends Msg
  case class UpdatePassword(password: String)               extends Msg
  case class UpdateConfirmPassword(confirmPassword: String) extends Msg
  case class UpdateFirstName(firstName: String)             extends Msg
  case class UpdateLastName(lastName: String)               extends Msg
  case class UpdateCompany(company: String)                 extends Msg
  // actions
  case object AttemptSignUp extends Msg
  case object NoOp          extends Msg
}
