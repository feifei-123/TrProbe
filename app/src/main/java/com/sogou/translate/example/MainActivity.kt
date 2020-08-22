package com.sogou.translate.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

import android.os.Bundle

import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sogou.tm.commonlib.log.Logu
import com.sogou.translate.breakpad.BreakPadCore


import java.io.File


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var externalReportPath: File? = null

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun checkPermision() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            val checks = ArrayList<String>()
            for (permission in permissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    checks.add(permission)
                }
            }
            if (checks.isNotEmpty()) {
                requestPermissions(checks.toTypedArray(), 1)
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Logu.d("feifei~onCreate()")

        checkPermision()

        Thread {
            while (true) {
                Thread.sleep(1000)
                Logu.d("feifei call timetick")
            }
        }.start()

    }


    override fun onClick(view: View) {

        when (view.id) {

            R.id.btn_go2crash_natvie -> {
                Logu.d("feifei - btn_test_dynamic_bind")
                BreakPadCore.go2crash()
            }
            R.id.btn_go2crash_java -> {
                Logu.d("feifei", "go2CrashJava")
                var j = 10 / 0
            }

            R.id.btn_anr -> try {
                Thread.sleep(30000)

            } catch (e: InterruptedException) {
                e.printStackTrace()
            }


            R.id.btn_log -> {
                var i = 0;
                Thread({
                    while (true) {
                        i++

                        Logu.d("feifei", "输出测试111:${i}")


                        Thread.sleep(1000)
                    }
                }).start()
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

    companion object {

        private val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 100
    }
}
