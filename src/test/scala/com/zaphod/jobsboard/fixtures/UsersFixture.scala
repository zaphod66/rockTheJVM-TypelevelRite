package com.zaphod.jobsboard.fixtures

import com.zaphod.jobsboard.domain.user.*

trait UsersFixture {
  val Norbert: User = User(
    "norbert@home.com",
    "$2a$10$crTNsbU2c/JK3vYgF3ADTedEjHwHur03COlnZd.MJql6Tj7o5IJZK", // "password1"
    Some("Norbert"),
    Some("Scheller"),
    Some("Home.com"),
    Role.ADMIN
  )

  val Jana: User = User(
    "jana@home.com",
    "$2a$10$8b3QoQpH045L57Y1jSKVf.FNvPkeScE6nN7a.SMbVnrt5TXPftq2G", // "password2"
    Some("Jana"),
    Some("Otte"),
    Some("Home.com"),
    Role.RECRUITER
  )

  val NewJana: User = User(
    "jana@home.com",
    "$2a$10$PhhLh39hO8672Bf9v7I7De3YyLsc.NEQzVi.x25NbL8Oj5K2vNr/y", // "password3"
    Some("NewJana"),
    Some("Otte"),
    Some("Home.com"),
    Role.RECRUITER
  )

  val NewUser: User = User(
    "newEmail@home.com",
    "$2a$10$QUgh6Tw41hIo4bofKggMbew1rR6vZyiWEAAQEqsmsLBf.V5BNW.3C", // "password4"
    Some("John"),
    Some("Doe"),
    Some("Company"),
    Role.RECRUITER
  )
}
