package si.uni_lj.fri.pbd.miniapp2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
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
import androidx.core.app.NotificationManagerCompat;

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
    private ArrayList<MusicData> musicList = new ArrayList<>();
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private IBinder serviceBinder = new RunServiceBinder();

    private int randomPos;
    private long startTime, endTime;
    private String songInformation;
    private int pos;
    public static int NOTIFICATION_ID = 100;
    public NotificationCompat.Builder builder;
    public static boolean foregroundService = false;
    //private final Handler mUpdateTimeHandler = new MainActivity.UIUpdateHandler(this);

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d(TAG, "creating service");

        startTime = 0;
        endTime = 0;
        isMusicPlaying = false;

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service");

        System.out.println("state: " + intent.getAction());

        // TODO: check the intent action and if equal to ACTION_STOP, stop the foreground service
        if(intent.getAction() == ACTION_TOGGLE_PLAY)
        {
            if(isMusicPlaying())
            {
                pauseMusic();
            }
            else
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
        Log.d(TAG, "Destroying service");
    }

    public boolean isMusicPlaying(){
        return isMusicPlaying;
    }

    public boolean isMusicPausing(){
        return isMusicPausing;
    }
    public void getMusicData() {
        ContentResolver contentResolver = getContentResolver();
        // 음악 앱의 데이터베이스에 접근해서 mp3 정보들을 가져온다.

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);
        cursor.moveToFirst();
        System.out.println("음악파일 개수 = " + cursor.getCount());
        if (cursor != null && cursor.getCount() > 0) {
            do {
                long track_id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                Integer mDuration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                String datapath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                System.out.println("mId = " + track_id + " albumId = " + albumId + " title : " + title + " album : " + album + " artist: " + artist + " totalDuration : " + mDuration + " data : " + datapath);
                // Save to audioList
                MusicData musicData = new MusicData(track_id, albumId, title, artist, album, mDuration, datapath, false);
                musicList.add(musicData);

            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    public void randomPlay(){
        Random ran = new Random();
        randomPos = ran.nextInt(musicList.size());
        MusicData item = musicList.get(randomPos);

        try {
            songInformation = item.getTitle() + " - " + item.getArtist();
            mediaPlayer.reset();
            mediaPlayer.setDataSource(item.getDataPath());
            mediaPlayer.prepare();
            mediaPlayer.start(); // 노래 재생 시작
        } catch (IOException e) {
            e.getMessage();
        }

        System.out.println("randomPos: " + randomPos);
    }

    public String getSongInfo(){
        if(!isMusicPlaying() && !isMusicPausing())
        {
            return null;
        }
        return songInformation;
    }

    public String getDuration(){
        if(!isMusicPlaying() && !isMusicPausing())
        {
            return null;
        }
        int total = mediaPlayer.getDuration()/1000;
        int total_h = total/3600;
        int total_m = (total%3600)/60;
        int total_s = (total%3600)%60;

        int current = mediaPlayer.getCurrentPosition()/1000;
        int current_h = current/3600;
        int current_m = (current%3600)/60;
        int current_s = (current%3600)%60;

        String totalD = String.format("%02d", total_h) + ":" + String.format("%02d", total_m) + ":" + String.format("%02d", total_s);
        String currentD = String.format("%02d", current_h) + ":" + String.format("%02d", current_m) + ":" + String.format("%02d", current_s);

        return currentD + " / " + totalD;
    }

    public void playMusic() {
        if (!isMusicPlaying && !isMusicPausing) {
            if(musicList.isEmpty())
            {
                getMusicData();
                System.out.println("music list is empty");
            }

            startTime = System.currentTimeMillis();
            isMusicPlaying = true;
            for(int i = 0; i<musicList.size();i++){
                System.out.println(musicList.get(i));
            }
            randomPlay();
        } else if (isMusicPausing) {
            isMusicPausing = false;
            isMusicPlaying = true;
            mediaPlayer.seekTo(pos);
            mediaPlayer.start();
        } else {
            Log.e(TAG, "playMusic request for an already running player");
        }

        if(foregroundService == true)
            foreground();
    }

    public void pauseMusic() {
        if (isMusicPlaying) {
            pos = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            isMusicPlaying = false;
            isMusicPausing = true;
        }
        else{
            Log.e(TAG, "pauseMusic request for a player that isn't running");
        }

        if(foregroundService == true)
            foreground();
    }

    public void stopMusic(){
        if(isMusicPlaying || isMusicPausing){
            endTime = System.currentTimeMillis();
            isMusicPlaying = false;
            isMusicPausing = false;
            mediaPlayer.stop();
        }
        else{
            Log.e(TAG, "stopMusic request for a player that isn't running");
        }
        if(foregroundService == true)
            foreground();
    }

    public void exitMusic(){
        MainActivity.context.finish();
    }

    private Notification createNotification() {

        // TODO: add code to define a notification action

        builder = new NotificationCompat.Builder(this, channelID)
                .setContentTitle(getSongInfo())
                .setContentText(getDuration())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setChannelId(channelID);

        Intent resultIntent = new Intent(this, MainActivity.class)
                .setAction(Intent.ACTION_MAIN) .addCategory(Intent.CATEGORY_LAUNCHER) .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

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

}
