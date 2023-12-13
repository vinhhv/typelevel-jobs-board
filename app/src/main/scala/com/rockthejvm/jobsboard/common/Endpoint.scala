package com.rockthejvm.jobsboard.common

import cats.effect.IO
import io.circe.Encoder
import io.circe.syntax.*
import tyrian.*
import tyrian.http.*

trait Endpoint[M] {
  val location: String
  val method: Method
  val onSuccess: Response => M
  val onError: HttpError => M

  def call[A: Encoder](payload: A): Cmd[IO, M] =
    Http.send(
      Request(
        url = location,
        method = method,
        headers = List(),
        body = Body.json(payload.asJson.toString),
        timeout = Request.DefaultTimeOut,
        withCredentials = false
      ),
      Decoder[M](onSuccess, onError)
    )
}
