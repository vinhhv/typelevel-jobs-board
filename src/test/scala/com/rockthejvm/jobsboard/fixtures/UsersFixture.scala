package com.rockthejvm.jobsboard.fixtures

import com.rockthejvm.jobsboard.domain.user.*

trait UsersFixture {
  val Vinh = User(
    "vinh@rockthejvm.com",
    "rockthejvm",
    Some("vinh"),
    Some("vu"),
    Some("Rock the JVM"),
    Role.ADMIN
  )

  val Joe = User(
    "joe@rockthejvm.com",
    "rockthejvm",
    Some("joe"),
    Some("schmoe"),
    Some("Rock the JVM"),
    Role.RECRUITER
  )

  val UpdatedJoe = User(
    "joe@rockthejvm.com",
    "joerocks",
    Some("JOE"),
    Some("SCHMOE"),
    Some("Apple"),
    Role.RECRUITER
  )

  val NewUser = User(
    "newuser@rockthejvm.com",
    "simplepassword",
    Some("John"),
    Some("Doe"),
    Some("Some company"),
    Role.RECRUITER
  )
}
