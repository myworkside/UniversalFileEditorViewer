package com.sumitupdat.universalfileeditorviewer.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader

class LogcatReader {
    fun readLogs(): Flow<String> = flow {
        try {
            val process = Runtime.getRuntime().exec("logcat -v time")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (true) {
                line = reader.readLine()
                if (line == null) break
                emit(line)
            }
        } catch (e: Exception) {
            emit("Error reading logs: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}
