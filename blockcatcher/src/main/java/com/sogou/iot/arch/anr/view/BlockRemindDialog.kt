package com.sogou.iot.arch.anr.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.example.blockcatcher.R

class BlockRemindDialog: Activity() {

    val DELAY_MILLIS = 50000L
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            doDismiss()
        }
    }

    var loadingTxt:TextView? = null
    var ll_loading:LinearLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_dialog)

        var lp =  getWindow().attributes
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setBackgroundDrawableResource(R.color.transparent);
        getWindow().getDecorView().setPadding(0, 0, 0, 0)
        loadingTxt = findViewById(R.id.loading_txt) as TextView

        ll_loading =  findViewById(R.id.loading)as LinearLayout
        ll_loading?.setOnClickListener{
            doDismiss()
        }

        try2ShowInfo()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        try2ShowInfo()
    }

    fun try2ShowInfo(){
        var mgs = intent?.getStringExtra("msg")
        loadingTxt?.setText(mgs)
        dismissDelay(DELAY_MILLIS)
    }

    fun dismissDelay(delay:Long){
        if (handler != null) {
            handler.removeCallbacksAndMessages(null)
            handler.sendMessageDelayed(handler.obtainMessage(), DELAY_MILLIS.toLong())
        }
    }

    fun doDismiss(){
        finish()
    }
}