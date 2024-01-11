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
      Anchors.renderSimpleNavLink("Jobs", Page.Urls.JOBS),
      Anchors.renderSimpleNavLink("Post Job", Page.Urls.POST_JOB)
    )

    val unauthedLinks = List(
      Anchors.renderSimpleNavLink("Login", Page.Urls.LOGIN),
      Anchors.renderSimpleNavLink("Sign up", Page.Urls.SIGNUP)
    )

    val authedLinks = List(
      Anchors.renderSimpleNavLink("Profile", Page.Urls.PROFILE),
      Anchors.renderNavLink("Log Out", Page.Urls.HASH)(_ => Session.Logout)
    )

    constantLinks ++ (
      if (Session.isActive) authedLinks
      else unauthedLinks
    )
  }

}
