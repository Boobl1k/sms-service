/*
 * Copyright 2026 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.sms.driver.sms4bV2.request

data class SendSmsRequest(
    val sender: String,
    val messages: List<SendSmsMessage>,
) {
    data class SendSmsMessage(
        val number: String,
        val text: String,
    )
}
