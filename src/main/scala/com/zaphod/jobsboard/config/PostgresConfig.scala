package com.zaphod.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import pureconfig.error.CannotConvert

final case class PostgresConfig(nThreads: Int, url: String, user: String, pass: String) derives ConfigReader

