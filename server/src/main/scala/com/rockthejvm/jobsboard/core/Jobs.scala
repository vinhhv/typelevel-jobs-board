package com.rockthejvm.jobsboard.core

import cats.*
import cats.effect.*
import cats.implicits.*
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.domain.pagination.*
import com.rockthejvm.jobsboard.logging.syntax.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*
import org.typelevel.log4cats.Logger

import java.util.UUID

trait Jobs[F[_]] {
  // "algebra"
  // CRUD

  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]
  def all(): fs2.Stream[F, Job]
  def all(filter: JobFilter, pagination: Pagination): F[List[Job]]
  def find(id: UUID): F[Option[Job]]
  def update(id: UUID, jobInfo: JobInfo): F[Option[Job]]
  def activate(id: UUID): F[Int]
  def delete(id: UUID): F[Int]
  def possibleFilters(): F[JobFilter]
}

/*
  id         : UUID,
  date       : Long,
  ownerEmail : String,
  company    : String,
  title      : String,
  description: String,
  externalUrl: String,
  remote     : Boolean,
  location   : String,
  salaryLo   : Option[Int],
  salaryHi   : Option[Int],
  currency   : Option[String],
  country    : Option[String],
  tags       : Option[List[String]],
  image      : Option[String],
  seniority  : Option[String],
  other      : Option[String],
  active     : Boolean
 */
class LiveJobs[F[_]: MonadCancelThrow: Logger] private (xa: Transactor[F]) extends Jobs[F] {
  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID] =
    sql"""
      INSERT INTO jobs(
        date,
        ownerEmail,
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
        other,
        active
     ) VALUES (
      ${System.currentTimeMillis()},
      $ownerEmail,
      ${jobInfo.company},
      ${jobInfo.title},
      ${jobInfo.description},
      ${jobInfo.externalUrl},
      ${jobInfo.remote},
      ${jobInfo.location},
      ${jobInfo.salaryLo},
      ${jobInfo.salaryHi},
      ${jobInfo.currency},
      ${jobInfo.country},
      ${jobInfo.tags},
      ${jobInfo.image},
      ${jobInfo.seniority},
      ${jobInfo.other},
      false
     )
   """.update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(xa)

  def all(): fs2.Stream[F, Job] =
    sql"""
      SELECT
        id,
        date,
        ownerEmail,
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
        other,
        active
      FROM jobs
      WHERE active = true
       """
      .query[Job]
      .stream
      .transact(xa)

  def all(filter: JobFilter, pagination: Pagination): F[List[Job]] = {
    val selectFragment: Fragment =
      fr"""
        SELECT
          id,
          date,
          ownerEmail,
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
          other,
          active
       """

    val fromFragment: Fragment =
      fr"FROM jobs"

    val whereFragment: Fragment = Fragments.whereAndOpt(
      filter.companies.toNel.map(companies => Fragments.in(fr"company", companies)),
      filter.locations.toNel.map(locations => Fragments.in(fr"location", locations)),
      filter.countries.toNel.map(countries => Fragments.in(fr"country", countries)),
      filter.seniorities.toNel.map(seniorities => Fragments.in(fr"country", seniorities)),
      filter.tags.toNel.map(tags => // intersection between filter.tags and row's tags
        Fragments.or(tags.toList.map(tag => fr"$tag=any(tags)"): _*)
      ),
      filter.maxSalary.map(salary => fr"salaryHi > $salary"),
      filter.remote.some.filter(identity).map(remote => fr"remote = $remote"),
      fr"active = true".some
    )

    val orderFragment: Fragment = fr"ORDER BY date DESC"
    val paginationFragment: Fragment =
      fr"LIMIT ${pagination.limit} OFFSET ${pagination.offset}"

    val statement = selectFragment |+| fromFragment |+| whereFragment |+| orderFragment |+| paginationFragment
    /*
      WHERE company in [filter.companies]
        AND location in [filter.locations]
        AND country in [filter.countries]
        AND seniority in [filter.seniorities]
        AND (
          tag1=any(tags)
            OR tag2=any(tags)
            OR ... (for every tag in filter.tags)
        )
        AND salaryHi > [filter.salary]
        AND remote = [filter.remote]
     */

    statement
      .query[Job]
      .to[List]
      .transact(xa)
      .logError(e => s"Failed query: ${e.getMessage}")
  }

  def find(id: UUID): F[Option[Job]] =
    sql"""
      SELECT
        id,
        date,
        ownerEmail,
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
        other,
        active
      FROM jobs
      WHERE id = $id
      AND active = true
    """
      .query[Job]
      .option
      .transact(xa)
  def update(id: UUID, jobInfo: JobInfo): F[Option[Job]] =
    sql"""
      UPDATE jobs
      SET
        company = ${jobInfo.company},
        title = ${jobInfo.title},
        description = ${jobInfo.description},
        externalUrl = ${jobInfo.externalUrl},
        remote = ${jobInfo.remote},
        location = ${jobInfo.location},
        salaryLo = ${jobInfo.salaryLo},
        salaryHi = ${jobInfo.salaryHi},
        currency = ${jobInfo.currency},
        country = ${jobInfo.country},
        tags = ${jobInfo.tags},
        image = ${jobInfo.image},
        seniority = ${jobInfo.seniority},
        other = ${jobInfo.other}
      WHERE id = $id
    """.update.run
      .transact(xa)
      .flatMap(_ => find(id)) // returned updated job

  override def activate(id: UUID): F[Int] =
    sql"UPDATE jobs SET active=true WHERE id=$id".update.run.transact(xa)

  def delete(id: UUID): F[Int] =
    sql"""
      DELETE FROM jobs
      WHERE id = $id
    """.update.run
      .transact(xa)

  // select all unique values from companies, locations, countries, seniorities, tags
  override def possibleFilters(): F[JobFilter] =
    /*
     * Run the following to print out more information
     * val yolo = xa.yolo
     * import yolo.*
     * query.check() *> query.option
     */
    sql"""
    SELECT
      ARRAY(SELECT DISTINCT(company) FROM jobs WHERE active = true) AS companies,
      ARRAY(SELECT DISTINCT(location) FROM jobs WHERE active = true) AS location,
      ARRAY(SELECT DISTINCT(country) FROM jobs WHERE country IS NOT NULL AND active = true) AS countries,
      ARRAY(SELECT DISTINCT(seniority) FROM jobs WHERE seniority IS NOT NULL AND active = true) AS seniorities,
      ARRAY(SELECT DISTINCT(UNNEST(tags)) FROM jobs WHERE active = true) AS tags,
      MAX(salaryHi),
      false FROM jobs
    """
      .query[JobFilter]
      .option
      .transact(xa)
      .map(_.getOrElse(JobFilter()))
}

