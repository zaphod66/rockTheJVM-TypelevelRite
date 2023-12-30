package com.zaphod.jobsboard.playground

import cats.effect.{IO, IOApp}
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

object PasswordHashing extends IOApp.Simple {

  val passwordPlain = "scalarocks"
  override def run: IO[Unit] =
    BCrypt.hashpw[IO](passwordPlain).flatMap(IO.println) *>
      BCrypt.checkpwBool[IO](
        passwordPlain,
        PasswordHash[BCrypt]("$2a$10$w0x1cct9qzIbmFSo6mn65OWm.hAEuiL5wkiapBRS9Ol8Z6NiwhmMa")
      ).flatMap(IO.println)
}
