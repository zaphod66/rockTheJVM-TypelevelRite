package com.zaphod.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class TokenConfig(tokenDuration: Long)
    derives ConfigReader // Long instead of FiniteDuration, because we wanna store it is the database
