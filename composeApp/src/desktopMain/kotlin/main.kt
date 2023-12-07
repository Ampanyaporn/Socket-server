package org.example.project

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun main() {
    runBlocking {
        startServer(4001)
    }
}

fun startServer(port: Int) = runBlocking {
    val serverSocket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(port = port)
    println("Server is running on port $port")

    while (true) {
        val socket = serverSocket.accept()
        println("Client connected: ${socket.remoteAddress}")

        launch(Dispatchers.IO) {
            handleClient(socket)
        }
    }
}

suspend fun handleClient(socket: Socket) {
    val input = socket.openReadChannel()
    val output = socket.openWriteChannel(autoFlush = true)

    try {
        val message = input.readUTF8Line()
        println("Received from client: $message")

        output.writeStringUtf8("Echo: $message\n")
    } finally {
        withContext(Dispatchers.IO) {
            socket.close()
        }
    }
}
