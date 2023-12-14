package com.rockthejvm.jobsboard.components

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import tyrian.*
import tyrian.Html.*

import com.rockthejvm.jobsboard.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.core.Router.ChangeLocation
import com.rockthejvm.jobsboard.pages.*

object Header {
  // public API
  def view(): Html[App.Msg] =
    div(`class` := "header-container")(
      renderLogo(),
      div(`class` := "header-nav")(
        ul(`class` := "header-links")(
          renderNavLinks()
        )
      )
    )

  // private api
  @js.native
  @JSImport("/static/img/logo.png", JSImport.Default)
  private val logoImage: String = js.native

  private def renderLogo() =
    a(
      href := "/",
      onEvent(
        "click",
        e => {
          e.preventDefault() // native JS - prevent reloading the page
          Router.ChangeLocation("/")
        }
      )
    )(
      img(
        `class` := "home-logo",
        src     := logoImage,
        alt     := "Rock the JVM"
      )
    )

  private def renderNavLinks(): List[Html[App.Msg]] = {
    val constantLinks = List(
      renderSimpleNavLink("Jobs", Page.Urls.JOBS)
    )

    val unauthedLinks = List(
      renderSimpleNavLink("Login", Page.Urls.LOGIN),
      renderSimpleNavLink("Sign up", Page.Urls.SIGNUP)
    )

    val authedLinks = List(
      renderNavLink("Log Out", Page.Urls.HASH)(_ => Session.Logout)
    )

    constantLinks ++ (
      if (Session.isActive) authedLinks
      else unauthedLinks
    )
  }

  private def renderSimpleNavLink(text: String, location: String) =
    renderNavLink(text, location)(Router.ChangeLocation(_))

  private def renderNavLink(text: String, location: String)(location2msg: String => App.Msg) =
    li(`class` := "nav-item")(
      a(
        href    := location,
        `class` := "nav-link",
        onEvent(
          "click",
          e => {
            e.preventDefault() // native JS - prevent reloading the page
            location2msg(location)
          }
        )
      )(text)
    )
}
