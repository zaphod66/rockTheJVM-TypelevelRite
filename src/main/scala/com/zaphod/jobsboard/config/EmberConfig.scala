package com.zaphod.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import com.comcast.ip4s.{Host, Port}
import pureconfig.error.CannotConvert

final case class EmberConfig (host: Host, port: Port) derives ConfigReader

object EmberConfig {
  given hostReader: ConfigReader[Host] = ConfigReader[String].emap { hostString =>
    Host.fromString(hostString) match {
      case None =>
        Left(CannotConvert(hostString, Host.getClass.toString, s"Invalid host: $hostString"))
      case Some(host) =>
        Right(host)

    }
  }
  given portReader: ConfigReader[Port] = ConfigReader[Int].emap { portInt =>
    Port
      .fromInt(portInt)
      .toRight(CannotConvert(portInt.toString, Port.getClass.toString, s"Invalid port: $portInt"))
  }
}
