package com.v39a.omni.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun nowUTC() = Clock.System.now().toLocalDateTime(TimeZone.UTC)