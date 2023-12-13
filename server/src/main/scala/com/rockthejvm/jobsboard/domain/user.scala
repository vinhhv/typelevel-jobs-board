package com.rockthejvm.jobsboard.domain

import com.rockthejvm.jobsboard.domain.job.*
import doobie.Meta
import tsec.authorization.{AuthGroup, SimpleAuthEnum}

object user {
  final case class User(
      email: String,
      hashedPassword: String,
      firstName: Option[String],
      lastName: Option[String],
      company: Option[String],
      role: Role
  ) {
    def owns(job: Job): Boolean = email == job.ownerEmail
    def isAdmin: Boolean        = role == Role.ADMIN
    def isRecruiter: Boolean    = role == Role.RECRUITER
  }

  enum Role {
    case ADMIN, RECRUITER
  }

  object Role {
    given metaRole: Meta[Role] =
      Meta[String].timap[Role](Role.valueOf)(_.toString)

    given RoleAuthEnum: SimpleAuthEnum[Role, String] with {
      override val values: AuthGroup[Role]     = AuthGroup(Role.ADMIN, Role.RECRUITER)
      override def getRepr(role: Role): String = role.toString
    }
  }
}
