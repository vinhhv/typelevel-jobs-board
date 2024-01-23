package com.rockthejvm.jobsboard.playground

import cats.effect.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.job.*
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.util.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.io.StdIn

object JobsPlayground extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val postgresResource: Resource[IO, HikariTransactor[IO]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool(32)
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        "jdbc:postgresql:board",
        "docker",
        "docker",
        ec
      )
    } yield xa

  val jobInfo = JobInfo.minimal(
    company = "Rock the JVM",
    title = "Software Engineer",
    description = "Best job ever",
    externalUrl = "rockthejvm.com",
    remote = true,
    location = "Anywhere"
  )

  override def run: IO[Unit] = postgresResource.use { xa =>
    for {
      jobs      <- LiveJobs[IO](xa)
      _         <- IO(println("Ready. Next...")) *> IO(StdIn.readLine)
      id        <- jobs.create("vinh@rockthejvm.com", jobInfo)
      _         <- IO(println("Next...")) *> IO(StdIn.readLine)
      list      <- jobs.all().compile.toList
      _         <- IO(println(s"All jobs: $list. Next...")) *> IO(StdIn.readLine)
      _         <- jobs.update(id, jobInfo.copy(title = "Software Rockstar"))
      newJob    <- jobs.find(id)
      _         <- IO(println(s"New job: $newJob. Next...")) *> IO(StdIn.readLine)
      _         <- jobs.delete(id)
      listAfter <- jobs.all().compile.toList
      _         <- IO(println(s"Deleted job. List now: $listAfter. Next...")) *> IO(StdIn.readLine)
    } yield ()
  }
}