object LiveJobs {
  given jobRead: Read[Job] = Read[
    (
        UUID,                 // id
        Long,                 // date
        String,               // ownerEmail
        String,               // company
        String,               // title
        String,               // description
        String,               // externalUrl
        Boolean,              // remote
        String,               // location
        Option[Int],          // salaryLo
        Option[Int],          // salaryHi
        Option[String],       // currency
        Option[String],       // country
        Option[List[String]], // tags
        Option[String],       // image
        Option[String],       // seniority
        Option[String],       // other
        Boolean               // active
    )
  ].map {
    case (
          id: UUID,
          date: Long,
          ownerEmail: String,
          company: String,
          title: String,
          description: String,
          externalUrl: String,
          remote: Boolean,
          location: String,
          salaryLo: Option[Int] @unchecked,
          salaryHi: Option[Int] @unchecked,
          currency: Option[String] @unchecked,
          country: Option[String] @unchecked,
          tags: Option[List[String]] @unchecked,
          image: Option[String] @unchecked,
          seniority: Option[String] @unchecked,
          other: Option[String] @unchecked,
          active: Boolean
        ) =>
      Job(
        id = id,
        date = date,
        ownerEmail = ownerEmail,
        JobInfo(
          company = company,
          title = title,
          description = description,
          externalUrl = externalUrl,
          remote = remote,
          location = location,
          salaryLo = salaryLo,
          salaryHi = salaryHi,
          currency = currency,
          country = country,
          tags = tags,
          image = image,
          seniority = seniority,
          other = other
        ),
        active = active
      )
  }

  def apply[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): F[LiveJobs[F]] =
    new LiveJobs[F](xa).pure[F]
}
