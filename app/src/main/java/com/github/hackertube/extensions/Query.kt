package com.github.hackertube.extensions

fun query(block: () -> Unit) {
    Thread {
        try {
            block.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}
