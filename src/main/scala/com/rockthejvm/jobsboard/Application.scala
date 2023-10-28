package com.rockthejvm.jobsboard

import cats.*
import cats.effect.*
import cats.implicits.*
import com.rockthejvm.jobsboard.config.*
import com.rockthejvm.jobsboard.config.syntax.*
import com.rockthejvm.jobsboard.http.HttpApi
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.*
import pureconfig.ConfigSource

/*
  1 - add a plain health endpoint to our app
  2 - add minimal configuration
  3 - basic http server layout
 */

object Application extends IOApp.Simple {
  override def run: IO[Unit] =
    ConfigSource.default.loadF[IO, EmberConfig].flatMap { config =>
      EmberServerBuilder
        .default[IO]
        .withHost(config.host)
        .withPort(config.port)
        .withHttpApp(HttpApi[IO].endpoints.orNotFound)
        .build
        .use(_ => IO.println("Server ready!") *> IO.never)
    }
}
