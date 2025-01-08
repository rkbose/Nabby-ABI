package com.example.myhelloapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

//import android.text.method.ScrollingMovementMethod;
import android.util.Log;
//import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
//import android.view.View;
import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private static final String MAINACTIVITY_TAG = MainActivity.class.getSimpleName();
    static private DatagramSocket ds = null;
    private TextView scrollwindow;
    private MyHandler myHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button mdnsdoorbellButton1 = findViewById(R.id.buttonMDNSdoorbell);//get id of button 1
        Button mdnsnabbyButton2 = findViewById(R.id.buttonMDNSnabby);//get id of button 2
        scrollwindow = findViewById(R.id.textView);
 //       scrollwindow.setMovementMethod(new ScrollingMovementMethod());
        scrollwindow.append("\n\nNabbyAbi started \n");

        myHandler = new MyHandler(this);

        try {
            ds = new DatagramSocket();
            //          Log.d(MAINACTIVITY_TAG, "--- Socket created, port: "+ ds.getPort());
        } catch (IOException e) {
            Log.d(MAINACTIVITY_TAG, "--- ERROR Socket not created");
        }

        scrollwindow.append("rcvThread started \n");
        if (!rcvThread.isAlive()) {
            rcvThread.start();
            Toast.makeText(getApplicationContext(), "rcvThread started", Toast.LENGTH_LONG).show();//display the text of button2
        }

        mdnsdoorbellButton1.setOnClickListener(view -> {
            scrollwindow.append("TX: Ring command sent \n");
            //          scrollview.fullScroll(View.FOCUS_DOWN); //scroll automatically to end
            Thread txinfThread = new Thread(new txinfThread());
            txinfThread.start();
            Toast.makeText(getApplicationContext(), "txinfThread started", Toast.LENGTH_LONG).show();//display the text of button1
        });

        mdnsnabbyButton2.setOnClickListener(view -> Toast.makeText(getApplicationContext(), "no function", Toast.LENGTH_LONG).show());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Static inner class to avoid memory leaks
    static class MyHandler extends Handler {
        private final WeakReference<MainActivity> activityWeakReference;

        public MyHandler(MainActivity activity) {
            super(Looper.getMainLooper());
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            MainActivity activity = activityWeakReference.get();
            if (activity != null) {
                String message = msg.getData().getString("message");
                activity.scrollwindow.append(message + "\n\n");
            }
        }
    }

    static class txinfThread implements Runnable {
        @Override
        public void run() {
            try {
                InetAddress address = InetAddress.getByName("192.168.23.103"); //103 (Nabby) of 160 (doorbell)
                int port = 1235;
                String s = "/inf\r";
                byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
                ds.send(packet);
                //      ds.close();
            } catch (IOException e) {
                Log.d(MAINACTIVITY_TAG, "--- ERROR");
                throw new RuntimeException(e);
            }
        }
    }

    Thread rcvThread = new Thread(new Runnable() {
        public void run() {
            try {
                scrollwindow = findViewById(R.id.textView); // get id of scroll window
                while (!Thread.currentThread().isInterrupted()) {
                    byte[] messagebytes = new byte[1500];
                    DatagramPacket p = new DatagramPacket(messagebytes, messagebytes.length);
                    ds.receive(p);
                    String rec_str = new String(p.getData());
                    // Send a message to the main thread
                    Message message = Message.obtain(); // obtain message from pool
                    Bundle bundle = new Bundle();
                    bundle.putString("message", "RCV: " + rec_str);
                    message.setData(bundle);
                    myHandler.sendMessage(message);
                }
                Log.d(MAINACTIVITY_TAG, "---stopped listening ");
            } catch (SocketException e) {
                Log.d(MAINACTIVITY_TAG, "DatagramSocket exception not opened ");
                throw new RuntimeException(e);
            } catch (IOException e) {
                Log.d(MAINACTIVITY_TAG, "DatagramSocket IO exception ");
                throw new RuntimeException(e);
            }
        }
    });
}

