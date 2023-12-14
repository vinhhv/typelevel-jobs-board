package com.rockthejvm.jobsboard.pages

import cats.effect.IO
import tyrian.*
import tyrian.Html.*
import tyrian.http.*

import com.rockthejvm.jobsboard.*
import com.rockthejvm.jobsboard.App.Msg
import com.rockthejvm.jobsboard.core.Router

abstract class FormPage(title: String, status: Option[Page.Status]) extends Page {

  // abstract API
  protected def renderFormContent(): List[Html[Msg]] // for every page to override

  def initCmd: Cmd[IO, Msg] = Cmd.None

  // public API
  override def view(): Html[Msg] =
    renderForm()

  // protected API
  protected def renderForm(): Html[Msg] =
    div(`class` := "form-section")(
      // title: sign up
      div(`class` := "top-section")(
        h1(title)
      ),
      // form
      form(
        name    := "login",
        `class` := "form",
        onEvent(
          "submit",
          e => {
            e.preventDefault()
            App.NoOp
          }
        )
      )(
        renderFormContent()
      ),
      status.map(s => div(s.message)).getOrElse(div())
    )

  // UI
  protected def renderInput(name: String, uid: String, kind: String, isRequired: Boolean, onChange: String => Msg) =
    div(`class` := "form-input")(
      label(`for` := name, `class` := "form-label")(
        if (isRequired) span("*") else span(),
        text(name)
      ),
      input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
    )

  protected def renderAuxLink(location: String, text: String): Html[App.Msg] =
    a(
      href    := location,
      `class` := "aux-link",
      onEvent(
        "click",
        e => {
          e.preventDefault() // native JS - prevent reloading the page
          Router.ChangeLocation(location)
        }
      )
    )(text)

}
