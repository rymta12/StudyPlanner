package com.studyplanner.app.core.data.local.converter

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun longToString(value: Long?): String = value?.toString() ?: "0"
    @TypeConverter fun stringToLong(value: String?): Long = value?.toLongOrNull() ?: 0L
}
