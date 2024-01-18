package com.rockthejvm.jobsboard.components

import tyrian.*
import tyrian.Html.*

import com.rockthejvm.jobsboard.*
import com.rockthejvm.jobsboard.core.*

object Anchors {
  def renderSimpleNavLink(text: String, location: String, cssClass: String = "") =
    renderNavLink(text, location, cssClass)(Router.ChangeLocation(_))

  def renderNavLink(text: String, location: String, cssClass: String = "")(location2msg: String => App.Msg) =
    a(
      href    := location,
      `class` := cssClass,
      onEvent(
        "click",
        e => {
          e.preventDefault() // native JS - prevent reloading the page
          location2msg(location)
        }
      )
    )(text)
}
