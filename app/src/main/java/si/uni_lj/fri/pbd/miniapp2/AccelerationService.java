package si.uni_lj.fri.pbd.miniapp2;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import static android.content.ContentValues.TAG;

public class AccelerationService extends Service implements SensorEventListener {
    public static final String GESTURE_ON = "gesture_on";
    public static final String GESTURE_OFF = "gesture_off";
    public static final String ACTION_START = "action_start";
    private final int Noise_threshold = 5;
    private static SensorManager sensorManager;
    private Sensor mAccelerometer;
    float[] mGravity = null;
    public static final String IDLE = "idle";
    public static final String HORIZONTAL = "horizontal";
    public static final String VERTICAL = "vertical";
    private String Command = IDLE;
    private IBinder serviceBinder = new AccelerationService.RunServiceBinder();

    float Xt=0, Yt=0, Zt=0;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Binding service");
        return serviceBinder;
        //return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service");

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate(){
        Log.d(TAG, "Creating service");
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        //Command = IDLE;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying service");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //System.out.println("onSensorChanged");
        float dX, dY, dZ;

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            mGravity = event.values;
            //System.out.println(Arrays.toString(mGravity));
        }
        if(mGravity != null){
            dX = Math.abs(Xt - mGravity[0]);
            dY = Math.abs(Yt - mGravity[1]);
            dZ = Math.abs(Zt - mGravity[2]);

            if(dX<=Noise_threshold)
                dX = 0;
            if(dY<=Noise_threshold)
                dY = 0;
            if(dZ <=Noise_threshold)
                dZ = 0;

            if(dX > dZ)
                Command = HORIZONTAL;
            else if(dZ > dX)
                Command = VERTICAL;
            else{
                Command = IDLE;
            }

            //System.out.println("in service command: " + Command);
            Xt = mGravity[0];
            Yt = mGravity[1];
            Zt = mGravity[2];
        }


    }

    public String getCommand(){
        return Command;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public class RunServiceBinder extends Binder {
        AccelerationService getService(){
            return AccelerationService.this;
        }
    }

}
