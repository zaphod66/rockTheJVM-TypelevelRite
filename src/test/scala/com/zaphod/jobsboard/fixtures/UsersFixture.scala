package com.zaphod.jobsboard.fixtures

import com.zaphod.jobsboard.domain.user.*

trait UsersFixture {
  val Norbert: User = User(
    "norbert@home.com",
    "hashedPassword",
    Some("Norbert"),
    Some("Scheller"),
    Some("Home.com"),
    Role.ADMIN
  )

  val Jana: User = User(
    "jana@home.com",
    "hashedPassword",
    Some("Jana"),
    Some("Otte"),
    Some("Home.com"),
    Role.RECRUITER
  )

  val NewJana: User = User(
    "jana@home.com",
    "hashedPassword",
    Some("NewJana"),
    Some("Otte"),
    Some("Home.com"),
    Role.RECRUITER
  )

  val NewUser: User = User(
    "newEmail@home.com",
    "hashedPassword",
    Some("John"),
    Some("Doe"),
    Some("Company"),
    Role.RECRUITER
  )
}
