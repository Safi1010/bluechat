package com.example.bluechat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ChatActivity : AppCompatActivity() {

    private var service: BluetoothService? = null
    private val adapter = ChatAdapter { msg ->
        // Long click handler for deleting messages
        showDeleteMessageDialog(msg)
    }
    private lateinit var database: ChatDatabase
    private lateinit var prefs: SharedPreferences
    
    private lateinit var chatLayout: RecyclerView
    private lateinit var inputLayout: LinearLayout
    private lateinit var qrLayout: LinearLayout
    private lateinit var loadingContainer: LinearLayout
    private lateinit var loadingText: TextView
    private lateinit var statusText: TextView
    private lateinit var topBar: LinearLayout
    private lateinit var bottomBar: LinearLayout
    private lateinit var contentFrame: FrameLayout
    private lateinit var statusIndicator: TextView
    
    private var connectionUUID: UUID = Constants.CHAT_UUID
    private var discoveryReceiver: BroadcastReceiver? = null
    
    // File handling
    private var pendingFileBytes: ByteArray? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            offerFile(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)

        database = ChatDatabase.getDatabase(this)
        prefs = getSharedPreferences("BlueChatPrefs", Context.MODE_PRIVATE)

        chatLayout = findViewById(R.id.chatRecyclerView)
        inputLayout = findViewById(R.id.inputContainer)
        qrLayout = findViewById(R.id.qrContainer)
        loadingContainer = findViewById(R.id.loadingContainer)
        loadingText = findViewById(R.id.loadingText)
        statusText = findViewById(R.id.statusText)
        topBar = findViewById(R.id.topBar)
        bottomBar = findViewById(R.id.bottomBar)
        contentFrame = findViewById(R.id.contentFrame)
        statusIndicator = findViewById(R.id.statusIndicator)
        
        val input = findViewById<EditText>(R.id.messageInput)
        val btn = findViewById<View>(R.id.sendButton)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        
        val btnShare = findViewById<ImageView>(R.id.btnShareFile)
        val btnCamera = findViewById<ImageView>(R.id.btnCamera)
        
        btnBack.setOnClickListener { finish() }
        
        btnShare.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }
        
        btnCamera.setOnClickListener {
             Toast.makeText(this, "Camera Sharing coming soon!", Toast.LENGTH_SHORT).show()
        }

        chatLayout.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        chatLayout.adapter = adapter
        
        database.chatDao().getAllMessages().observe(this) { messages ->
            adapter.setMessages(messages)
            if (messages.isNotEmpty()) {
                chatLayout.smoothScrollToPosition(messages.size - 1)
            }
        }

        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val deviceExtra = intent.getParcelableExtra<BluetoothDevice>("device")
        val deviceAddress = intent.getStringExtra("device_address")
        val qrContent = intent.getStringExtra("connection_data")

        if (deviceExtra == null && deviceAddress == null && qrContent == null) {
            setupHostMode(btAdapter)
        } else {
            setupClientMode(btAdapter, deviceExtra, deviceAddress ?: qrContent)
        }

        btn.setOnClickListener {
            val msg = input.text.toString()
            if (msg.isNotBlank()) {
                if (service == null) {
                    Toast.makeText(this, "Not connected yet", Toast.LENGTH_SHORT).show()
                } else {
                    service?.send(msg)
                    saveMessage(msg, true)
                    input.text.clear()
                }
            }
        }
        
        val root = findViewById<View>(android.R.id.content)
        root.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())
            
            topBar.setPadding(
                topBar.paddingLeft, 
                systemBars.top, 
                topBar.paddingRight, 
                topBar.paddingBottom
            )
            
            if (ime.bottom > 0) {
                 bottomBar.setPadding(
                    bottomBar.paddingLeft,
                    bottomBar.paddingTop,
                    bottomBar.paddingRight,
                    12.dpToPx() 
                )
            } else {
                 bottomBar.setPadding(
                    bottomBar.paddingLeft,
                    bottomBar.paddingTop,
                    bottomBar.paddingRight,
                    systemBars.bottom + 12.dpToPx()
                )
            }

            insets
        }
    }
    
    private fun showDeleteMessageDialog(msg: ChatMessage) {
        AlertDialog.Builder(this)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                deleteMessage(msg)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteMessage(msg: ChatMessage) {
        lifecycleScope.launch(Dispatchers.IO) {
            database.chatDao().delete(msg)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ChatActivity, "Message deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun saveMessage(text: String, isMine: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            val user = if(isMine) "Me" else "Friend"
            database.chatDao().insert(ChatMessage(text = text, isMine = isMine, senderName = user))
        }
    }
    
    private fun saveKnownDevice(name: String, uuid: String) {
        // Construct the "data" string as it would appear in QR code
        val data = "$name${Constants.DATA_DELIMITER}$uuid"
        
        val count = prefs.getInt("device_count", 0)
        
        // Check duplicates
        for (i in 0 until count) {
            val existingData = prefs.getString("device_${i}_data", "")
            if (existingData == data) return // Already saved
        }
        
        prefs.edit()
            .putString("device_${count}_name", name)
            .putString("device_${count}_data", data)
            .putInt("device_count", count + 1)
            .apply()
            
        // Note: For a robust app, use Room DB for this too.
    }

    private fun offerFile(uri: Uri) {
        val fileName = getFileName(uri)
        
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileBytes = inputStream?.readBytes()
            inputStream?.close()
            
            if (fileBytes != null && service != null) {
                pendingFileBytes = fileBytes
                saveMessage("Offering file: $fileName", true)
                service?.offerFile(fileName, fileBytes.size)
                Toast.makeText(this, "Offering: $fileName", Toast.LENGTH_SHORT).show()
            } else {
                 Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error preparing file", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showAcceptFileDialog(fileName: String, size: Int) {
        val sizeKb = size / 1024
        AlertDialog.Builder(this)
            .setTitle("Incoming File")
            .setMessage("User wants to send a file:\n\n$fileName\nSize: $sizeKb KB")
            .setPositiveButton("Accept") { _, _ ->
                service?.acceptFile(true)
                saveMessage("Accepted file transfer: $fileName", true)
            }
            .setNegativeButton("Reject") { _, _ ->
                service?.acceptFile(false)
                saveMessage("Rejected file transfer: $fileName", true)
            }
            .setCancelable(false)
            .show()
    }

    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "unknown_file"
    }
    
    private fun saveFileToStorage(fileName: String, data: ByteArray) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        // Simple MIME type guessing or default to binary
                        put(MediaStore.MediaColumns.MIME_TYPE, "*/*") 
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BlueChat")
                    }
                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        val outputStream = contentResolver.openOutputStream(it)
                        outputStream?.write(data)
                        outputStream?.close()
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ChatActivity, "File saved to Downloads/BlueChat", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val blueChatDir = File(downloadsDir, "BlueChat")
                    if (!blueChatDir.exists()) blueChatDir.mkdirs()
                    
                    val file = File(blueChatDir, fileName)
                    val fos = FileOutputStream(file)
                    fos.write(data)
                    fos.close()
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ChatActivity, "File saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Error saving file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    @SuppressLint("MissingPermission")
    private fun setupHostMode(btAdapter: BluetoothAdapter) {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        startActivity(discoverableIntent)
        
        // Host Mode always generates NEW session key for security
        // But for "reconnecting" we might want to advertise a known UUID if possible.
        // For this architecture (QR Code based Key Exchange), the Host dictates the UUID.
        // A known client connects to THIS specific UUID.
        // So if we want to support reconnecting, the Host must reuse the UUID or the Client must scan again.
        // However, standard Bluetooth chat usually has a fixed Service UUID.
        // We used Random UUID to solve the MAC Address privacy issue.
        
        // Fix: Use a Consistent UUID based on device name or saved pref if available?
        // OR: Use the standard Fixed UUID but rely on Name Discovery (which we implemented).
        
        // Let's try to revert to fixed UUID for "Known Device" logic to work smoothly without QR scan?
        // NO, the random UUID was key to the privacy fix.
        
        // COMPROMISE: We will generate a UUID once and save it in Prefs as "MyHostUUID".
        // This way, my Host UUID is constant across app restarts.
        // Clients who saved me can reconnect to this UUID.
        
        var myHostUuidString = prefs.getString("my_host_uuid", null)
        if (myHostUuidString == null) {
            myHostUuidString = UUID.randomUUID().toString()
            prefs.edit().putString("my_host_uuid", myHostUuidString).apply()
        }
        
        connectionUUID = UUID.fromString(myHostUuidString)
        
        qrLayout.visibility = View.VISIBLE
        loadingContainer.visibility = View.GONE
        chatLayout.visibility = View.GONE
        bottomBar.visibility = View.GONE

        val address = btAdapter.address ?: "Unavailable"
        val deviceName = btAdapter.name ?: "BlueChatHost"
        // Ensure format is: Name|UUID
        val qrContent = "$deviceName${Constants.DATA_DELIMITER}$connectionUUID"
        
        generateQRCode(qrContent)
        statusText.text = "Scan to connect\n(Persisted Host Key)"


        BluetoothServer(btAdapter, connectionUUID) { socket ->
            startService(socket)
        }.start()
    }

    @SuppressLint("MissingPermission")
    private fun setupClientMode(btAdapter: BluetoothAdapter, deviceExtra: BluetoothDevice?, qrContent: String?) {
        qrLayout.visibility = View.GONE
        bottomBar.visibility = View.GONE
        chatLayout.visibility = View.GONE
        loadingContainer.visibility = View.VISIBLE
        
        var targetMac = ""
        var targetUuid = Constants.CHAT_UUID
        var targetName = ""
        
        if (qrContent != null && qrContent.contains(Constants.DATA_DELIMITER)) {
            val parts = qrContent.split(Constants.DATA_DELIMITER)
            if (BluetoothAdapter.checkBluetoothAddress(parts[0])) {
                targetMac = parts[0]
            } else {
                targetName = parts[0]
            }
            
            try {
                targetUuid = UUID.fromString(parts[1])
                // Save this connection immediately on client side too
                // (Wait for handshake ideally, but we have data now)
                connectionUUID = targetUuid
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
             targetMac = qrContent ?: deviceExtra?.address ?: ""
        }
        
        if (BluetoothAdapter.checkBluetoothAddress(targetMac)) {
             val device = btAdapter.getRemoteDevice(targetMac)
             connectToDevice(device, targetUuid)
        } else {
            if (targetName.isNotEmpty()) {
                loadingText.text = "Searching for '$targetName'..."
                startDiscoveryForName(btAdapter, targetUuid, targetName)
            } else {
                Toast.makeText(this, "Invalid QR Data", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun startDiscoveryForName(btAdapter: BluetoothAdapter, uuid: UUID, targetName: String) {
        // If we are reconnecting, we might not find the device if it's not discoverable.
        // But the Host logic above now enables discoverability every time.
        
        discoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when(intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        val name = device?.name
                        if (name != null && name == targetName) {
                            btAdapter.cancelDiscovery()
                            try {
                                unregisterReceiver(this)
                                discoveryReceiver = null
                            } catch (e: Exception) {}
                            
                            loadingText.text = "Found $name. Connecting..."
                            connectToDevice(device, uuid)
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(discoveryReceiver, filter)
        btAdapter.startDiscovery()
        
        // Fallback: If device is paired/bonded, we might find it in bonded devices without discovery
        val bonded = btAdapter.bondedDevices
        val known = bonded.find { it.name == targetName }
        if (known != null) {
            loadingText.text = "Found known device. Connecting..."
            btAdapter.cancelDiscovery() // Stop expensive discovery
            connectToDevice(known, uuid)
        }
    }
    
    private fun connectToDevice(device: BluetoothDevice, uuid: UUID) {
        BluetoothClient(device, uuid) { socket ->
            startService(socket)
        }.start()
    }

    private fun generateQRCode(content: String) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(content, BarcodeFormat.QR_CODE, 600, 600)
            findViewById<ImageView>(R.id.qrCodeImage).setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startService(socket: BluetoothSocket) {
        service = BluetoothService(socket)
        
        runOnUiThread {
            qrLayout.visibility = View.GONE
            loadingContainer.visibility = View.GONE
            chatLayout.visibility = View.VISIBLE
            bottomBar.visibility = View.VISIBLE
            statusIndicator.text = "Connected"
            statusIndicator.setTextColor(getColor(R.color.accent))
            
            // Send Handshake
            val myName = prefs.getString("username", "BlueChat User") ?: "BlueChat User"
            // Send special handshake: KEY:MyName|MyUUID
            // We send OUR listening UUID so they can save us correctly?
            // Actually the one who initiates doesn't listen on a UUID usually.
            // But if we want bidirectional saving, we should send our "Host UUID" too.
            val myHostUuid = prefs.getString("my_host_uuid", Constants.CHAT_UUID.toString())
            
            val handshake = "${Constants.KEY_EXCHANGE_PREFIX}$myName${Constants.DATA_DELIMITER}$myHostUuid"
            service?.send(handshake)
        }

        service?.listen(
            onMessage = { msg ->
                if (msg.startsWith(Constants.KEY_EXCHANGE_PREFIX)) {
                    // Handle Handshake
                    val data = msg.removePrefix(Constants.KEY_EXCHANGE_PREFIX)
                    val parts = data.split(Constants.DATA_DELIMITER)
                    if (parts.size >= 2) {
                        val peerName = parts[0]
                        val peerUuid = parts[1]
                        
                        runOnUiThread {
                            Toast.makeText(this, "Connected to $peerName", Toast.LENGTH_SHORT).show()
                            saveKnownDevice(peerName, peerUuid)
                            statusIndicator.text = "Connected to $peerName"
                        }
                    }
                } else {
                    saveMessage(msg, false)
                }
            },
            onFileOffer = { fileName, fileSize ->
                runOnUiThread {
                    showAcceptFileDialog(fileName, fileSize)
                }
            },
            onFileTransferStatus = { accepted ->
                if (accepted) {
                    // Start sending
                    val bytes = pendingFileBytes
                    if (bytes != null) {
                        saveMessage("Sending file...", true)
                        service?.sendFileData(bytes) { percent ->
                             if (percent == 100) {
                                runOnUiThread {
                                    Toast.makeText(this, "File Sent Successfully!", Toast.LENGTH_SHORT).show()
                                    saveMessage("File Sent", true)
                                }
                            }
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "File rejected by peer", Toast.LENGTH_SHORT).show()
                        saveMessage("File transfer rejected", true)
                        pendingFileBytes = null
                    }
                }
            },
            onFileProgress = { percent ->
                if (percent % 20 == 0) {
                     // runOnUiThread { Toast.makeText(this, "Receiving: $percent%", Toast.LENGTH_SHORT).show() }
                }
            },
            onFileReceived = { fileName, fileData ->
                val sizeKb = fileData.size / 1024
                saveMessage("Received file: $fileName ($sizeKb KB)", false)
                
                // Save to real storage
                saveFileToStorage(fileName, fileData)
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (discoveryReceiver != null) {
            try {
                unregisterReceiver(discoveryReceiver)
            } catch (e: Exception) {}
        }
    }
}
