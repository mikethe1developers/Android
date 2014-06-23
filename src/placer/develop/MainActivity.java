package placer.develop;

import placer.general.ResultObject;
import placer.general.SearchObject;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	//General
	public static final String TAG = null;
	public static final int PORTRAIT = 90;
	public static final int LANDSCAPE = 0;
	public static ProgressDialog progressDialogSearch;
	public static TextView txtLat, txtLon, txtBearing, txtPitch, txtRoll, txtAzimuth;
	public Button takePhotoButton;
	public static SearchObject searchObject;  //Will be accessed from helper classes, therefore is static
	public static ResultObject resultObject;  //Will be accessed from helper classes, therefore is static
	public String id = "";
    //FOR THE CAMERA
	public Camera cameraBack;
	public CameraPreview previewBack;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static int orientationAngle = PORTRAIT;  //The default is portrait: 90-Portrait, 0-landscape. This is the way to know the screen orientation at any moment
    //GPS+Sensors
    public static SensorManager sensorManager;
    public static Sensor accelerometerSensor, magneticFeildSensor;
    public LocationManager gpsSensor;
    float[] mGravity = null;
    float[] mGeomagnetic = null;
    float Rmat[] = new float[9];
    float Imat[] = new float[9];
    float rotationMatrixAfterCoordinateChanged[] = new float[9];
    float orientation[] = new float[3];    
    public static double myLatitiude=0.0;
    public static double myLongitude=0.0;
    public static double myAltitude=0.0;
    public static float axisPitch=0.0f;
    public static float axisRoll=0.0f;
    public static float axisAzimuth=0.0f;
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        //id = Secure.getString(this.getContentResolver(),Secure.ANDROID_ID);
        //Registering GPS
        gpsSensor = (LocationManager) getSystemService(LOCATION_SERVICE);
        gpsSensor.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, myLocationListener);
        //Registering other sensors       
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); 	
        sensorManager.registerListener(accelerometerListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL); 
        sensorManager.registerListener(magnetometerListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
		//Regular tasks
		txtLat = (TextView) findViewById(R.id.textLat);
		txtLon = (TextView) findViewById(R.id.textLon);
		txtBearing = (TextView) findViewById(R.id.textBearing);
		txtPitch = (TextView) findViewById(R.id.textPitch);
		txtRoll = (TextView) findViewById(R.id.textRoll);
		txtAzimuth = (TextView) findViewById(R.id.textAzimuth);
		takePhotoButton = (Button) findViewById(R.id.take_photo);
		takePhotoButton.setClickable(true);
		takePhotoButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				/*if (false == gpsEnableChecker()) {}
				else {*/
					//Here we do the Job after all parameters are ready!
					//TODO: Call to Google elevation API
					myAltitude = 100;  //For example
					/*
					 * Loading start responsibility:
					 * 	1. Make calls to DB
					 * 	2. Create & populate the result object of this  
					 * 	Attenuation: all the variables are belong to this
					 */
					MainActivity.searchObject = new SearchObject(id, myLatitiude, myLongitude, myAltitude, axisPitch, axisRoll, axisAzimuth);
					new LoadingStart(MainActivity.this).execute();
				//}
	        }
	    });
    }
    
    //METHODS FOR CAMERA
	private void initCamera() {		
        cameraBack = getCameraInstance(0);  											//Create an instance of Camera: 0->back, 1->front        
        previewBack = new CameraPreview(this, cameraBack, orientationAngle);  			//Create our Preview view and set it as the content of our activity.
        FrameLayout framePreviewBack = (FrameLayout) findViewById(R.id.camera_preview_back);
        framePreviewBack.addView(previewBack);
	}
	
    private void releaseCamera() {
        if (cameraBack != null){
            FrameLayout previewBack = (FrameLayout) findViewById(R.id.camera_preview_back);
            previewBack.removeView(previewBack);
        	cameraBack.release();        //release the camera for other applications
        	cameraBack = null;
        }
    }
    
    private Camera getCameraInstance(int x) {  	
        int cameraCount = 0;	//A safe way to get an instance of the Camera object. x=0->back, x=1->front
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for ( int camIdx = 0; camIdx < cameraCount; camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo );
            if ((x==1) && (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)) {
            	try {
                    cam = Camera.open( camIdx );
                } catch (RuntimeException e) {
                }
            }
            else if ((x==0) && (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)) {
                try {
                    cam = Camera.open( camIdx );
                } catch (RuntimeException e) {
                }
            }
        }
        return cam;
    }
    
	//GPS Listener
	LocationListener myLocationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
	    	if (location.getLongitude() != 0.0 && location.getLatitude() != 0.0) {
	    		myLatitiude = location.getLatitude();
	    		myLongitude = location.getLongitude();
	    		updateLocationText();
	    	}
	    }
		public void onProviderDisabled(String provider) {
			gpsMessage();
	    }
		public void onProviderEnabled(String provider) {

	    }
	    public void onStatusChanged(String provider, int status, Bundle extras) {
	    	
	    }
	};
	
	//Accelerometer Listener
    SensorEventListener accelerometerListener = new SensorEventListener(){
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity = event.values.clone();
                processSensorData();
            }
        }   
    };
    
    //Magnetometer Listener
    SensorEventListener magnetometerListener = new SensorEventListener(){
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
                mGeomagnetic = event.values.clone();
                processSensorData();                
            }
        }   
    };
    
    //Helper methods for sensors
    private float restrictAngle(float tmpAngle) {
        while(tmpAngle>=180) tmpAngle-=360;
        while(tmpAngle<-180) tmpAngle+=360;
        return tmpAngle;
    }
    //x is a raw angle value from getOrientation(...), y is the current filtered angle value
    private float calculateFilteredAngle(float x, float y) { 
        final float alpha = 0.3f;
        float diff = x-y;
        //here, we ensure that abs(diff)<=180
        diff = restrictAngle(diff);
        y += alpha*diff;
        //ensure that y stays within [-180, 180] bounds
        y = restrictAngle(y);
        return y;
    }

    public void processSensorData() {
        if (mGravity != null && mGeomagnetic != null) { 
            boolean success = SensorManager.getRotationMatrix(Rmat, Imat, mGravity, mGeomagnetic);
            if (success) {
            	/*if (orientationAngle==0) SensorManager.remapCoordinateSystem(Rmat, SensorManager.AXIS_X, SensorManager.AXIS_Z, rotationMatrixAfterCoordinateChanged);  //Orient to landscape
            	else rotationMatrixAfterCoordinateChanged = Rmat.clone();  //The default is portrait*/
            	SensorManager.getOrientation(Rmat, orientation);                  	 	               
                MainActivity.axisAzimuth = /*(float)Math.toDegrees((double)orientation[0]);*/calculateFilteredAngle((float)Math.toDegrees((double)orientation[0]), MainActivity.axisAzimuth);  //azimuth
                MainActivity.axisPitch = /*(float)Math.toDegrees((double)orientation[1]);*/calculateFilteredAngle((float)Math.toDegrees((double)orientation[1]), MainActivity.axisPitch);  //pitch
                MainActivity.axisRoll = /*(float)Math.toDegrees((double)orientation[2]);*/calculateFilteredAngle((float)Math.toDegrees((double)orientation[2]), MainActivity.axisRoll);  //-roll
    			updateAttitudeText(MainActivity.axisPitch, MainActivity.axisRoll, MainActivity.axisAzimuth);
            }           
            mGravity=null;
            mGeomagnetic=null;
        }
    }	

    private boolean gpsEnableChecker() {
    	if ( myLatitiude == 0 && myLongitude == 0 ) {   		
    		gpsMessage();
     		return false;
    	}
    	return true;
	}

	private void gpsMessage() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);	
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
					startActivity(intent);
				}
			});
		AlertDialog alertDialog = alertDialogBuilder.create();
 		alertDialog.show();
	}
	
	private void updateLocationText() {
		txtLat.setText("Lat: "+myLatitiude);
		txtLon.setText("Lon: "+myLongitude);
	}
	
	private void updateAttitudeText(float pitch, float roll, float azimuth) {
		txtPitch.setText("Pitch: "+Float.toString(pitch));
		txtRoll.setText("Roll: "+Float.toString(roll));
		txtAzimuth.setText("Azimuth: "+Float.toString(azimuth));
	}
    
	@Override
    public void onResume() {
    	super.onResume();
    	if (getResources().getConfiguration().orientation == 1) orientationAngle = 90;  //Portrait
    	else {
    		orientationAngle = 0;
    	}
    	initCamera();
    }
      
    @Override
    public void onDestroy(){
        super.onDestroy();
        gpsSensor.removeUpdates(myLocationListener);
        sensorManager.unregisterListener(accelerometerListener);
        sensorManager.unregisterListener(magnetometerListener);
        releaseCamera();              
    }
  
    @Override
    protected void onPause() {
        super.onPause();			
        releaseCamera(); 
    }

}