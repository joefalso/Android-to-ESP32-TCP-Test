package com.example.test_001

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
//import androidx.compose.ui.semantics.text
import androidx.lifecycle.lifecycleScope
import com.example.test_001.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Assuming your TextView ID in XML is 'this_text'
        binding.sampleText.text = "Press a button"

        // Set up the click listener for the first button
        binding.firstButton.setOnClickListener {
            sendMessage("on\n")
            Toast.makeText(this, "Attempting to send 'on'...\n", Toast.LENGTH_SHORT).show()
        }

        // Set up the click listener for the second button
        binding.secondButton.setOnClickListener {
            sendMessage("off\n")
            Toast.makeText(this, "Attempting to send 'off'...\n", Toast.LENGTH_SHORT).show()
        }

        // Set up the click listener for the third button to request the status
        binding.thirdButton.setOnClickListener {
            Toast.makeText(this, "Requesting status...\n", Toast.LENGTH_SHORT).show()
            requestStatus() // Use the new function to get the status
        }
    }

    /**
     * Connects, sends a "status" request, and reads ALL lines from the server's response.
     * This function relies on the SERVER CLOSING the connection after sending its response.
     */
    private fun requestStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val serverIp = "192.168.1.135"
                val serverPort = 8080

                // The Socket is created inside a use block to ensure it's auto-closed
                Socket(serverIp, serverPort).use { socket ->
                    // 1. Send the status request message
                    socket.getOutputStream().write("status\n".toByteArray())
                    socket.getOutputStream().flush() // Ensure data is sent immediately

                    // 2. Read the response from the server using a for loop
                    val reader = socket.getInputStream().bufferedReader()
                    val responseBuilder = StringBuilder()

                    // The for loop will read each line until the stream is closed by the server
                    for (line in reader.readLines()) {
                        responseBuilder.append(line).append("\n")
                    }

                    val serverResponse = responseBuilder.toString().trim() // Use trim() to remove trailing newline

                    // 3. Update the UI on the Main thread with the response
                    withContext(Dispatchers.Main) {
                        if (serverResponse.isNotEmpty()) {
                            binding.sampleText.text = serverResponse
                            Toast.makeText(this@MainActivity, "Status updated!", Toast.LENGTH_SHORT).show()
                        } else {
                            // This will now be reached if the server closes the connection without sending data
                            Toast.makeText(this@MainActivity, "Received empty response from server.", Toast.LENGTH_LONG).show()
                        }
                    }
                } // The socket and its streams are automatically closed here

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Status Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Connects to a TCP server and sends a one-way message, then immediately closes.
     */
    private fun sendMessage(message: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val serverIp = "192.168.1.135"
                val serverPort = 8080

                // **IMPROVEMENT**: Use 'use' block for safety
                Socket(serverIp, serverPort).use { socket ->
                    socket.getOutputStream().write(message.toByteArray())
                    socket.getOutputStream().flush()
                } // Socket is automatically closed here

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Message '$message' sent!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Send Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Native method and companion object remain the same...
    external fun stringFromJNI(): String
    companion object {
        init {
            System.loadLibrary("test_001")
        }
    }
}


