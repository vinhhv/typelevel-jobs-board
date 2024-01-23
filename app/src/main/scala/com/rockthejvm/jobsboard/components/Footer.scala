package com.rockthejvm.jobsboard.components

import tyrian.*
import tyrian.Html.*

import com.rockthejvm.jobsboard.*

object Footer {
  def view(): Html[App.Msg] =
    div(`class` := "footer")(
      p(
        text("Written in "),
        a(href := "https://scala-lang.org", target := "blank")("Scala"),
        text(" by the one and only Vinh")
      )
    )
}
