package com.rockthejvm.jobsboard.common

import org.scalajs.dom.window
import scala.scalajs.LinkingInfo
import scala.scalajs.js
import scala.scalajs.js.annotation.*

object Constants {
  @js.native
  @JSImport("/static/img/logo.png", JSImport.Default)
  val logoImage: String = js.native

  @js.native
  @JSImport("/static/img/job-image-default.png", JSImport.Default)
  val jobImageDefault: String = js.native

  val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  val defaultPageSize   = 20
  val jobAdvertPriceUSD = 99

  object endpoints {
    val root =
      if (LinkingInfo.developmentMode) "http://localhost:4041"
      else window.location.origin

    val signup          = s"$root/api/auth/users"
    val login           = s"$root/api/auth/login"
    val logout          = s"$root/api/auth/logout"
    val checkToken      = s"$root/api/auth/checkToken"
    val forgotPassword  = s"$root/api/auth/reset"
    val resetPassword   = s"$root/api/auth/recover"
    val changePassword  = s"$root/api/auth/users/password"
    val postJob         = s"$root/api/jobs/create"
    val postJobPromoted = s"$root/api/jobs/promoted"
    val jobs            = s"$root/api/jobs"
    val filters         = s"$root/api/jobs/filters"
  }

  object cookies {
    val duration = 10 * 24 * 3600 * 1000
    val email    = "email"
    val token    = "token"
  }
}
