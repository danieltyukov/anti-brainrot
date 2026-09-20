package app.revanced.android.youtube

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button

class HomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)
        findViewById<Button>(R.id.open_shorts).setOnClickListener { startActivity(Intent(this, ShortsActivity::class.java)) }
        findViewById<Button>(R.id.open_watch).setOnClickListener { startActivity(Intent(this, WatchActivity::class.java)) }
    }
}
