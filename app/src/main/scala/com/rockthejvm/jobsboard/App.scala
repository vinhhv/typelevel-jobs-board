package com.rockthejvm.jobsboard

import cats.effect.IO
import org.scalajs.dom.{console, document, window}
import scala.scalajs.js.annotation.*
import tyrian.*
import tyrian.Html.*
import tyrian.cmds.Logger

import core.*

import scala.concurrent.duration.*

object App {
  type Msg = Router.Msg
  case class Increment(amount: Int) extends Msg
  case class Model(router: Router)
}

@JSExportTopLevel("RockTheJvmApp")
class App extends TyrianApp[App.Msg, App.Model] {
  import App.*
  /*
   We can send message by
   - trigger a command
   - create a subscription
   - listening for an event
   */
  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
    val (router, cmd) = Router.startAt(window.location.pathname)
    (Model(router), cmd)
  }

  // potentially endless stream of messages
  def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.make( // listener for browser history changes
      "urlChange",
      model.router.history.state.discrete
        .map(_.get)
        .map(newLocation => Router.ChangeLocation(newLocation, true))
    )

  // model can change by receiving messages
  // model => message => (new model, new command)
  // update triggered whenever we get a new message
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = { case msg: Router.Msg =>
    val (newRouter, cmd) = model.router.update(msg)
    (model.copy(router = newRouter), cmd)
  }

  // view triggered whenever model changes
  def view(model: Model): Html[Msg] =
    div(
      renderNavLink("Jobs", "/jobs"),
      renderNavLink("Login", "/login"),
      renderNavLink("Sign Up", "/signup"),
      div(s"You are now at: ${model.router.location}")
    )

  private def renderNavLink(text: String, location: String) =
    a(
      href    := location,
      `class` := "nav-link",
      onEvent(
        "click",
        e => {
          e.preventDefault() // native JS - prevent reloading the page
          Router.ChangeLocation(location)
        }
      )
    )(text)
}
