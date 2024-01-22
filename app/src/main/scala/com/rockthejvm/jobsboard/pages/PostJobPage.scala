package com.rockthejvm.jobsboard.pages

import cats.effect.IO
import cats.syntax.traverse.*
import io.circe.generic.auto.*
import io.circe.parser.*
import org.scalajs.dom.{File, FileReader}
import tyrian.*
import tyrian.Html.*
import tyrian.cmds.Logger
import tyrian.http.*

import com.rockthejvm.jobsboard.*
import com.rockthejvm.jobsboard.common.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.job.*

import scala.util.Try

case class PostJobPage(
    company: String = "",
    title: String = "",
    description: String = "",
    externalUrl: String = "",
    remote: Boolean = false,
    location: String = "",
    salaryLo: Option[Int] = None,
    salaryHi: Option[Int] = None,
    currency: Option[String] = None,
    country: Option[String] = None,
    tags: Option[String] = None, // TODO: parse the tags before sending them to the server
    image: Option[String] = None,
    seniority: Option[String] = None,
    other: Option[String] = None,
    status: Option[Page.Status] = None
) extends FormPage("Post Job", status) {
  import PostJobPage.*

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case UpdateCompany(v) =>
      (this.copy(company = v), Cmd.None)
    case UpdateTitle(v) =>
      (this.copy(title = v), Cmd.None)
    case UpdateDescription(v) =>
      (this.copy(description = v), Cmd.None)
    case UpdateExternalUrl(v) =>
      (this.copy(externalUrl = v), Cmd.None)
    case ToggleRemote =>
      (this.copy(remote = !this.remote), Cmd.None)
    case UpdateLocation(v) =>
      (this.copy(location = v), Cmd.None)
    case UpdateSalaryLo(v) =>
      (this.copy(salaryLo = Some(v)), Cmd.None)
    case UpdateSalaryHi(v) =>
      (this.copy(salaryHi = Some(v)), Cmd.None)
    case UpdateCurrency(v) =>
      (this.copy(currency = Some(v)), Cmd.None)
    case UpdateCountry(v) =>
      (this.copy(country = Some(v)), Cmd.None)
    case UpdateImageFile(maybeFile) =>
      (this, Commands.loadFile(maybeFile))
    case UpdateImage(maybeImage) =>
      (this.copy(image = maybeImage), Logger.consoleLog[IO](s"I HAZ IMAGE: $maybeImage"))
    case UpdateTags(v) =>
      (this.copy(tags = Some(v)), Cmd.None)
    case UpdateSeniority(v) =>
      (this.copy(seniority = Some(v)), Cmd.None)
    case UpdateOther(v) =>
      (this.copy(other = Some(v)), Cmd.None)

    // action
    case AttemptPostJob =>
      (
        this,
        Commands.postJob(promoted = true)(
          company,
          title,
          description,
          externalUrl,
          remote,
          location,
          salaryLo,
          salaryHi,
          currency,
          country,
          tags,
          image,
          seniority,
          other
        )
      )

    // status
    case PostJobError(error) =>
      (setErrorStatus(error), Cmd.None)
    case PostJobSuccess(jobId) =>
      (setSuccessStatus("Success!"), Logger.consoleLog[IO](s"Posted job with id $jobId"))

    case _ => (this, Cmd.None)
  }

  override protected def renderFormContent(): List[Html[App.Msg]] =
    if (!Session.isActive) renderInvalidContents()
    else
      List(
        renderInput("Company", "company", "text", true, UpdateCompany(_)),
        renderInput("Title", "title", "text", true, UpdateTitle(_)),
        renderTextArea("Description", "description", true, UpdateDescription(_)),
        renderInput("ExternalUrl", "externalUrl", "text", true, UpdateExternalUrl(_)),
        renderToggle("Remote?", "remote", true, _ => ToggleRemote),
        renderInput("Location", "location", "text", true, UpdateLocation(_)),
        renderInput("salaryLo", "salaryLo", "number", false, value => UpdateSalaryLo(parseNumber(value))),
        renderInput("salaryHi", "salaryHi", "number", false, value => UpdateSalaryHi(parseNumber(value))),
        renderInput("currency", "currency", "text", false, UpdateCurrency(_)),
        renderInput("country", "country", "text", false, UpdateCountry(_)),
        renderImageUploadInput("Logo", "logo", image, UpdateImageFile(_)),
        renderInput("tags", "tags", "text", false, UpdateTags(_)),
        renderInput("seniority", "seniority", "text", false, UpdateSeniority(_)),
        renderInput("other", "other", "text", false, UpdateOther(_)),
        button(`type` := "button", onClick(AttemptPostJob))("Post Job")
      )

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // private
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // UI
  private def renderInvalidContents() = List(
    p(`class` := "form-text")("You need to be logged in to post a job.")
  )

  // util
  private def parseNumber(s: String) =
    Try(s.toInt).getOrElse(0)

  def setErrorStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))
  def setSuccessStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))
}

