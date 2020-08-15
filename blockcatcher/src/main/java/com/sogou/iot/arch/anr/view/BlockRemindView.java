package com.sogou.iot.arch.anr.view;

import android.app.Dialog;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import com.example.blockcatcher.R;

public class BlockRemindView extends Dialog {
    public static final int DELAY_MILLIS = 50000;
    private Context context;
    private View views;
    public static Boolean isShown = false;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            dismissDialog();
        }
    };

    public BlockRemindView(final Context context) {
        super(context);
        this.context = context;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        views = inflater.inflate(R.layout.layout_dialog, null);
    }

    public void showDialog(String info) {
        if (isShown) {
            return;
        }

        attachWindown(info);
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler.sendMessageDelayed(handler.obtainMessage(), DELAY_MILLIS);
        }

        isShown = true;
    }

    private void attachWindown(String  info) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        TextView loadingTxt = ((TextView) views.findViewById(R.id.loading_txt));
        loadingTxt.setText(info);
        loadingTxt.setMovementMethod(ScrollingMovementMethod.getInstance());
        getWindow().getWindowManager().addView(views, params);
        views.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dismissDialog();
            }
        });


    }

    public void dismissDialog() {

        Log.d("feifei","dismissDialog:"+isShown);
        if (isShown) {
            getWindow().getWindowManager().removeView(views);
        }
        isShown = false;
        dismiss();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public boolean isShowing() {
        return isShown;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}