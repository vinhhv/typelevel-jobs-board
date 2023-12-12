package com.rockthejvm.jobsboard.playground

import cats.effect.*
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

object PasswordHashingPlayground extends IOApp.Simple {
  override def run: IO[Unit] =
    BCrypt.hashpw[IO]("rockthejvm").flatMap(IO.println) *>
      BCrypt
        .checkpwBool[IO](
          "scalarocks",
          PasswordHash[BCrypt]("$2a$10$a0rzBxby5D1AxZMUxfcGkOFWLfapFkjcXWk1YWqfRxaR3oXyj1tci")
        )
        .flatMap(IO.println) *>
      BCrypt.hashpw[IO]("joerocks").flatMap(IO.println) *>
      BCrypt.hashpw[IO]("simplepassword").flatMap(IO.println)

}
