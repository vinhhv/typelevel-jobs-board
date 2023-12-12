package com.rockthejvm.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

case class EmailServiceConfig(
    host: String,
    port: Int,
    user: String,
    pass: String,
    frontendUrl: String
) derives ConfigReader