object PostJobPage {
  trait Msg                                           extends App.Msg
  case class UpdateCompany(company: String)           extends Msg
  case class UpdateTitle(title: String)               extends Msg
  case class UpdateDescription(description: String)   extends Msg
  case class UpdateExternalUrl(externalUrl: String)   extends Msg
  case object ToggleRemote                            extends Msg
  case class UpdateLocation(location: String)         extends Msg
  case class UpdateSalaryLo(salaryLo: Int)            extends Msg
  case class UpdateSalaryHi(salaryHi: Int)            extends Msg
  case class UpdateCurrency(currency: String)         extends Msg
  case class UpdateCountry(country: String)           extends Msg
  case class UpdateImageFile(maybeFile: Option[File]) extends Msg
  case class UpdateImage(maybeImage: Option[String])  extends Msg
  case class UpdateTags(tags: String)                 extends Msg
  case class UpdateSeniority(seniority: String)       extends Msg
  case class UpdateOther(other: String)               extends Msg
  // Actions
  case object AttemptPostJob               extends Msg
  case class PostJobError(error: String)   extends Msg
  case class PostJobSuccess(jobId: String) extends Msg

  object Endpoints {
    val postJob = new Endpoint[Msg] {
      override val location: String            = Constants.endpoints.postJob
      override val method: Method              = Method.Post
      override val onError: HttpError => Msg   = e => PostJobError(e.toString)
      override val onResponse: Response => Msg = Endpoint.onResponseText(PostJobSuccess(_), PostJobError(_))
    }

    val postJobPromoted = new Endpoint[App.Msg] {
      override val location: String              = Constants.endpoints.postJobPromoted
      override val method: Method                = Method.Post
      override val onError: HttpError => App.Msg = e => PostJobError(e.toString)
      override val onResponse: Response => App.Msg =
        Endpoint.onResponseText(Router.ExternalRedirect(_), PostJobError(_))
    }
  }

  object Commands {
    def postJob(promoted: Boolean = true)(
        company: String,
        title: String,
        description: String,
        externalUrl: String,
        remote: Boolean,
        location: String,
        salaryLo: Option[Int],
        salaryHi: Option[Int],
        currency: Option[String],
        country: Option[String],
        tags: Option[String],
        image: Option[String],
        seniority: Option[String],
        other: Option[String]
    ) = {
      val endpoint =
        if (promoted) Endpoints.postJobPromoted
        else Endpoints.postJob
      endpoint.callAuthorized(
        JobInfo(
          company,
          title,
          description,
          externalUrl,
          remote,
          location,
          salaryLo,
          salaryHi,
          currency,
          country,
          tags.map(text => text.split(",").map(_.trim).toList),
          image,
          seniority,
          other
        )
      )
    }

    def loadFile(maybeFile: Option[File]) =
      Cmd.Run[IO, Option[String], Msg](
        // run the effect here that returns an Option[String]
        // Option[File] => Option[String]
        // Option[File].traverse(file => IO[String]) => IO[Option[String]]
        maybeFile.traverse { file =>
          IO.async_ { cb =>
            // create a reader
            val reader = new FileReader
            // set the onload
            reader.onload = _ => cb(Right(reader.result.toString))
            // trigger the reader
            reader.readAsDataURL(file)
          }
        }
      )(UpdateImage(_))
  }
}
