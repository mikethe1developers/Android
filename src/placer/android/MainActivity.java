package placer.android;
/*
 * rotation matrix->3d line in the real geographic world->postgis can input 3d line and find intersect directly!
 */
import java.util.LinkedList;

import placer.custom.LocationObject;
import placer.custom.OrientationObject;
import placer.custom.ResultObject;
import placer.custom.SearchObject;
import placer.develop.R;
import placer.general.CameraPreview;
import placer.general.HelperMethods;
import placer.general.LoadingStart;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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
	public static final int SENSOR_COLLECTION_LIMIT = 20;  			//FIFO: Each sensor takes sample every 5-12 milliseconds, so for example 20 (actually 21) means 200 milliseconds
	public static ProgressDialog progressDialogSearch;
	public static TextView txtLat, txtLon, txtPitch, txtRoll, txtAzimuth;
	public static Button takePhotoButton;
	public static SearchObject searchObject;  						
	public static ResultObject resultObject;
	public static LocationObject locationObject;
	public static OrientationObject orientationObject;
	public static String deviceId = "";
    public static double myLatitiude=0.0;
    public static double myLongitude=0.0;
    public static double myAltitude=0.0;
    public static float axisPitch=0.0f;
    public static float axisRoll=0.0f;
    public static float axisAzimuth=0.0f;
    //FOR THE CAMERA
	public static Camera cameraBack;
	public static CameraPreview previewBack;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static int orientationAngle = PORTRAIT;  				//The default is portrait: 90-Portrait, 0-landscape. This is the way to know the screen orientation at any moment
    //GPS+Sensors
    public static SensorManager sensorManager;
    public static Sensor rotationVectorSensor;
    public LocationManager gpsSensor;
    public static float[] mGravity = null;
    public static float[] mGeomagnetic = null;
    public static float[] mRotation = null;
    public static float Rmat[] = new float[16];
    public static float Imat[] = new float[9];
    public static float rotationMatrixAfterCoordinateChanged[] = new float[9];  	//If using rotationVectorSensor only, so no need in this
    public static float orientation[] = new float[3];    
    public static LinkedList<Float> pitchList = new LinkedList<Float>();	//The lists are FIFO up to SENSOR_COLLECTION_LIMIT 
    public static LinkedList<Float> rollList = new LinkedList<Float>();	//The lists are FIFO up to SENSOR_COLLECTION_LIMIT 
    public static LinkedList<Float> azimuthList = new LinkedList<Float>();	//The lists are FIFO up to SENSOR_COLLECTION_LIMIT 
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        //Getting unique device id for tracking this user number of clicks for statistics using GAE datastore
        deviceId = Secure.getString(this.getContentResolver(),Secure.ANDROID_ID);
        //Registering GPS
        gpsSensor = (LocationManager) getSystemService(LOCATION_SERVICE);
        gpsSensor.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, myLocationListener);
        //Registering other sensors       
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); 	
        sensorManager.registerListener(rotationListener, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_NORMAL);
		//Regular tasks
		txtLat = (TextView) findViewById(R.id.textLat); txtLon = (TextView) findViewById(R.id.textLon); txtPitch = (TextView) findViewById(R.id.textPitch);		
		txtRoll = (TextView) findViewById(R.id.textRoll); txtAzimuth = (TextView) findViewById(R.id.textAzimuth);		
		takePhotoButton = (Button) findViewById(R.id.take_photo);
		takePhotoButton.setClickable(true);
		takePhotoButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//if (false == gpsEnableChecker()) {/*The message is sent from gpsEnableChecker() directly...*/}  
				//else {
					myAltitude = 100;  //TODO: Call to Google elevation API, this is for example
					//The FIFO lists are (always...) ready, time to smooth and average the data
					MainActivity.axisAzimuth= HelperMethods.smootherAndAverager(azimuthList);
					MainActivity.axisPitch	= HelperMethods.smootherAndAverager(pitchList);
					MainActivity.axisRoll 	= HelperMethods.smootherAndAverager(rollList);					
					/*
					 * Loading start responsibility:
					 * 	1. Make calls to DB
					 * 	2. Create & populate the result object of this  
					 * 	Attenuation: all the variables are belong to this
					 */
					MainActivity.locationObject 	= new LocationObject(myLatitiude, myLongitude, myAltitude);
					MainActivity.orientationObject 	= new OrientationObject(axisPitch, axisRoll, axisAzimuth);
					MainActivity.searchObject 		= new SearchObject(deviceId, myLatitiude, myLongitude, myAltitude, axisPitch, axisRoll, axisAzimuth);
					new LoadingStart(MainActivity.this).execute();
					Log.e(TAG,"###azimuth: "+MainActivity.axisAzimuth); Log.e(TAG,"###pitch: "+MainActivity.axisPitch); Log.e(TAG,"###roll: "+MainActivity.axisRoll);
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
			HelperMethods.gpsMessage(MainActivity.this);
	    }
		public void onProviderEnabled(String provider) {

	    }
	    public void onStatusChanged(String provider, int status, Bundle extras) {
	    	
	    }
	};
    
    //Rotation Listener
    SensorEventListener rotationListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                mRotation = event.values.clone();
                processRotationVectorSensorData();                
            }
        }   
    };

    public void processSensorData() {
        if (mGravity != null && mGeomagnetic != null) { 
            boolean success = SensorManager.getRotationMatrix(Rmat, Imat, mGravity, mGeomagnetic);
            if (success) {
            	/*if (orientationAngle==0) SensorManager.remapCoordinateSystem(Rmat, SensorManager.AXIS_X, SensorManager.AXIS_Z, rotationMatrixAfterCoordinateChanged);  //Orient to landscape
            	else rotationMatrixAfterCoordinateChanged = Rmat.clone();  //The default is portrait*/
            	SensorManager.getOrientation(Rmat, orientation);                  	 	               
                MainActivity.axisAzimuth = HelperMethods.calculateFilteredAngle((float)Math.toDegrees((double)orientation[0]), MainActivity.axisAzimuth);  //azimuth
                MainActivity.axisPitch = HelperMethods.calculateFilteredAngle((float)Math.toDegrees((double)orientation[1]), MainActivity.axisPitch);  //pitch
                MainActivity.axisRoll = HelperMethods.calculateFilteredAngle((float)Math.toDegrees((double)orientation[2]), MainActivity.axisRoll);  //-roll
    			if (azimuthList.size() > SENSOR_COLLECTION_LIMIT) {
    				azimuthList.remove();  //Removes the head
    				pitchList.remove();
    				rollList.remove();
    			}
                azimuthList.add(MainActivity.axisAzimuth);  //Add is to the tail
    			pitchList.add(MainActivity.axisPitch);
    			rollList.add(MainActivity.axisRoll);
                updateAttitudeText(MainActivity.axisPitch, MainActivity.axisRoll, MainActivity.axisAzimuth);
            }           
            mGravity=null;
            mGeomagnetic=null;
        }
    }
    
    public void processRotationVectorSensorData() {
    	if (mRotation != null) {
    		float RmatVector[] = new float[16];
    		SensorManager.getRotationMatrixFromVector(RmatVector, mRotation);
        	SensorManager.remapCoordinateSystem(RmatVector, SensorManager.AXIS_X, SensorManager.AXIS_Z, Rmat);  //Check sizes
        	SensorManager.getOrientation(Rmat, orientation);
            MainActivity.axisAzimuth = HelperMethods.calculateFilteredAngle((float)Math.toDegrees((double)orientation[0]), MainActivity.axisAzimuth);  //azimuth
            MainActivity.axisPitch = (-1)*HelperMethods.calculateFilteredAngle((float)Math.toDegrees((double)orientation[1]), MainActivity.axisPitch);  //pitch: need the (-1) to consistency with aviation terminology
            MainActivity.axisRoll = HelperMethods.calculateFilteredAngle((float)Math.toDegrees((double)orientation[2]), MainActivity.axisRoll);  //-roll
        	updateAttitudeText(MainActivity.axisPitch, MainActivity.axisRoll, MainActivity.axisAzimuth);
			if (azimuthList.size() > SENSOR_COLLECTION_LIMIT) {
				azimuthList.remove();  //Removes the head
				pitchList.remove();
				rollList.remove();
			}
            azimuthList.add(MainActivity.axisAzimuth);  //Add is to the tail
			pitchList.add(MainActivity.axisPitch);
			rollList.add(MainActivity.axisRoll);
    	}
    	mRotation = null;
    }

    private boolean gpsEnableChecker() {
    	if ( myLatitiude == 0 && myLongitude == 0 ) {   		
    		HelperMethods.gpsMessage(MainActivity.this);
     		return false;
    	}
    	return true;
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
        sensorManager.unregisterListener(rotationListener);
        releaseCamera();              
    }
  
    @Override
    protected void onPause() {
        super.onPause();			
        releaseCamera(); 
    }

}