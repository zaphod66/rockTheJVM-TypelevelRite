package com.zaphod.jobsboard.domain

import com.zaphod.jobsboard.domain.job.Job
import com.zaphod.jobsboard.domain.user.Role.RECRUITER
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
    def isAdmin: Boolean = role == Role.ADMIN
    def isRecruiter: Boolean = role == Role.RECRUITER
    def owns(job: Job): Boolean = email == job.ownerEmail
  }

  final case class NewUserInfo(
      email: String,
      password: String,
      firstName: Option[String],
      lastName: Option[String],
      company: Option[String]
  )

  enum Role {
    case ADMIN, RECRUITER
  }

  object Role {
    given metaRole: Meta[Role] =
      Meta[String].timap[Role](Role.valueOf)(_.toString)
  }

  given roleAuthEnum: SimpleAuthEnum[Role, String] with {
    override val values: AuthGroup[Role] = AuthGroup(Role.ADMIN, RECRUITER)
    override def getRepr(role: Role): String = role.toString
  }
}
