package com.rockthejvm.jobsboard

import cats.*
import cats.effect.*
import cats.implicits.*
import com.rockthejvm.jobsboard.config.*
import com.rockthejvm.jobsboard.config.syntax.*
import com.rockthejvm.jobsboard.modules.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.*
import org.http4s.server.middleware.CORS
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource

/*
  1 - add a plain health endpoint to our app
  2 - add minimal configuration
  3 - basic http server layout
 */

object Application extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    ConfigSource.default.loadF[IO, AppConfig].flatMap {
      case AppConfig(postgresConfig, emberConfig, securityConfig, tokenConfig, emailServiceConfig, stripeConfig) =>
        val appResource = for {
          xa      <- Database.makePostgresResource[IO](postgresConfig)
          core    <- Core[IO](xa, tokenConfig, emailServiceConfig, stripeConfig)
          httpApi <- HttpApi[IO](core, securityConfig)
          server <- EmberServerBuilder
            .default[IO]
            .withHost(emberConfig.host)
            .withPort(emberConfig.port)
            .withHttpApp(httpApi.endpoints.orNotFound) // TODO: remove before deploying
            .build
        } yield server
        appResource.use(_ => IO.println("Server ready!") *> IO.never)
    }
}
