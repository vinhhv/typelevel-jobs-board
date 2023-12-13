package com.rockthejvm.jobsboard.pages

import cats.effect.IO
import tyrian.*
import tyrian.Html.*

import com.rockthejvm.jobsboard.*

final case class NotFoundPage() extends Page {
  override def initCmd: Cmd[IO, App.Msg] =
    Cmd.None // TODO

      // update
  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) =
    (this, Cmd.None) // TODO

  // render
  override def view(): Html[App.Msg] =
    div("Ouch! This page doesn't exist!")
}
