package placer.general;

import java.util.LinkedList;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;

/**
 * This is helper class just to put code outside MainActivity
 */

public class HelperMethods {
	
    //Helper methods for sensors
	public static float restrictAngle(float tmpAngle) {
        while (tmpAngle >= 180) tmpAngle -= 360;
        while (tmpAngle < -180) tmpAngle += 360;
        return tmpAngle;
    }
    
    //x is a raw angle value from getOrientation(...), y is the current filtered angle value
	public static float calculateFilteredAngle(float x, float y) { 
        final float alpha = 0.3f;
        float diff = x-y;
        //here, we ensure that abs(diff)<=180
        diff = restrictAngle(diff);
        y += alpha*diff;
        //ensure that y stays within [-180, 180] bounds
        y = restrictAngle(y);
        return y;
    }
	
    /**
     * 
     * @param List containing sensor raw data collected during SENSOR_COLLECTION_TIME  
     * @return Angle (pitch/roll/azimuth according to the kind of the list) after:
     * 1. Smoothing the data by activating algorithmic implementation of lowpass filter
     * 2. Averaging the data by cutting off extreme values (max/min) and perform simple arithmetic average
     */
	public static float smootherAndAverager(LinkedList<Float> list) {
		final float ALPHA = 0.2f;   //0 < ALPHA < 1; a smaller value basically means more smoothing
		float tmpVal = 0, max = 0, min = 0;
		//Low pass
		for (int i=1; i<list.size(); i++) {
			tmpVal = list.get(i);
			list.set(i, list.get(i-1) + ALPHA * (tmpVal - list.get(i-1)));
			if (i == 0) min = list.get(0);
			if (list.get(i) > max) max = list.get(i);
			if (list.get(i) < min) min = list.get(i);
		}
		//Average: Delete max min value & then compute the average
		list.remove(max);
		list.remove(min);
		for (int i=1; i<list.size(); i++) {
			tmpVal += list.get(i);
		}
		return tmpVal/list.size();
	}
	
	public static void gpsMessage(final Context cnt) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(cnt);	
			alertDialogBuilder.setTitle("Weak GPS signal");
			alertDialogBuilder
			.setMessage("Placer need Fine GPS signal - Press OPEN to enable GPS")
			.setCancelable(false)
			.setPositiveButton("OK",new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) {
					//Nothing
				}
			})
			.setNegativeButton("OPEN",new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) { 				
					Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);  // if this button is clicked open menu
					cnt.startActivity(intent);
				}
			});
		AlertDialog alertDialog = alertDialogBuilder.create();
 		alertDialog.show();
	}
	
	//As members
	//public static Sensor accelerometerSensor, magneticFeildSensor;
	//Under onCreate
    /*sensorManager.registerListener(accelerometerListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL); 
    sensorManager.registerListener(magnetometerListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);*/
	//Near other listener
	/*
	//Accelerometer Listener
    SensorEventListener accelerometerListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity = event.values.clone();
                processSensorData();
            }
        }   
    };
    
    //Magnetometer Listener
    SensorEventListener magnetometerListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic = event.values.clone();
                processSensorData();                
            }
        }   
    };
    */
	//Under onDestroy
    /*sensorManager.unregisterListener(accelerometerListener);
    sensorManager.unregisterListener(magnetometerListener);*/

}