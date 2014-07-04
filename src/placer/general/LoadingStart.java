package placer.general;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

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
	public static final int LOS_FULL_LENGTH = 300;	//In meters
	public static final int LOS_INTERVAL	= 300;  //In meters, full LOS consists of LOS_FULL_LENGTH/LOS_INTERVAL intervals. for each interval will be elevation call.
	private ProgressDialog progressDialogSearch;
	private Context context;
    //Connecting DB
    public Connection connection = null;
	public static Statement statement = null;
	public static ResultSet resultSet = null;
	//Logic
	public static ArrayList<LocationObject> losList = new ArrayList<LocationObject>();  //The first element in the list is MY CURRENT LOCATION!
	public static String resultTxt = "";
	public static String SQL = "";
	//GIS
	GeodeticCalculator geoCalc = new GeodeticCalculator();
	Ellipsoid reference = Ellipsoid.WGS84;
	
	public LoadingStart(Context cnt) {
		this.context = cnt;
		progressDialogSearch = new ProgressDialog(context);
	}
	
	@Override  
    protected void onPreExecute() { 
		progressDialogSearch.setProgressStyle(1);
		progressDialogSearch.setTitle("Let`s Find Out");
		progressDialogSearch.setMessage("Exploring...");
		progressDialogSearch.setProgress(0);
		progressDialogSearch.setCancelable(true);
		progressDialogSearch.show();
    }  
	
	@Override  
    protected Void doInBackground(Void... params) {    
		synchronized (this) {
			/**
			 * This logs are for test
			 */
			craeteLOS(MainActivity.locationObject, MainActivity.orientationObject);
			Iterator<LocationObject> itr = losList.iterator();
			while (itr.hasNext()) {
				LocationObject tmp = (LocationObject) itr.next();
				Log.e(TAG,"###los list lat: "+tmp.getLatitude()+", lon: "+tmp.getLongitude());
				Log.e(TAG,"###LOS_FULL_LENGTH/LOS_INTERVAL "+losList.get(LOS_FULL_LENGTH/LOS_INTERVAL).getLongitude()+" "+losList.get(LOS_FULL_LENGTH/LOS_INTERVAL).getLatitude());
			}
        	publishProgress(33);            	 
        	retreiveGISData(losList);  //Will call figureOutResult() & publishProgress...
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
     	 	.setMessage("You are looking at: \n"+resultTxt)
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
		losList.add(location);  //The first element in the list is MY CURRENT LOCATION!
		for (int i=0; i<(LOS_FULL_LENGTH/LOS_INTERVAL); i++) {
			tmpLocation = geoCalc.calculateEndingGlobalCoordinates(reference, startLocation, orientation.getAzimuth(), LOS_INTERVAL*(i+1), endBearing);
			tmpElevation = 100; //TODO: Call google elevation API
			losList.add(new LocationObject(tmpLocation.getLatitude(), tmpLocation.getLongitude(), tmpElevation, LOS_INTERVAL*(i+1)*Math.tan(orientation.getAzimuth())));  //The 1.5m is added automatically
		}
	}
	
	private void retreiveGISData(ArrayList<LocationObject> losList2) {
		try {
			Class.forName("org.postgresql.Driver");
			connection = DriverManager.getConnection("jdbc:postgresql://postgresql-db1.cp3lk1mrandp.us-west-2.rds.amazonaws.com:5432/dbname", "michael", "jankyur2");
			if (connection != null) {
				publishProgress(66);
				statement = connection.createStatement();
				SQL = "SELECT osm_id, gid, name, type, ST_Distance_Spheroid(ST_Centroid(buildings.geom), ST_GeomFromText('POINT("+losList.get(0).getLongitude()+" "+losList.get(0).getLatitude()+")',4326), 'SPHEROID[\"WGS 84\",6378137,298.257223563]') AS distance_meters"
					+ " FROM buildings"
					+ " WHERE ST_Intersects(buildings.geom, ST_GeomFromText('LINESTRING("+losList.get(0).getLongitude()+" "+losList.get(0).getLatitude()+", "+losList.get(LOS_FULL_LENGTH/LOS_INTERVAL).getLongitude()+" "+losList.get(LOS_FULL_LENGTH/LOS_INTERVAL).getLatitude()+")',4326))"
					+ " ORDER BY distance_meters ASC";
				resultSet = statement.executeQuery(SQL);
				/**
				 * This is the most simplest algorithm - returns the first object...
				 */
				int counter = 0;
				while (resultSet.next()) {
					/**
					 * This log is for test
					 */
					Log.e(TAG,"###resultSet "+resultSet.getString(1)+", "+resultSet.getString(2)+", "+resultSet.getString(3)+", "+resultSet.getString(4));
					if (counter == 0) {
						if (resultSet.getString(3) != null) {
							if (resultSet.getString(4) != null) {
								resultTxt = resultSet.getString(3)+"\nWhich is: \n"+resultSet.getString(4);
							}
							else {
								resultTxt = resultSet.getString(3);
							}
						}
						else {
							resultTxt = "Building unique Map id is "+resultSet.getString(2);
						}
					}
					counter++;
				}
				publishProgress(100); 
				resultSet.close();
				statement.close();
				connection.close();
				MainActivity.pitchList.clear();
				MainActivity.rollList.clear();
				MainActivity.azimuthList.clear();
				losList.clear();
			}
		}
		catch (SQLException e) {}
		catch (ClassNotFoundException e) {Log.e(TAG,"###ClassNotFoundException");}
	}
	 
}