/*
 * Copyright 2026 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.sms.driver.sms4bV2

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache

data class Sms4bV2Config(
    val client: HttpClient = HttpClient(Apache),
    val mapper: ObjectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(kotlinModule()),
    val url: String = "https://api.sms4b.ru/v1/sms",
    val token: String,
    val sender: String,
)
