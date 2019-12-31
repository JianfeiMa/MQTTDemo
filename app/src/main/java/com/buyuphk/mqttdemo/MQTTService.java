package com.buyuphk.mqttdemo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTService extends Service {
    public static final String TAG = MQTTService.class.getSimpleName();
    private static MqttAndroidClient mqttAndroidClient;
    private MqttConnectOptions mqttConnectOptions;

    private String host = "tcp://192.168.1.33:1883";
    private String userName = "admin";
    private String password = "password";
    /**
     * 要订阅的主题
     */
    private static String myTopic = "ForTest";
    /**
     * 客服端标识
     */
    private String clientId = "androidId";
    private IGetMessageCallBack iGetMessageCallBack;

    // MQTT监听并且接受消息
    private MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            Log.w(TAG, "失去连接");
            cause.printStackTrace();
            Log.d(TAG, "失去连接，开始重连");
            doClientConnection();
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            String str1 = new String(message.getPayload());
            Log.d(TAG, "接收到订阅的主题为：" + topic + "的消息：" + str1);
            if (iGetMessageCallBack != null) {
                iGetMessageCallBack.setMessage(str1);
            }
            String str2 = topic + ";qos:" + message.getQos() + ";retained:" + message.isRetained();
            Log.i(TAG, "messageArrived:" + str1);
            Log.i(TAG, str2);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.d(TAG, "消息投递完成");
        }
    };

    // MQTT是否连接成功
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            Log.i(TAG, "MQTT客户端和Broker(代理服务器)连接成功 ");
            try {
                // 订阅myTopic话题
                mqttAndroidClient.subscribe(myTopic, 1);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            Log.e(TAG, "MQTT客服端和Broker(代理服务器)连接失败，失败原因：" + arg1.getMessage());
            arg1.printStackTrace();
            // 连接失败，重连
            Log.d("debug", "连接异常，开始重连");
            doClientConnection();
        }
    };

    public static void publish(String msg) {
        String topic = myTopic;
        Integer qos = 0;
        Boolean retained = false;
        try {
            if (mqttAndroidClient != null) {
                mqttAndroidClient.publish(topic, msg.getBytes(), qos.intValue(), retained.booleanValue());
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public MQTTService() {
        Log.e(getClass().getName(), "MQTTService开始构建对象");
    }

    @Override
    public void onCreate() {
        Log.e(getClass().getName(), "MQTTService对象开始执行了onCreate方法");
        super.onCreate();
        init();
    }

    @Override
    public void onDestroy() {
        Log.e(getClass().getName(), "MQTTService对象开始执行了onDestroy方法");
        stopSelf();
        try {
            mqttAndroidClient.disconnect();
            Log.e(getClass().getName(), "MQTT客服端和Broker(代理服务器断开了连接)");
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(getClass().getName(), "MQTTService对象开始执行了onBind方法");
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return new CustomBinder();
    }

    public void setIGetMessageCallBack(IGetMessageCallBack iGetMessageCallBack) {
        this.iGetMessageCallBack = iGetMessageCallBack;
    }

    public class CustomBinder extends Binder {
        public MQTTService getService() {
            return MQTTService.this;
        }
    }

    private void init() {
        String uri = host;
        mqttAndroidClient = new MqttAndroidClient(this, uri, clientId);
        mqttAndroidClient.setCallback(mqttCallback);
        mqttConnectOptions = new MqttConnectOptions();
        //清除缓存
        mqttConnectOptions.setCleanSession(true);
        //设置超时时间，单位：秒
        mqttConnectOptions.setConnectionTimeout(10);
        //心跳发送间隔，单位：秒
        mqttConnectOptions.setKeepAliveInterval(20);
        //mqttConnectOptions.setUserName(userName);
        //mqttConnectOptions.setPassword(password.toCharArray());
        //last will message
        boolean doConnect = true;
        String message = "{\"terminal_uid\":\"" + clientId + "\"}";
        Log.e(getClass().getName(), "message是:" + message);
        String topic = myTopic;
        Integer qos = 0;
        Boolean retained = false;
        if ((!message.equals("")) || (!topic.equals(""))) {
            // 最后的遗嘱
            // MQTT本身就是为信号不稳定的网络设计的，所以难免一些客户端会无故的和Broker断开连接。
            //当客户端连接到Broker时，可以指定LWT，Broker会定期检测客户端是否有异常。
            //当客户端异常掉线时，Broker就往连接时指定的topic里推送当时指定的LWT消息。

            try {
                mqttConnectOptions.setWill(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
            } catch (Exception e) {
                Log.i(TAG, "Exception Occured", e);
                doConnect = false;
                iMqttActionListener.onFailure(null, e);
            }
        }

        if (doConnect) {
            doClientConnection();
        }
    }

    /**
     * 连接MQTT服务器
     */
    private void doClientConnection() {
        if (!mqttAndroidClient.isConnected()) {
            try {
                Log.e(getClass().getName(), "MQTT客服端开始和Broker(代理服务器建立连接)");
                mqttAndroidClient.connect(mqttConnectOptions, null, iMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 判断网络是否连接
     */
    private boolean isConnectIsNormal() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i(TAG, "MQTT当前网络名称：" + name);
            return true;
        } else {
            Log.i(TAG, "MQTT 没有可用网络");
            return false;
        }
    }

    public void toCreateNotification(String message) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, new Intent(this, MQTTService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);//3、创建一个通知，属性太多，使用构造器模式

        Notification notification = builder
                .setTicker("测试标题")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("")
                .setContentText(message)
                .setContentInfo("")
                .setContentIntent(pendingIntent)//点击后才触发的意图，“挂起的”意图
                .setAutoCancel(true)        //设置点击之后notification消失
                .build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        startForeground(0, notification);
        notificationManager.notify(0, notification);
    }
}
