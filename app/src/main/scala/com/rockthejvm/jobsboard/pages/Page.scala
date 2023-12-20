package com.rockthejvm.jobsboard.pages

import cats.effect.IO
import tyrian.*

import com.rockthejvm.jobsboard.*

object Page {
  trait Msg

  enum StatusKind {
    case SUCCESS, ERROR, LOADING
  }

  case class Status(message: String, kind: StatusKind)

  object Urls {
    val LOGIN           = "/login"
    val SIGNUP          = "/signup"
    val FORGOT_PASSWORD = "/forgotpassword"
    val RESET_PASSWORD  = "/resetpassword"
    val HOME            = "/"
    val JOBS            = "/jobs"
    val PROFILE         = "/profile"
    val EMPTY           = ""
    val HASH            = "#"
  }

  import Urls.*
  def get(location: String): Page = location match {
    case `LOGIN`                   => LoginPage()
    case `SIGNUP`                  => SignUpPage()
    case `FORGOT_PASSWORD`         => ForgotPasswordPage()
    case `RESET_PASSWORD`          => ResetPasswordPage()
    case `PROFILE`                 => ProfilePage()
    case `EMPTY` | `HOME` | `JOBS` => JobListPage()
    case s"/jobs/$id"              => JobPage(id)
    case _                         => NotFoundPage()
  }
}

abstract class Page {
  // API
  // send a command upon instantiating
  def initCmd: Cmd[IO, App.Msg]

  // update
  def update(msg: App.Msg): (Page, Cmd[IO, App.Msg])

  // render
  def view(): Html[App.Msg]
}
