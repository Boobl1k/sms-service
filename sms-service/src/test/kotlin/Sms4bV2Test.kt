/*
 * Copyright 2026 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

import com.fasterxml.jackson.core.type.TypeReference
import com.icerockdev.service.sms.SmsException
import com.icerockdev.service.sms.SmsService
import com.icerockdev.service.sms.driver.sms4bV2.Sms4bV2Config
import com.icerockdev.service.sms.driver.sms4bV2.Sms4bV2Driver
import com.icerockdev.service.sms.driver.sms4bV2.request.SendSmsRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.http.hostWithPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Test

class Sms4bV2Test {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val smsUrl = "https://api.sms4b.ru/v1/sms"
    private val smsSender = "SMS4B-Test"
    private val smsToken = "test_token"

    private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
    private val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"

    private val successResponse = "{\"id\":\"20260324111030358735\"}"
    private val successPhone = "+79139999999"
    private val errorResponse = "{\"status\":409,\"detail\":\"\\u041e\\u0448\\u0438\\u0431\\u043a\\u0430 \\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0446\\u0438\\u0438 \\u043f\\u0430\\u0440\\u0430\\u043c\\u0435\\u0442\\u0440\\u0430 \\u0437\\u0430\\u043f\\u0440\\u043e\\u0441\\u0430.\",\"errors\":[{\"detail\":\"\\u0422\\u0430\\u043a\\u0430\\u044f \\u0440\\u0430\\u0441\\u0441\\u044b\\u043b\\u043a\\u0430 \\u0443\\u0436\\u0435 \\u0431\\u044b\\u043b\\u0430 \\u043d\\u0435\\u0434\\u0430\\u0432\\u043d\\u043e \\u043e\\u0442\\u043f\\u0440\\u0430\\u0432\\u043b\\u0435\\u043d\\u0430. \\u041e\\u0442\\u043f\\u0440\\u0430\\u0432\\u043a\\u0430 \\u043e\\u0434\\u0438\\u043d\\u0430\\u043a\\u043e\\u0432\\u044b\\u0445 SMS \\u0432 \\u0442\\u0435\\u0447\\u0435\\u043d\\u0438\\u0435 \\u0441\\u0443\\u0442\\u043e\\u043a \\u0437\\u0430\\u043f\\u0440\\u0435\\u0449\\u0435\\u043d\\u0430.\\n\\u0414\\u043b\\u044f \\u043f\\u043e\\u0432\\u0442\\u043e\\u0440\\u043d\\u043e\\u0439 \\u043e\\u0442\\u043f\\u0440\\u0430\\u0432\\u043a\\u0438 \\u0438\\u0441\\u043f\\u0440\\u0430\\u0432\\u044c\\u0442\\u0435 \\u0442\\u0435\\u043a\\u0441\\u0442 \\u0438\\u043b\\u0438 \\u043d\\u043e\\u043c\\u0435\\u0440\\u0430.\"}"

    private val httpClient: HttpClient = HttpClient(MockEngine) {
        engine {
            addHandler { request ->

                when (request.url.fullUrl) {
                    smsUrl -> {
                        val responseHeaders =
                            headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()))

                        val token = request.headers[HttpHeaders.Authorization]

                        val requestBodyString = when (val body = request.body) {
                            is TextContent -> body.text
                            is ByteArrayContent -> body.bytes().decodeToString()
                            is OutgoingContent.ByteArrayContent -> body.bytes().decodeToString()
                            is OutgoingContent.ReadChannelContent -> body.readFrom().readRemaining().readText()
                            else -> ""
                        }

                        val requestBody =
                            smsConfig.mapper.readValue(requestBodyString, object : TypeReference<SendSmsRequest>() {})

                        val phone = requestBody.messages.single().number
                        val sender = requestBody.sender

                        if (token != smsToken || sender != smsSender) {
                            return@addHandler respond(
                                errorResponse,
                                HttpStatusCode.BadRequest,
                                headers = responseHeaders
                            )
                        }

                        if (phone != successPhone) {
                            return@addHandler respond(
                                errorResponse,
                                HttpStatusCode.BadRequest,
                                headers = responseHeaders
                            )
                        }

                        return@addHandler respond(
                            successResponse,
                            headers = responseHeaders
                        )
                    }

                    else -> error("Unhandled ${request.url.fullUrl}")
                }
            }
        }
    }

    private val smsConfig = Sms4bV2Config(
        client = httpClient,
        url = smsUrl,
        token = smsToken,
        sender = smsSender,
    )

    private val smsService = SmsService(
        scope,
        Sms4bV2Driver(
            smsConfig,
        )
    )

    @Test
    fun testSuccessSend() {
        runBlocking {
            smsService
                .sendAsync(successPhone, "TEST MESSAGE")
                .await()
        }
    }

    @Test(expected = SmsException::class)
    fun testFailedFormatPhoneNumber() {
        runBlocking {
            smsService
                .sendAsync("79134", "TEST MESSAGE")
                .await()
        }
    }

}
