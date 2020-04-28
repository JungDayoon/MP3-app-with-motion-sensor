package si.uni_lj.fri.pbd.miniapp2;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Random;

import static android.content.ContentValues.TAG;

public class MediaPlayerService extends Service {

    public static final String ACTION_START = "start service";

    public static final String ACTION_EXIT = "exit_service";

    public static final String ACTION_STOP= "stop_service";

    public static final String ACTION_TOGGLE_PLAY = "toggle_service";

    public static final String channelID = "background_timer";

    private boolean isMusicPlaying;
    private boolean isMusicPausing;
    private MediaPlayer mediaPlayer;
    private IBinder serviceBinder = new RunServiceBinder();
    int resMp3[] = {R.raw.a, R.raw.b, R.raw.c};
    String songInfo[] = {"Calvin Harris - Slide", "Ed Sheeran - South of the Border (feat.Camila Cabello, Cardi B)", "Ellie Goulding - Love me like you do"};
    private int randomPos;
    private long startTime, endTime;
    private String songInformation;
    private int pos;
    public static int NOTIFICATION_ID = 100;
    public NotificationCompat.Builder builder;
    public static boolean foregroundService = false;
    private AccelerationService accelerationService;
    private final Handler mUpdateGestureHandler = new MediaPlayerService.GESTUREUpdateHandler(this);
    private final static int MSG_UPDATE_GESTURE = 0;
    private boolean isGestureOn = false;
    boolean aserviceBound;
    Intent intent;

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d(TAG, "creating service");

        startTime = 0;
        endTime = 0;
        isMusicPlaying = false;

        intent = new Intent(getApplicationContext(), AccelerationService.class);
        startService(intent); //start acceleration player service

        intent.setAction(AccelerationService.ACTION_START);
        bindService(intent, aConnection, 0);

