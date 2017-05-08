package com.apptronix.bluealert;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Set;
import java.util.prefs.PreferenceChangeListener;

public class MainActivity extends AppCompatActivity {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private final int REQUEST_ENABLE_BT = 1;

    private ImageView button;
    TextView statusView;

    int blue_state = 0;

    public static String BLUETOOTH_CONNECTED = "com.apptronix.bluealert.bluetooth-connection-started";
    public static String BLUETOOTH_DISCONNECTED = "com.apptronix.bluealert.bluetooth-connection-lost";
    public static String BLUETOOTH_FAILED = "com.apptronix.bluealert.bluetooth-connection-failed";
    public static String BLUETOOTH_LISTENING = "com.apptronix.bluealert.bluetooth-listening";

    private void getPairedDevice() {
        ArrayList<String> pairedDeviceArray = new ArrayList<String>();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                pairedDeviceArray.add(device.getName() + "\n" + device.getAddress());
            }
        }

        String[] pairedDeviceStringArray = pairedDeviceArray.toArray(new String[pairedDeviceArray.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Connect to paired device")
                .setItems(pairedDeviceStringArray, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        //
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }

                })
                .setPositiveButton("Pair new Device", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent settingsIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                        startActivity(settingsIntent);
                        dialog.cancel();
                    }

                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    SmsManager smsManager;
    BluetoothAdapter mBluetoothAdapter;
    ColorMatrixColorFilter filter;

    public class MessageEvent {

        public final String message;

        public MessageEvent(String message) {
            this.message = message;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            switch (resultCode) {
                case RESULT_OK: {
                    getPairedDevice();
                    break;
                }
                default: {
                    Toast.makeText(this, "Please switch on bluetooth_color to start", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the main; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, IceContacts.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public final String key = "ice_contacts_list";

    public synchronized boolean sendEmergencyMessage() {

        String contactsList = PreferenceManager.getDefaultSharedPreferences(this).getString(key, null);

        if (contactsList == null) return false;

        Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + contactsList));
        smsIntent.putExtra("sms_body", "Help, I'm in trouble!");
        startActivity(smsIntent);


        smsManager = SmsManager.getDefault();


        String lat = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("Lat","");
        String lng = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("Lng","");

        for(String contact: contactsList.split(";")){
            smsManager.sendTextMessage(
                    contact,
                    null,
                    "Help! I'm in trouble at " + lat + "," + lng,
                    null,
                    null
            );
        }

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        r.play();

        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {

        String action=event.message;

        if(action.equals(BLUETOOTH_CONNECTED)){

            statusView.setText("connected");
            Toast.makeText(getApplicationContext(),"Device HC-05 connected",Toast.LENGTH_LONG).show();

        } else if (action.equals(BLUETOOTH_DISCONNECTED)){

            statusView.setText("long press to start!");
            button.setColorFilter(filter);
            blue_state=0;
            Toast.makeText(getApplicationContext(),"Device disconnected",Toast.LENGTH_LONG).show();

        } else if (action.equals(BLUETOOTH_FAILED)){

            Toast.makeText(getApplicationContext(),"Connection Failed",Toast.LENGTH_LONG).show();
            statusView.setText("long press to start!");
            button.setColorFilter(filter);
            blue_state=0;

        } else if (action.equals(BLUETOOTH_LISTENING)){

            statusView.setText("listening");
        }
    };

    public void soundAlarm(){

        String extra="";
        if(sendEmergencyMessage()){
            extra="Message has been sent to ICE contacts";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setTitle("Alarm")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage("The bluetooth alarm has sounded. " + extra)
                .setPositiveButton("dismiss", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final BluetoothSerial bluetoothSerial = new BluetoothSerial(this, new BluetoothSerial.MessageHandler() {
            @Override
            public int read(int bufferSize, byte[] buffer) {

                for(byte x:buffer){
                    if(x=='A'){
                        soundAlarm();
                        return -1;
                    }

                }

                return bufferSize;
            }
        }, "HC-05");

        button = (ImageView)findViewById(R.id.blueButton);
        statusView = (TextView)findViewById(R.id.statusView);



        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        filter = new ColorMatrixColorFilter(matrix);


        button.setColorFilter(filter);      //set image to gray scale
        blue_state=0;

        Glide.with(this).load(R.drawable.bluetooth_color)
            .fitCenter()
            .into(button);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                if(mBluetoothAdapter == null)
                {
                    Toast.makeText(getParent(),"No bluetooth adapter available",Toast.LENGTH_SHORT).show();
                    return true;

                } else if(!mBluetoothAdapter.isEnabled())
                {
                    Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetooth, 0);

                    return true;

                } else {
                    if(blue_state==0){

                        button.clearColorFilter();
                        blue_state=1;
                        statusView.setText("connected.");
                        bluetoothSerial.connect();

                    } else {
                        button.setColorFilter(filter);
                        blue_state=0;
                        statusView.setText("long press to start!");
                        bluetoothSerial.close();
                    }
                }

                return true;
            }
        });

    }



}
