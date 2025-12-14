package com.example.bluechat

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.bluechat.ui.theme.BlueChatTheme
import android.Manifest
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.content.Context
import android.graphics.Color
import androidx.core.view.WindowCompat
import android.graphics.drawable.AnimationDrawable




class MainActivity : AppCompatActivity() {
    private val btPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val host = findViewById<View>(R.id.btnHost)
        val join = findViewById<View>(R.id.btnJoin)

        animateButton(this, host)
        animateButton(this,join)

        host.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        join.setOnClickListener {
            startActivity(Intent(this, DeviceListActivity::class.java))
        }


        window.statusBarColor = Color.parseColor("#0B1C26")
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val root = findViewById<View>(R.id.rootLayout)
        val animationDrawable = root.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(2000)
        animationDrawable.setExitFadeDuration(2000)
        animationDrawable.start()



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            btPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        }

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