package com.example.lanslayer;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public abstract class ActivityThread extends Thread{
    Handler handler;
    AppCompatActivity activity;
    public ActivityThread(AppCompatActivity activity) {
        handler = new Handler(Looper.getMainLooper());
        this.activity = activity;
    }

    @Override
    public void run() {

    }
    public void doByMainThread(Runnable r){
        handler.post(r);
    }
    public void showAToast(String message){
        doByMainThread(new Thread(){
            @Override
            public void run() {
                Toast.makeText(activity,message,Toast.LENGTH_LONG).show();
            }
        });

    }


}
