package com.example.bluechat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.example.bluechat.ui.theme.BlueChatTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : AppCompatActivity() {
    
    private lateinit var prefs: SharedPreferences
    private lateinit var statusText: TextView

    private val btPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("connection_data", result.contents)
            saveKnownDevice(result.contents)
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        prefs = getSharedPreferences("BlueChatPrefs", Context.MODE_PRIVATE)

        val host = findViewById<View>(R.id.btnHost)
        val join = findViewById<View>(R.id.btnJoin)
        val history = findViewById<View>(R.id.btnHistory)
        val txtTitle = findViewById<TextView>(R.id.txtAppTitle)

        animateButton(this, host)
        animateButton(this,join)
        
        // Show current username or default
        val currentName = prefs.getString("username", "User")
        txtTitle.text = "Hello, $currentName"
        
        txtTitle.setOnClickListener {
             showUpdateNameDialog()
        }

        host.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        join.setOnClickListener {
            showJoinOptionsDialog()
        }
        
        history.setOnClickListener {
             // In a real app this opens a list of known devices
             showKnownDevicesDialog()
        }


        window.statusBarColor = Color.parseColor("#0B1C26")
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val root = findViewById<View>(R.id.rootLayout)
        val animationDrawable = root.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(2000)
        animationDrawable.setExitFadeDuration(2000)
        animationDrawable.start()

        val perms = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
            perms.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
             perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
             perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
             perms.add(Manifest.permission.READ_MEDIA_IMAGES)
        }
        
        btPermissionLauncher.launch(perms.toTypedArray())

    }
    
    private fun showJoinOptionsDialog() {
        val devices = getKnownDevices()
        if (devices.isEmpty()) {
            launchScanner()
            return
        }
        
        val options = arrayOf("Scan New Device") + devices.map { it.name }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Connect to...")
            .setItems(options) { _, which ->
                if (which == 0) {
                    launchScanner()
                } else {
                    val device = devices[which - 1]
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("connection_data", device.data)
                    startActivity(intent)
                }
            }
            .show()
    }
    
    private fun launchScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan Host QR Code")
        options.setCameraId(0)
        options.setBeepEnabled(false)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(false)
        scanLauncher.launch(options)
    }
    
    private fun showUpdateNameDialog() {
        val input = EditText(this)
        input.hint = "Enter username"
        
        AlertDialog.Builder(this)
            .setTitle("Update Profile")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString()
                if (name.isNotEmpty()) {
                    prefs.edit().putString("username", name).apply()
                    findViewById<TextView>(R.id.txtAppTitle).text = "Hello, $name"
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showKnownDevicesDialog() {
        val devices = getKnownDevices()
        val names = devices.map { it.name }.toTypedArray()
        
        if (names.isEmpty()) {
            Toast.makeText(this, "No saved devices yet", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Saved Devices")
            .setItems(names) { _, which ->
                 // Reconnect logic
                 val device = devices[which]
                 val intent = Intent(this, ChatActivity::class.java)
                 intent.putExtra("connection_data", device.data)
                 startActivity(intent)
            }
            .show()
    }
    
    data class SavedDevice(val name: String, val data: String)
    
    private fun saveKnownDevice(data: String) {
        // Simple storage in SharedPreferences: "device_count", "device_0_name", "device_0_data", etc.
        // A real DB table would be better but this works for simple lists.
        
        // Parse name from data if possible "Name|UUID"
        var name = "Unknown Device"
        if (data.contains("|")) {
            name = data.split("|")[0]
        }
        
        // Check duplicates
        val existing = getKnownDevices()
        if (existing.any { it.data == data }) return
        
        val idx = existing.size
        prefs.edit()
            .putString("device_${idx}_name", name)
            .putString("device_${idx}_data", data)
            .putInt("device_count", idx + 1)
            .apply()
    }
    
    private fun getKnownDevices(): List<SavedDevice> {
        val count = prefs.getInt("device_count", 0)
        val list = mutableListOf<SavedDevice>()
        for (i in 0 until count) {
            val name = prefs.getString("device_${i}_name", "Device $i") ?: ""
            val data = prefs.getString("device_${i}_data", "") ?: ""
            if (data.isNotEmpty()) {
                list.add(SavedDevice(name, data))
            }
        }
        return list
    }
}

private fun animateButton(context: Context, view: View) {
    val press = AnimationUtils.loadAnimation(context, R.anim.btn_press)
    val release = AnimationUtils.loadAnimation(context, R.anim.btn_release)

    view.setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> v.startAnimation(press)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> v.startAnimation(release)
        }
        false
    }
}





@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BlueChatTheme {
        Greeting("Android")
    }
}