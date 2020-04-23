package si.uni_lj.fri.pbd.miniapp2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity{


    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_READ_EXT_STORAGE = 101;
    public static MediaPlayerService mediaPlayerService;
    private AccelerationService accelerationService;
    private TextView songInfo, songDuration;
    Intent intent, intent2;
    boolean mserviceBound, aserviceBound;
    private boolean flag = false;

    private final Handler mUpdateTimeHandler = new UIUpdateHandler(this);
    private final Handler mUpdateGestureHandler = new GESTUREUpdateHandler(this);
    private final static int MSG_UPDATE_TIME = 0;
    private final static int MSG_UPDATE_GESTURE = 0;
    public static Activity context;
    private boolean isGestureOn = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Media player Service Bound");

            MediaPlayerService.RunServiceBinder binder = (MediaPlayerService.RunServiceBinder) service;
            mediaPlayerService = binder.getService();
            mserviceBound = true;

            if(mediaPlayerService.isMusicPlaying() || mediaPlayerService.isMusicPausing()){
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
                songInfo.setText(mediaPlayerService.getSongInfo());
            }
            mediaPlayerService.foreground();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service Disconnected");
            mserviceBound = false;
        }
    };

    private ServiceConnection aConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Acceleration Service Bound");

            AccelerationService.RunServiceBinder binder = (AccelerationService.RunServiceBinder) service;
            accelerationService = binder.getService();
            aserviceBound = true;

//            if(accelerationService.isGestureOn()){
//                mUpdateGestureHandler.sendEmptyMessage(MSG_UPDATE_GESTURE);
//            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service Disconnected");
            aserviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.out.println("onCreate");

        songDuration = findViewById(R.id.songDuration);
        songInfo = findViewById(R.id.songInfo);
        context = MainActivity.this;

        intent = new Intent(getApplicationContext(), MediaPlayerService.class);
        intent2 = new Intent(getApplicationContext(), AccelerationService.class);

        startService(intent);
        startService(intent2);
    }

    @Override
    protected void onStart() {
        super.onStart();
        System.out.println("onStart");
        Log.d(TAG, "Starting and binding service");

        intent.setAction(MediaPlayerService.ACTION_START);
        bindService(intent, mConnection, 0);

        intent2.setAction(AccelerationService.ACTION_START);
        bindService(intent2, aConnection, 0);
    }

    @Override
    protected void onStop(){
        super.onStop();
        //mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);

        if(mserviceBound){
            unbindService(mConnection);
            mserviceBound = false;

            mediaPlayerService.foreground();

        }

        if(aserviceBound){
            unbindService(aConnection);
            aserviceBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG,"MainActivity onDestroy");
        stopService(new Intent(this,MediaPlayerService.class));
        stopService(new Intent(this, AccelerationService.class));
        super.onDestroy();
    }

    public void playBtnClick(View v){
        System.out.println("play button clicked");
        if(requestReadExternalStoragePermission() == true)
        {
//            if(flag == false){
//                mediaPlayerService.getMusicData();
//                flag = true;
//            }
            mediaPlayerService.playMusic();
            //songInfo.setText(mediaPlayerService.getSongInfo());
            mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
        }
    }

    public void pauseBtnClick(View v){
        System.out.println("pause button clicked");
        mediaPlayerService.pauseMusic();
    }

    public void stopBtnClick(View v){
        System.out.println("stop button clicked");
        mediaPlayerService.stopMusic();
        songInfo.setText("(Song Info)");
        songDuration.setText("(Duration)");
        mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
    }

    public void exitBtnClick(View v){
        System.out.println("exit button clicked");
        finish();
    }

    public void gonBtnClick(View v){

        Toast.makeText(this, "Gesture ON", Toast.LENGTH_SHORT).show();
        isGestureOn = true;
        mUpdateGestureHandler.sendEmptyMessage(MSG_UPDATE_GESTURE);
        //startService(intent2);
    }

    public void goffBtnClick(View v){

        Toast.makeText(this, "Gesture OFF", Toast.LENGTH_SHORT).show();
        isGestureOn = false;
        mUpdateGestureHandler.removeMessages(MSG_UPDATE_GESTURE);
        //stopService(intent2);
    }

    private void updateUIDuration(){
        if(mediaPlayerService.getSongInfo() == null)
            songInfo.setText("(Song Info)");
        else
            songInfo.setText(mediaPlayerService.getSongInfo());

        if(mediaPlayerService.getDuration() == null)
            songDuration.setText("(Song Duration)");
        else
            songDuration.setText(mediaPlayerService.getDuration());
    }

    private void controlGesture(){
        System.out.println("Control Gesture: " + accelerationService.getCommand());
        System.out.println(isGestureOn);
        if(!isGestureOn)
        {
            System.out.println("Gesture OFF");
        }
        else{
            if(accelerationService.getCommand() == accelerationService.HORIZONTAL)
            {
                System.out.println("HORIZONTAL pause music");
                mediaPlayerService.pauseMusic();
            }
            else if(accelerationService.getCommand() == accelerationService.VERTICAL)
            {
                System.out.println("VERTICAL play music");
                if(flag == false){
                    mediaPlayerService.getMusicData();
                    flag = true;
                }
                mediaPlayerService.playMusic();
                songInfo.setText(mediaPlayerService.getSongInfo());
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
            else{
                System.out.println("IDLE");
            }
        }
    }

    class UIUpdateHandler extends Handler{
        private final static int UPDATE_RATE_MS = 1000;
        private final WeakReference<MainActivity> activity;

        UIUpdateHandler(MainActivity activity){
            this.activity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message message){
            if(MSG_UPDATE_TIME == message.what)
            {
                Log.d(TAG, "updating UI");
                activity.get().updateUIDuration();

                if(MediaPlayerService.foregroundService == true)
                {
                    mediaPlayerService.foreground();
                }
                sendEmptyMessageDelayed(MSG_UPDATE_TIME, UPDATE_RATE_MS);
            }
        }
    }

    class GESTUREUpdateHandler extends Handler{
        private final static int UPDATE_RATE_MS = 500;
        private final WeakReference<MainActivity> activity;

        GESTUREUpdateHandler(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message message){
            if(MSG_UPDATE_GESTURE == message.what)
            {
                Log.d(TAG, "updating gesture");
                activity.get().controlGesture();

                sendEmptyMessageDelayed(MSG_UPDATE_GESTURE, UPDATE_RATE_MS);
            }
        }
    }

    private boolean requestReadExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            System.out.println("permission x");
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXT_STORAGE);

            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXT_STORAGE : {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    System.out.println("permission granted");
                    mediaPlayerService.getMusicData();
                    mediaPlayerService.randomPlay();
                    songInfo.setText(mediaPlayerService.getSongInfo());
                } else {
                    System.out.println("permission denied");
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

}



