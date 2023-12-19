package com.rockthejvm.jobsboard.core

import cats.effect.IO
import org.scalajs.dom.document
import scala.scalajs.js.Date
import tyrian.*
import tyrian.cmds.Logger
import tyrian.http.*

import com.rockthejvm.jobsboard.*
import com.rockthejvm.jobsboard.common.*
import com.rockthejvm.jobsboard.pages.Page

final case class Session(email: Option[String] = None, token: Option[String] = None) {
  import Session.*

  def initCmd: Cmd[IO, Msg] = {
    val maybeCommand = for {
      email <- getCookie(Constants.cookies.email)
      token <- getCookie(Constants.cookies.token)
    } yield Cmd.Emit(SetToken(email, token, isNewUser = false))

    maybeCommand.getOrElse(Cmd.None)
  }

  def update(msg: Msg): (Session, Cmd[IO, App.Msg]) = msg match {
    case SetToken(e, t, isNewUser) =>
      val cookieCmd = Commands.setAllSessionCookies(e, t, true)
      val routerCmd =
        if (isNewUser) Cmd.Emit(Router.ChangeLocation(Page.Urls.HOME))
        else Commands.checkToken // check whether the token is still valid on the server
      (
        this.copy(email = Some(e), token = Some(t)),
        cookieCmd |+| routerCmd
      )
    case CheckToken =>
      (this, Commands.checkToken)
    case KeepToken =>
      (this, Cmd.None)
    // logout
    case Logout =>
      val cmd = token.map(_ => Commands.logout).getOrElse(Cmd.None)
      (this, cmd)
    case LogoutSuccess | InvalidateToken =>
      (
        this.copy(email = None, token = None),
        Commands.clearAllSessionCookies() |+| Cmd.Emit(Router.ChangeLocation(Page.Urls.HOME))
      )
  }
}

object Session {
  trait Msg                                                                   extends App.Msg
  final case class SetToken(email: String, token: String, isNewUser: Boolean) extends Msg
  // check token
  case object CheckToken      extends Msg
  case object KeepToken       extends Msg
  case object InvalidateToken extends Msg
  // logout
  case object Logout        extends Msg
  case object LogoutSuccess extends Msg
  case object LogoutFailure extends Msg

  def isActive = getUserToken().nonEmpty

  def getUserToken() = getCookie(Constants.cookies.token)

  object Endpoints {
    val logout = new Endpoint[Msg] {
      override val location                    = Constants.endpoints.logout
      override val method                      = Method.Post
      override val onResponse: Response => Msg = _ => LogoutSuccess
      override val onError: HttpError => Msg   = _ => LogoutFailure
    }

    val checkToken = new Endpoint[Msg] {
      override val location: String = Constants.endpoints.checkToken
      override val method: Method   = Method.Get
      override val onResponse: Response => Msg = response =>
        response.status match {
          case Status(200, _) => KeepToken
          case _              => InvalidateToken
        }
      override val onError: HttpError => Msg = _ => InvalidateToken
    }
  }

  object Commands {
    def checkToken: Cmd[IO, Msg] = Endpoints.checkToken.callAuthorized()
    def logout: Cmd[IO, Msg]     = Endpoints.logout.callAuthorized()

    def setSessionCookie(name: String, value: String, isFresh: Boolean = false): Cmd[IO, Msg] =
      Cmd.SideEffect[IO] {
        if (getCookie(name).isEmpty || isFresh)
          document.cookie = s"$name=$value;expires=${new Date(Date.now() + Constants.cookies.duration)};path=/"
      }

    def setAllSessionCookies(email: String, token: String, isFresh: Boolean = false): Cmd[IO, Msg] =
      setSessionCookie(Constants.cookies.email, email, isFresh) |+|
        setSessionCookie(Constants.cookies.token, token, isFresh)

    def clearSessionCookie(name: String): Cmd[IO, Msg] =
      Cmd.SideEffect[IO] {
        document.cookie = s"$name=;expires=${new Date(0)};path=/"
      }

    def clearAllSessionCookies(): Cmd[IO, Msg] =
      clearSessionCookie(Constants.cookies.email) |+|
        clearSessionCookie(Constants.cookies.token)
  }

  private def getCookie(name: String): Option[String] =
    document.cookie
      .split(";")
      .map(_.trim)
      .find(_.startsWith(s"$name=")) // option
      .map(_.split("="))
      .map(_(1))
}
