package org.example.project

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ServerUI()
        }
    }
}

@Composable
fun ServerUI() {
    var messages by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var serverStatus by remember { mutableStateOf("Not connected") }
    var receivedMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val serverIp = getCurrentWifiIpAddress(context) ?: "IP not available"

    LaunchedEffect(Unit) {
        setupServer { message ->
            receivedMessage = message
            serverStatus = "Message received: $message"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Server IP: $serverIp\n\n", style = MaterialTheme.typography.h6)
        Text("Server $serverStatus \n\n", style = MaterialTheme.typography.h6)
        OutlinedTextField(
            value = messages,
            onValueChange = { messages = it },
            label = { Text("Enter your message here") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            response = "Function not implemented yet"
        }) {
            Text("Send")
        }
        Text("Response from Client: \n$receivedMessage", style = MaterialTheme.typography.subtitle1)
    }
}

fun getCurrentWifiIpAddress(context: Context): String? {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val wifiNetwork = connectivityManager.allNetworks.firstOrNull { network ->
        connectivityManager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    } ?: return null

    val linkProperties = connectivityManager.getLinkProperties(wifiNetwork) ?: return null
    return linkProperties.linkAddresses.firstOrNull { it.address is Inet4Address }?.address?.hostAddress
}

suspend fun setupServer(onMessageReceived: (String) -> Unit) {
    withContext(Dispatchers.IO) {
        val serverSocket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(port = 4001)
        println("Server is running on port 4001")

        while (true) {
            val socket = serverSocket.accept()
            launch {
                val input = socket.openReadChannel()
                val message = input.readUTF8Line()
                println("Message from client: $message")
                message?.let { onMessageReceived(it) }
                withContext(Dispatchers.IO) {
                    socket.close()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ServerUI()
}
