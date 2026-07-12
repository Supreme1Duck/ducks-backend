package com.ducks.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

class MinuteChangeNotifierService {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val timeStampState = MutableStateFlow<Long>(0)

    init {
        coroutineScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val secondsUntilNextMinute = 60 - now.second
                val threeSecsDelay = 3000L
                val millis = secondsUntilNextMinute * 1000L - threeSecsDelay

                delay(millis)

                val correctedNow = LocalDateTime.now()
                timeStampState.value = correctedNow.toInstant(ZoneOffset.ofHours(3)).toEpochMilli()

                delay(threeSecsDelay)
            }
        }
    }

    fun observe(): Flow<Long> {
        return timeStampState
    }
}