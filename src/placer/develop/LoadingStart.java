package placer.develop;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Vibrator;

public class LoadingStart extends AsyncTask<Void, Integer, Void> {
	
	private ProgressDialog progressDialogSearch;
	private Context context;
	
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
       		 	//TODO: add real job
        		publishProgress(25);            	 
       		 	this.wait(1000);
       		 	publishProgress(50); 
       		 	this.wait(1000);
       		 	publishProgress(75);
       		 	this.wait(1000);	
       		 	publishProgress(100);
       		 	this.wait(500);
       	 	} 
        	catch (InterruptedException e) {
       	 		e.printStackTrace();
       	   	}
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