        createNotificationChannel();
    }

    //make acceleration service connection
    private ServiceConnection aConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Acceleration Service Bound");

            AccelerationService.RunServiceBinder binder = (AccelerationService.RunServiceBinder) service;
            accelerationService = binder.getService();
            aserviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service Disconnected");
            aserviceBound = false;
        }
    };
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service");

        System.out.println("state: " + intent.getAction());

        // TODO: check the intent action and if equal to ACTION_STOP, stop the foreground service
        if(intent.getAction() == ACTION_TOGGLE_PLAY) //in notification, we should implement toggle play
        {
            if(isMusicPlaying()) // if music is playing, we should pause music.
            {
                pauseMusic();
            }
            else // if music is pausing, we should play music
                playMusic();
        }
        else if(intent.getAction() == ACTION_STOP)
        {
            stopMusic();
        }

        else if(intent.getAction() == ACTION_EXIT)
        {
            exitMusic();
        }
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Binding service");
        return serviceBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying media player service");

        if(isMusicPlaying()) //if music is playing, we should stop playing music
            stopMusic();

        stopService(new Intent(this, AccelerationService.class)); // stop acceleration service

        //unbind acceleration service
        if(aserviceBound){
            unbindService(aConnection);
            aserviceBound = false;
        }
    }

    public boolean isMusicPlaying() {
        return isMusicPlaying;
    }

    public boolean isMusicPausing() {
        return isMusicPausing;
    }

    public String getSongInfo() {
        //get song information
        if (!isMusicPlaying() && !isMusicPausing()) {
            return null;
        }
        return songInformation;
    }

    public String getDuration(){
        //get song duration
        if(!isMusicPlaying() && !isMusicPausing()) {
            return null;
        }

        //convert msec to timestamp
        int total = mediaPlayer.getDuration() / 1000;
        int total_h = total / 3600;
        int total_m = (total % 3600) / 60;
        int total_s = (total % 3600) % 60;

        int current = mediaPlayer.getCurrentPosition() / 1000;
        int current_h = current/3600;
        int current_m = (current%3600)/60;
        int current_s = (current%3600)%60;

        if (total_h == current_h && total_m == current_m && total_s == current_s) { // if music ends
            isMusicPlaying = false;
            isMusicPausing = false;
        }

        String totalD = String.format("%02d", total_h) + ":" + String.format("%02d", total_m) + ":" + String.format("%02d", total_s);
        String currentD = String.format("%02d", current_h) + ":" + String.format("%02d", current_m) + ":" + String.format("%02d", current_s);

        return currentD + " / " + totalD;
    }

    public void playMusic() {
        if (!isMusicPlaying && !isMusicPausing) { //if playing music newly
            isMusicPlaying = true;
            Random ran = new Random(); //for random playing
            randomPos = ran.nextInt(resMp3.length);

            mediaPlayer = MediaPlayer.create(MediaPlayerService.this, resMp3[randomPos]);
            songInformation = songInfo[randomPos];
            mediaPlayer.start(); //start music

        } else if (isMusicPausing) {
            isMusicPausing = false;
            isMusicPlaying = true;
            mediaPlayer.seekTo(pos); //Play back from the time you paused it
            mediaPlayer.start();
        } else {
            Log.e(TAG, "playMusic request for an already running player");
        }
    }

    public void pauseMusic() {
        if (isMusicPlaying) {
            pos = mediaPlayer.getCurrentPosition(); //remember the time you clicked pause
            mediaPlayer.pause();
            isMusicPlaying = false;
            isMusicPausing = true;
        }
        else{
            Log.e(TAG, "pauseMusic request for a player that isn't running");
        }
    }

    public void stopMusic(){
        if(isMusicPlaying || isMusicPausing){
            isMusicPlaying = false;
            isMusicPausing = false;
            mediaPlayer.stop();
        }
        else{
            Log.e(TAG, "stopMusic request for a player that isn't running");
        }
    }

    public void exitMusic(){
        MainActivity.context.finish(); //exit main activity
    }

    public void gestureON(){
        isGestureOn = true;
        mUpdateGestureHandler.sendEmptyMessage(MSG_UPDATE_GESTURE); //start updateGestureHandler to control gesture
    }

    public void gestureOFF(){
        isGestureOn = false;
        mUpdateGestureHandler.removeMessages(MSG_UPDATE_GESTURE); //stop updateGestureHandler to stop controlling gesture
    }

    private Notification createNotification() {

        // create notification

        builder = new NotificationCompat.Builder(this, channelID)
                .setContentTitle(getSongInfo())
                .setContentText(getDuration())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setChannelId(channelID);

        Intent resultIntent = new Intent(this, MainActivity.class)
                .setAction(Intent.ACTION_MAIN) .addCategory(Intent.CATEGORY_LAUNCHER) .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // if touch the notification
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(this, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        //stop
        Intent actionIntent = new Intent(this, MediaPlayerService.class);
        actionIntent.setAction(ACTION_STOP);
        PendingIntent actionPendingIntent = PendingIntent.getService(this, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //toggle play
        Intent actionIntent2 = new Intent(this, MediaPlayerService.class);
        actionIntent2.setAction(ACTION_TOGGLE_PLAY);
        PendingIntent actionPendingIntent2 = PendingIntent.getService(this, 0, actionIntent2, PendingIntent.FLAG_UPDATE_CURRENT);

        //exit
        Intent actionIntent3 = new Intent(this, MediaPlayerService.class);
        actionIntent3.setAction(ACTION_EXIT);
        PendingIntent actionPendingIntent3 = PendingIntent.getService(this, 0, actionIntent3, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.addAction(android.R.drawable.ic_media_pause, isMusicPlaying()? "Pause": "Play", actionPendingIntent2);
        builder.addAction(android.R.drawable.ic_media_pause, "Stop", actionPendingIntent);
        builder.addAction(android.R.drawable.ic_media_pause, "Exit", actionPendingIntent3);

        return  builder.build();
    }

    // TODO: Uncomment for creating a notification channel for the foreground service
    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT < 26) {
            return;
        } else {

            NotificationChannel channel = new NotificationChannel(MediaPlayerService.channelID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.channel_desc));
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);

            NotificationManager managerCompat = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            managerCompat.createNotificationChannel(channel);
        }
    }

    public void foreground(){
        startForeground(NOTIFICATION_ID, createNotification());
        foregroundService = true;
    }

    public void background(){
        stopForeground(true);
        foregroundService = false;
    }

    public class RunServiceBinder extends Binder {
        MediaPlayerService getService(){
            return MediaPlayerService.this;
        }
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
                pauseMusic();
            }
            else if(accelerationService.getCommand() == accelerationService.VERTICAL)
            {
                System.out.println("VERTICAL play music");
                playMusic();
            }
            else{
                System.out.println("IDLE");
            }
        }
    }

    class GESTUREUpdateHandler extends Handler{
        private final static int UPDATE_RATE_MS = 500;
        private final WeakReference<MediaPlayerService> activity;

        GESTUREUpdateHandler(MediaPlayerService activity) {
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
}
