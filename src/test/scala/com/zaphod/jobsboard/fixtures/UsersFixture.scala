package com.zaphod.jobsboard.fixtures

import com.zaphod.jobsboard.domain.user.*

// password1 => $2a$10$crTNsbU2c/JK3vYgF3ADTedEjHwHur03COlnZd.MJql6Tj7o5IJZK
// password2 => $2a$10$8b3QoQpH045L57Y1jSKVf.FNvPkeScE6nN7a.SMbVnrt5TXPftq2G
// password3 => $2a$10$PhhLh39hO8672Bf9v7I7De3YyLsc.NEQzVi.x25NbL8Oj5K2vNr/y
// password4 => $2a$10$QUgh6Tw41hIo4bofKggMbew1rR6vZyiWEAAQEqsmsLBf.V5BNW.3C

trait UsersFixture {
  val Norbert: User = User(
    "norbert@home.com",
    "$2a$10$crTNsbU2c/JK3vYgF3ADTedEjHwHur03COlnZd.MJql6Tj7o5IJZK", // "password1"
    Some("Norbert"),
    Some("Scheller"),
    Some("Home.com"),
    Role.ADMIN
  )
  val PlainPasswordNorbert = "password1"

  val Jana: User = User(
    "jana@home.com",
    "$2a$10$8b3QoQpH045L57Y1jSKVf.FNvPkeScE6nN7a.SMbVnrt5TXPftq2G", // "password2"
    Some("Jana"),
    Some("Otte"),
    Some("Home.com"),
    Role.RECRUITER
  )
  val PlainPasswordJana = "password2"

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

  val NewUserInfoNorbert: NewUserInfo = NewUserInfo(
    Norbert.email,
    PlainPasswordNorbert,
    None,
    None,
    None
  )

  val NewUserInfoJana: NewUserInfo = NewUserInfo(
    Jana.email,
    PlainPasswordJana,
    None,
    None,
    None
  )
}
