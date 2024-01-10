package com.rockthejvm.jobsboard.components

import cats.effect.IO
import tyrian.*
import tyrian.Html.*

trait Component[Msg, +Model] {
  // send a command upon initiating
  def initCmd: Cmd[IO, Msg]
  // update
  def update(msg: Msg): (Model, Cmd[IO, Msg])
  // render
  def view(): Html[Msg]
}
