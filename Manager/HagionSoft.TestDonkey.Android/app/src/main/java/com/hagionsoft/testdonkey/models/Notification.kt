package com.hagionsoft.testdonkey.models

import java.util.*

data class Notification(
    var TopicId: String = UUID.randomUUID().toString(),
    var TopicName: String,
    var TopicArn: String = "",
    var Cron: String,
    var Messages: MutableList<String> = mutableListOf())