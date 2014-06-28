package placer.general;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import placer.android.MainActivity;
import placer.custom.LocationObject;
import placer.custom.OrientationObject;
import placer.geodesy.Ellipsoid;
import placer.geodesy.GeodeticCalculator;
import placer.geodesy.GlobalCoordinates;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.util.Log;

public class LoadingStart extends AsyncTask<Void, Integer, Void> {
	
	public static final String TAG = null;
	public static final int LOS_FULL_LENGTH = 1000;	//In meters
	public static final int LOS_INTERVAL	= 50;  	//In meters, full LOS consists of LOS_FULL_LENGTH/LOS_INTERVAL intervals. for each interval will be elevation call.
	private ProgressDialog progressDialogSearch;
	private Context context;
    //Connecting DB
    public Connection connection = null;
	public static Statement statement = null;
	public static ResultSet resultSet = null;
	//Logic
	public static ArrayList<LocationObject> losList = new ArrayList<LocationObject>();
	public static String resultTxt = "";
	//GIS
	GeodeticCalculator geoCalc = new GeodeticCalculator();
	Ellipsoid reference = Ellipsoid.WGS84;
	
	public LoadingStart(Context cnt) {
		this.context = cnt;
		progressDialogSearch = new ProgressDialog(context);
	}
	
	@Override  
    protected void onPreExecute() { 
		try {
			Class.forName("org.postgresql.Driver");
			connection = DriverManager.getConnection("jdbc:postgresql://postgresql-db1.cp3lk1mrandp.us-west-2.rds.amazonaws.com:5432/dbname", "michael", "jankyur2");
			progressDialogSearch.setProgressStyle(1);
			progressDialogSearch.setTitle("Let`s Find Out");
			progressDialogSearch.setMessage("Exploring...");
			progressDialogSearch.setProgress(0);
			progressDialogSearch.setCancelable(true);
			progressDialogSearch.show();
		}
		catch (ClassNotFoundException e) {Log.e(TAG,"###ClassNotFoundException");}
		catch (SQLException e) {}
    }  
	
	@Override  
    protected Void doInBackground(Void... params) {    
		synchronized (this) {										  			  													    			
			craeteLOS(MainActivity.locationObject, MainActivity.orientationObject);
        	publishProgress(33);            	 
        	retreiveGISData(losList);
       		publishProgress(66); 
       		resultTxt = figureOutResult();	
       		publishProgress(100);
        }  
        return null;  
	}
	


	@Override  
	protected void onProgressUpdate(Integer... values) {                
		progressDialogSearch.setProgress(values[0]);				//set the current progress of the progress dialog 
	}
	 
	@Override  
	protected void onPostExecute(Void result) {   
		progressDialogSearch.dismiss();  
		Vibrator vibrate = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		vibrate.vibrate(500);
		//Show message
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);	
     	 	alertDialogBuilder.setTitle("Results Ready!");
     	 	alertDialogBuilder
     	 	.setMessage("You are looking at "+resultTxt)
     	 	.setCancelable(false)
     	 	.setPositiveButton("Google it",new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) {
					//Nothing
				}
     	 	})
     	 	.setNegativeButton("Close",new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) { 				
					//Nothing
				}
     	 	});
     	 AlertDialog alertDialog = alertDialogBuilder.create();
     	 alertDialog.show();
	}
	
	private void craeteLOS(LocationObject location, OrientationObject orientation) {
		double[] endBearing = new double[1];
		GlobalCoordinates tmpLocation;
		double tmpElevation;
		GlobalCoordinates startLocation = new GlobalCoordinates(location.getLatitude(), location.getLongitude());
		for (int i=0; i<(LOS_FULL_LENGTH/LOS_INTERVAL); i++) {
			tmpLocation = geoCalc.calculateEndingGlobalCoordinates(reference, startLocation, orientation.getAzimuth(), LOS_INTERVAL*(i+1), endBearing);
			tmpElevation = 100; //TODO: Call google elevation API
			losList.add(new LocationObject(tmpLocation.getLatitude(), tmpLocation.getLongitude(), tmpElevation));
		}
	}
	
	private void retreiveGISData(ArrayList<LocationObject> losList2) {
		/*
		if (connection != null) {
			statement = connection.createStatement();
			resultSet = statement.executeQuery("SELECT * FROM buildings WHERE gid = 500");
			while (resultSet.next()) {
			   Log.e(TAG,resultSet.getString(3)+", "+resultSet.getString(4));
			} 
			resultSet.close();
			statement.close();
			connection.close();
		}
		*/
	}
	
	private String figureOutResult() {

		return null;
	}
	 
}