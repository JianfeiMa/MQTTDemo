package com.buyuphk.mqttdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

/**
 * description: MQTT = Message Queuing Telemetry Transport 消息队列遥测传输协议
 * Copyright (C), buyuphk物流中转站
 * author: JianfeiMa
 * email: majianfei93@163.com
 * revised: 2019-11-21 12:25
 * motto: 勇于向未知领域探索
 */
public class MainActivity extends AppCompatActivity implements IGetMessageCallBack {
    private ConstraintLayout constraintLayout;
    private TextView textView;
    private Button button;
    private EditText editText;
    private MyServiceConnection serviceConnection;
    private MQTTService mqttService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        constraintLayout = findViewById(R.id.root);
        textView = (TextView) findViewById(R.id.activity_main_text_view);
        button = (Button) findViewById(R.id.activity_main_button);
        editText = editText = findViewById(R.id.activity_main_edit_text_publish_topic_message);

        serviceConnection = new MyServiceConnection();
        serviceConnection.setIGetMessageCallBack(MainActivity.this);

        Intent intent = new Intent(this, MQTTService.class);

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String topicMessage = editText.getText().toString();
                if (!topicMessage.equals("")) {
                    MQTTService.publish(topicMessage);
                } else {
                    Toast.makeText(mqttService, "不能发布空的主题消息", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void setMessage(String message) {
        textView.setText(message);
        //Toast.makeText(mqttService, "收到新的订阅主题消息：" + message, Toast.LENGTH_SHORT).show();
        Snackbar.make(constraintLayout, "收到新的订阅主题消息：" + message, Snackbar.LENGTH_SHORT).show();
        mqttService = serviceConnection.getMqttService();
        mqttService.toCreateNotification(message);
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();
    }
}
