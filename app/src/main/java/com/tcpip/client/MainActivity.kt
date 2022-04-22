package com.tcpip.client

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Button
import android.widget.TextView
import java.io.IOException
import java.net.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    //declared variables
    private var clientThread: ClientThread? = null
    private var thread: Thread? = null
    private var tv: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val address = getWifiApIpAddress()
        Log.d(TAG, "onCreate: >>>> $address")
        

        //Create a thread so that the received data does not run within the main user interface
        clientThread = ClientThread()
        thread = Thread(clientThread)
        thread!!.start()

        // create a value that is linked to a button called (id) MyButton in the layout
        // val buttonPress = findViewById<Button>(R.id.MyButton)
        tv = findViewById(R.id.textView)
        //  tv!!.text = "Data Captured"

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            sendUDP("Hello")
        }

    }

    //************************************ Some test code to send a UDP package
    fun sendUDP(messageStr: String) {
        // Hack Prevent crash (sending should be done using a separate thread)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)  //Just for testing relax the rules...
        try {
            //Open a port to send a UDP package
            val socket = DatagramSocket()
            socket.broadcast = true
            val sendData = messageStr.toByteArray()
            val sendPacket = DatagramPacket(sendData, sendData.size, InetAddress.getByName(SERVER_IP), SERVERPORT)
            socket.send(sendPacket)
            println("Packet Sent")
        } catch (e: IOException) {
            println(">>>>>>>>>>>>> IOException  "+e.message)
        }
    }

    //************************************* Some test code for receiving a UDP package
    internal inner class ClientThread : Runnable {
        private var socket: DatagramSocket? = null
        private val recvBuf = ByteArray(1500)
        private val packet = DatagramPacket(recvBuf, recvBuf.size)
        // **********************************************************************************************
        // * Open the network socket connection and start receiving a Byte Array                        *
        // **********************************************************************************************
        override fun run() {

            try {
                //Keep a socket open to listen to all the UDP trafic that is destined for this port
                socket = DatagramSocket(CLIENTPORT)
                while (true) {
                    //Receive a packet
                    socket!!.receive(packet)

                    //Packet received
                    println("Packet received from: " + packet.address.hostAddress)
                    val data = String(packet.data).trim { it <= ' ' }
                    println("Packet received; data: $data")
                    //Change the text on the main activity view
                    runOnUiThread { tv?.text = data }
                }
            }
            catch (e1: IOException) {
                println(">>>>>>>>>>>>> IOException  "+e1.message)
                socket?.close()
            }
            catch (e2: UnknownHostException) {
                println(">>>>>>>>>>>>> UnknownHostException  "+e2.message)
                socket?.close()
            }
            finally{
                socket?.close()
            }
        }
    }

    companion object {
        val CLIENTPORT = 50001
        val SERVERPORT = 50001
        val SERVER_IP = "192.168.58.112"
    }


    fun getWifiApIpAddress(): String? {
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en
                    .hasMoreElements()
            ) {
                val intf: NetworkInterface = en.nextElement()
                if (intf.name.contains("wlan")) {
                    val enumIpAddr: Enumeration<InetAddress> = intf.inetAddresses
                    while (enumIpAddr
                            .hasMoreElements()
                    ) {
                        val inetAddress: InetAddress = enumIpAddr.nextElement()

                        Log.d(TAG, "getWifiApIpAddress: >>> $inetAddress ")
                        if (!inetAddress.isLoopbackAddress
                            && inetAddress.address.size == 4
                        ) {
                            Log.d("size", inetAddress.hostAddress)
                            return inetAddress.hostAddress
                        }
                    }
                }
            }
        } catch (ex: SocketException) {
            Log.e("SocketException", ex.toString())
        }
        return null
    }

}