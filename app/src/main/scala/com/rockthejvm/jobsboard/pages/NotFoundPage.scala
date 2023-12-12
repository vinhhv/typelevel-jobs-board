package com.rockthejvm.jobsboard.pages

import cats.effect.IO
import tyrian.*
import tyrian.Html.*

final case class NotFoundPage() extends Page {
  override def initCmd: Cmd[IO, Page.Msg] =
    Cmd.None // TODO

      // update
  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) =
    (this, Cmd.None) // TODO

  // render
  override def view(): Html[Page.Msg] =
    div("Ouch! This page doesn't exist!")
}
