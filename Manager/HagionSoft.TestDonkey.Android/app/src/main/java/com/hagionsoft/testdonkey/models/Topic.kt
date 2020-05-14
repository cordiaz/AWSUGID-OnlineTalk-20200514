package com.hagionsoft.testdonkey.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Topic(
    var id: String = UUID.randomUUID().toString(),
    var name: String,
    var arn: String,
    var cron: String,
    val messages: MutableList<String> = mutableListOf()
) : Parcelable