/*
 * Copyright 2026 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.sms.driver.sms4bV2

import com.icerockdev.service.sms.SmsException
import com.icerockdev.service.sms.driver.ISmsDriver
import com.icerockdev.service.sms.driver.sms4bV2.request.SendSmsRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess


class Sms4bV2Driver(private val config: Sms4bV2Config) : ISmsDriver {
    @Throws(SmsException::class)
    override suspend fun send(to: String, text: String): Boolean {
        val request = SendSmsRequest(
            sender = config.sender,
            messages = listOf(
                SendSmsRequest.SendSmsMessage(
                    number = to,
                    text = text,
                )
            ),
        )

        try {
            val response = config.client.post(config.url) {
                setBody(config.mapper.writeValueAsString(request))
                headers[HttpHeaders.ContentType] = ContentType.Application.Json.toString()
                headers[HttpHeaders.Authorization] = config.token
            }
            handleResponse(response)
            return true
        } catch (e: SmsException) {
            throw e
        } catch (e: Throwable) {
            throw SmsException("Send sms failed", e)
        }
    }

    private fun handleResponse(response: HttpResponse) {
        if (!response.status.isSuccess()) {
            throw SmsException("Send sms failed with HTTP code: ${response.status.value}")
        }
    }
}
