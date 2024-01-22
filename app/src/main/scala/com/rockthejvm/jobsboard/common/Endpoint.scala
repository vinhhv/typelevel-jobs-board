package com.rockthejvm.jobsboard.common

import cats.effect.IO
import io.circe.Encoder
import io.circe.parser.*
import io.circe.syntax.*
import tyrian.*
import tyrian.http.*

import com.rockthejvm.jobsboard.core.Session

trait Endpoint[M] {
  val location: String
  val method: Method
  val onResponse: Response => M
  val onError: HttpError => M

  def call[A: Encoder](payload: A): Cmd[IO, M] =
    internalCall(payload, None)

  def call(): Cmd[IO, M] =
    internalCall(None)

  def callAuthorized[A: Encoder](payload: A): Cmd[IO, M] =
    internalCall(payload, Session.getUserToken())

  def callAuthorized(): Cmd[IO, M] =
    internalCall(Session.getUserToken())

  // private api
  private def internalCall[A: Encoder](payload: A, authorization: Option[String]): Cmd[IO, M] =
    Http.send(
      Request(
        url = location,
        method = method,
        headers = authorization.map(token => Header("Authorization", token)).toList,
        body = Body.json(payload.asJson.toString),
        timeout = Request.DefaultTimeOut,
        withCredentials = false
      ),
      Decoder[M](onResponse, onError)
    )

  private def internalCall(authorization: Option[String]): Cmd[IO, M] =
    Http.send(
      Request(
        url = location,
        method = method,
        headers = authorization.map(token => Header("Authorization", token)).toList,
        body = Body.Empty,
        timeout = Request.DefaultTimeOut,
        withCredentials = false
      ),
      Decoder[M](onResponse, onError)
    )
}

object Endpoint {
  def onResponse[A: io.circe.Decoder, Msg](
      valueCb: A => Msg,
      errorCb: String => Msg
  ): Response => Msg = response =>
    response.status match {
      case Status(code, _) if code >= 200 && code < 300 =>
        val json   = response.body
        val parsed = parse(json).flatMap(_.as[A])
        parsed match {
          case Left(parsingError) => errorCb(s"Parsing error: $parsingError")
          case Right(value)       => valueCb(value)
        }
      case Status(code, message) if code >= 400 && code < 600 =>
        errorCb(s"Error: $message")
    }

  def onResponseText[Msg](
      valueCallback: String => Msg,
      errorCallback: String => Msg
  ): Response => Msg = response =>
    response.status match {
      case Status(code, _) if code >= 200 && code < 300 =>
        valueCallback(response.body)
      case Status(code, _) if code >= 400 && code < 500 =>
        val json   = response.body
        val parsed = parse(json).flatMap(_.hcursor.get[String]("error"))
        parsed match {
          case Left(e)  => errorCallback(s"Error: $e")
          case Right(e) => errorCallback(e)
        }
      case _ =>
        errorCallback("Unknown reply from server. Something's fishy.")
    }
}
