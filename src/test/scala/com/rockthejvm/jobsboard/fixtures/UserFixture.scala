package com.rockthejvm.jobsboard.fixtures

import com.rockthejvm.jobsboard.domain.user.*

trait UserFixture {
  val rockthejvmPw       = "rockthejvm"
  val rockthejvmPwHashed = "$2a$10$q1knuC.xPbjT6w/XuBHei.S3WJxS3zF3FE89Ha2xVbb1YY.kkkKb."
  val Vinh = User(
    "vinh@rockthejvm.com",
    rockthejvmPwHashed,
    Some("Vinh"),
    Some("Vu"),
    Some("Rock the JVM"),
    Role.ADMIN
  )
  val vinhEmail    = Vinh.email
  val vinhPassword = rockthejvmPw
  
  val NewUserVinh = NewUserInfo(
    vinhEmail,
    vinhPassword,
    Some("Vinh"),
    Some("Vu"),
    Some("RockTheJvm")
  )

  val Joe = User(
    "joe@rockthejvm.com",
    rockthejvmPwHashed,
    Some("Joe"),
    Some("Schmoe"),
    Some("Rock the JVM"),
    Role.RECRUITER
  )
  val joeEmail    = Joe.email
  val joePassword = rockthejvmPw
  
  val NewUserJoe = NewUserInfo(
    joeEmail,
    joePassword,
    Some("Joe"),
    Some("Schmoe"),
    Some("Rock the JVM")
  )

  val updatedJoePassword       = "joerocks"
  val updatedJoePasswordHashed = "$2a$10$4EDdSiB8zDqX8OhcFJSHV.VpiGw8TARc.24esEJmdbRJKYOJIwGPS"
  val UpdatedJoe = User(
    "joe@rockthejvm.com",
    updatedJoePasswordHashed,
    Some("JOE"),
    Some("SCHMOE"),
    Some("Apple"),
    Role.RECRUITER
  )

  val simplePassword       = "simplepassword"
  val simplePasswordHashed = "$2a$10$I0nd2Wddnfam40yvsRDdTeh8.F7BA9mb.QXpIQEbx7hdSKbuhPs06"
  val NewUser = User(
    "newuser@rockthejvm.com",
    simplePasswordHashed,
    Some("John"),
    Some("Doe"),
    Some("Some company"),
    Role.RECRUITER
  )
  val newUserPassword = simplePassword
}
