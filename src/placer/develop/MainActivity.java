package placer.develop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	public static final String TAG = null;
	public static ProgressDialog progressDialogSearch;
	public static TextView txtLat, txtLon, txtBearing, txtPitch, txtRoll, txtAzimuth;
	public Button takePhotoButton;
	public static SearchObject searchObject;
    //FOR THE CAMERA
	public Camera mCameraBack;
	public CameraPreview mPreviewBack;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static int orientationAngle = 90;  //The default is portrait: 90-Portrait, 0-landscape. This is the way to know the screen orientation at any moment
    //GPS+Sensors
    public static SensorManager sensorManager;
    public static Sensor accelerometerSensor, magneticFeildSensor;
    public LocationManager gpsSensor;
    //Sensors
    float[] mGravity = null;
    float[] mGeomagnetic = null;
    float Rmat[] = new float[9];
    float Imat[] = new float[9];
    float rotationMatrixAfterCoordinateChanged[] = new float[9];
    float orientation[] = new float[3];    
    public static Double myLatitiude=0.0;
    public static Double myLongitude=0.0;
    public static Float myBearing=(float) 0.0;
    public static Float axisPitch=(float) 0.0;
    public static Float axisRoll=(float) 0.0;
    public static Float axisAzimuth=(float) 0.0;
    //Connecting DB
    public Connection connection = null;
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
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
					/*//Here we do the Job!
					MainActivity.searchObject = new SearchObject(myLatitiude, myLongitude, myBearing, axisPitch, axisRoll, axisAzimuth);
					new LoadingStart(MainActivity.this).execute();*/
					try {
						Log.e(TAG,"start");
						Class.forName("org.postgresql.Driver");	
						connection = DriverManager.getConnection("jdbc:postgresql://postgresql-db1.cp3lk1mrandp.us-west-2.rds.amazonaws.com:5432/dbname", "michael", "jankyur2");
						Log.e(TAG,"end");
					} 
					catch (ClassNotFoundException e) {
						return;
					}
					catch (SQLException e) {						 
						return;
					}
					if (connection != null) {
						Log.e(TAG,"1234567890");
					}
				//}
	        }
	    });
    }

    //METHODS FOR CAMERA
	private void initCamera() {		
        mCameraBack = getCameraInstance(0);  											//Create an instance of Camera: 0->back, 1->front        
        mPreviewBack = new CameraPreview(this, mCameraBack, orientationAngle);  							//Create our Preview view and set it as the content of our activity.
        FrameLayout previewBack = (FrameLayout) findViewById(R.id.camera_preview_back);
        previewBack.addView(mPreviewBack);
	}
	
    private void releaseCamera() {
        if (mCameraBack != null){
            FrameLayout previewBack = (FrameLayout) findViewById(R.id.camera_preview_back);
            previewBack.removeView(mPreviewBack);
        	mCameraBack.release();        //release the camera for other applications
        	mCameraBack = null;
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
	    		myBearing = location.getBearing();
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
		txtLat.setText("Lat: "+myLatitiude.toString());
		txtLon.setText("Lon: "+myLongitude.toString());
		txtBearing.setText("Bearing: "+myBearing.toString());
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