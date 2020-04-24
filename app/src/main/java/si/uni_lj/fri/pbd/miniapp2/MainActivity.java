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

import java.io.IOException;
import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity{
    private static final String TAG = MainActivity.class.getSimpleName();
    private MediaPlayerService mediaPlayerService;
    private TextView songInfo, songDuration;
    Intent intent;
    boolean mserviceBound;

    private final Handler mUpdateTimeHandler = new UIUpdateHandler(this);
    private final static int MSG_UPDATE_TIME = 0;
    public static Activity context;

    //make media player service connection
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
            mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service Disconnected");
            mserviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.out.println("onCreate");

        songDuration = findViewById(R.id.songDuration); //text view that indicates songDuration
        songInfo = findViewById(R.id.songInfo); //text view that indicates songInformation
        context = MainActivity.this;

        intent = new Intent(getApplicationContext(), MediaPlayerService.class);
        startService(intent); //start media player service
    }

    @Override
    protected void onStart() {
        super.onStart();
        System.out.println("onStart");
        Log.d(TAG, "Starting and binding service");

        intent.setAction(MediaPlayerService.ACTION_START);
        bindService(intent, mConnection, 0); //bind media player service

    }

    @Override
    protected void onStop(){
        super.onStop();
        mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);

        if(mserviceBound){
            unbindService(mConnection);
            mserviceBound = false;

            mediaPlayerService.foreground();
        } //unbind media player service


    }

    @Override
    protected void onDestroy() {
        Log.e(TAG,"MainActivity onDestroy");
        stopService(new Intent(this,MediaPlayerService.class)); //when main activity destroyed, stop media player service
        super.onDestroy();
    }

    public void playBtnClick(View v) {
        //play button clicked
        mediaPlayerService.playMusic();
    }

    public void pauseBtnClick(View v){
        //pause button clicked
        mediaPlayerService.pauseMusic();
    }

    public void stopBtnClick(View v){
        //stop button clicked
        mediaPlayerService.stopMusic();
    }

    public void exitBtnClick(View v){
        //exit button clicked
        finish();
    }

    public void gonBtnClick(View v){
        //gestureOn button clicked
        mediaPlayerService.gestureON();
        Toast.makeText(this, "Gesture activated", Toast.LENGTH_SHORT).show();

    }

    public void goffBtnClick(View v){
        //gestureOff button clicked
        mediaPlayerService.gestureOFF();
        Toast.makeText(this, "Gesture deactivated", Toast.LENGTH_SHORT).show();
    }

    private void updateUIDuration(){
        // if mediaPlayerService is stopped, set songInfo and songDuration default.
        // else update songInfo and songDuration
        if(mediaPlayerService.getSongInfo() == null)
            songInfo.setText("(Song Info)");
        else
            songInfo.setText(mediaPlayerService.getSongInfo());

        if(mediaPlayerService.getDuration() ==  null)
            songDuration.setText("(Song Duration)");
        else
            songDuration.setText(mediaPlayerService.getDuration());
    }

    class UIUpdateHandler extends Handler{
        private final static int UPDATE_RATE_MS = 1000; // update every second
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

                sendEmptyMessageDelayed(MSG_UPDATE_TIME, UPDATE_RATE_MS);
            }
        }
    }
}



