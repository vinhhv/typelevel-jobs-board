package com.rockthejvm.jobsboard

import cats.effect.IO
import org.scalajs.dom.window
import scala.scalajs.js.annotation.*
import tyrian.*
import tyrian.Html.*
import tyrian.cmds.Logger

import components.*
import core.*
import pages.*

import scala.concurrent.duration.given

object App {
  trait Msg

  case object NoOp extends Msg

  case class Model(router: Router, session: Session, page: Page)
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
    val location            = window.location.pathname
    val page                = Page.get(location)
    val pageCmd             = page.initCmd
    val (router, routerCmd) = Router.startAt(location)
    val session             = Session()
    val sessionCmd          = session.initCmd
    (Model(router, session, page), routerCmd |+| sessionCmd |+| pageCmd)
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
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case msg: Router.Msg =>
      val (newRouter, routerCmd) = model.router.update(msg)
      if (model.router == newRouter) // no change is necessary
        (model, Cmd.None)
      else {
        // location changed, need to re-render the appropriate page
        val newPage    = Page.get(newRouter.location)
        val newPageCmd = newPage.initCmd
        (model.copy(router = newRouter, page = newPage), routerCmd |+| newPageCmd)
      }
    case msg: Session.Msg =>
      val (newSession, newCmd) = model.session.update(msg)
      (model.copy(session = newSession), newCmd)
    case msg: App.Msg =>
      val (newPage, cmd) = model.page.update(msg)
      (model.copy(page = newPage), cmd)
  }

  // view triggered whenever model changes
  def view(model: Model): Html[Msg] =
    div(
      Header.view(),
      main(
        div(`class` := "container-fluid")(
          model.page.view()
        )
      )
    )

}
