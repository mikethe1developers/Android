package placer.develop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.util.Log;

public class LoadingStart extends AsyncTask<Void, Integer, Void> {
	
	public static final String TAG = null;
	private ProgressDialog progressDialogSearch;
	private Context context;
    //Connecting DB
    public Connection connection = null;
	public static Statement statement = null;
	public static ResultSet resultSet = null;
	
	LoadingStart(Context cnt) {
		this.context = cnt;
		progressDialogSearch = new ProgressDialog(context);
	}
	
	@Override  
    protected void onPreExecute() {    
		progressDialogSearch = ProgressDialog.show(context,"Let`s Find Out", "Exploring...", false, false);
    }  
	
	@Override  
    protected Void doInBackground(Void... params) {    
        synchronized (this) {										//Get the current thread's token  			 
        	try {  													//Do job and publish %
    			Class.forName("org.postgresql.Driver");	
    			connection = DriverManager.getConnection("jdbc:postgresql://postgresql-db1.cp3lk1mrandp.us-west-2.rds.amazonaws.com:5432/dbname", "michael", "jankyur2");
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
        		publishProgress(25);            	 
       		 	this.wait(1000);
       		 	publishProgress(50); 
       		 	this.wait(1000);
       		 	publishProgress(75);
       		 	this.wait(1000);	
       		 	publishProgress(100);
       		 	this.wait(500);
       	 	}
    		catch (ClassNotFoundException e) {Log.e(TAG,"###ClassNotFoundException");}
    		catch (SQLException e) {}
        	catch (InterruptedException e) {}
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
     	 	.setMessage("You are looking at Placer")
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
}
