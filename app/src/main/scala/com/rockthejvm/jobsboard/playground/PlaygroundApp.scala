package com.rockthejvm.jobsboard

import cats.effect.IO
import org.scalajs.dom.{console, document}
import scala.scalajs.js.annotation.*
import tyrian.*
import tyrian.Html.*
import tyrian.cmds.Logger

import scala.concurrent.duration.*

object PlaygroundApp {
  sealed trait Msg
  case class Increment(amount: Int) extends Msg
  case class Model(count: Int)
}

class PlaygroundApp extends TyrianApp[PlaygroundApp.Msg, PlaygroundApp.Model] {
  import PlaygroundApp.*
  /*
   We can send message by
   - trigger a command
   - create a subscription
   - listening for an event
   */
  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(0), Cmd.None)

  // potentially endless stream of messages
  def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.every[IO](1.second).map(_ => Increment(1))

  // model can change by receiving messages
  // model => message => (new model, new command)
  // update triggered whenever we get a new message
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = { case Increment(amount) =>
    (model.copy(count = model.count + amount), Logger.consoleLog[IO]("Changing count by " + amount))
  }

  // view triggered whenever model changes
  def view(model: Model): Html[Msg] =
    div(
      button(onClick(Increment(1)))("increase"),
      button(onClick(Increment(-1)))("decrease"),
      div(s"Tyrian running: ${model.count}")
    )
}
