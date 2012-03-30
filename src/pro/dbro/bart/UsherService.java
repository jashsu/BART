package pro.dbro.bart;

import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class UsherService extends Service {
    private NotificationManager mNM;
    
    private PendingIntent contentIntent;
    
    private Context c;
    
    private Notification notification; // keep an instance of the notification to update time text
    
    private int currentLeg; // keep track of which leg of the route we're currently on
    private boolean didBoard; // keep track of whether we've boarded the current leg, or are waiting for it to arrive
    private CountDownTimer timer; // keep track of current countdown for cancelling if new request comes
    							  // else we can get errors related to a timer expecting previous route

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.local_service_started;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        UsherService getService() {
            return UsherService.this;
        }
    }
    
    @Override
    public void onCreate() {
    	c = this;
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // Display a notification about us starting.  We put an icon in the status bar.
        
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("UsherService", "Received start id " + startId + ": " + intent);
        if(timer != null)
        	timer.cancel();
        //departureStation = ((leg)usherRoute.legs.get(0)).boardStation;

        /*
        if (intent !=null && intent.getExtras()!=null){
             //departureStation = intent.getExtras().getString("departure");
        }
        */
        
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        showNotification();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        // Tell the user we stopped.
        //Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        //CharSequence text = getText(R.string.local_service_started);
    	route usherRoute = TheActivity.usherRoute;
    
    	String destinationStation = ((leg)usherRoute.legs.get(usherRoute.legs.size()-1)).disembarkStation;
    	currentLeg = 0;
    	didBoard = false;
    	CharSequence text = "Guiding to " + TheActivity.REVERSE_STATION_MAP.get(destinationStation.toLowerCase());
        // Set the icon, scrolling text and timestamp
        notification = new Notification(R.drawable.ic_launcher, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, TheActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        Date now = new Date();
        long minutesUntilNext = ((((leg)usherRoute.legs.get(0)).boardTime.getTime() - now.getTime()));
        //minutesUntilNext is, for this brief moment, actually milliseconds. 
        makeLegCountdownTimer(minutesUntilNext);
        // back to minutes
        minutesUntilNext = minutesUntilNext/(1000*60);
        
        CharSequence currentStationText = "At " + TheActivity.REVERSE_STATION_MAP.get(((leg)usherRoute.legs.get(0)).boardStation.toLowerCase());
        CharSequence nextStep = "Board "+ TheActivity.REVERSE_STATION_MAP.get(((leg)usherRoute.legs.get(0)).trainHeadStation.toLowerCase()) + " train in " + String.valueOf(minutesUntilNext) + "m";
        /*
        if(usherRoute.legs.size()>1){
        	nextStep = "Transfer at " + TheActivity.REVERSE_STATION_MAP.get(((leg)usherRoute.legs.get(0)).disembarkStation.toLowerCase()) + " in "+((leg)usherRoute.legs.get(0)).disembarkTime.toString();
        }
        else{
        	nextStep = "Arriving at " + TheActivity.REVERSE_STATION_MAP.get(((leg)usherRoute.legs.get(0)).disembarkStation.toLowerCase()) + " at "+ ((leg)usherRoute.legs.get(0)).disembarkTime.toString();
        }
        */
        notification.setLatestEventInfo(this, currentStationText,
        		nextStep, contentIntent);

        // Send the notification.

        mNM.notify(NOTIFICATION, notification);
    }
    
    private void updateNotification(){
    	route usherRoute = TheActivity.usherRoute;
    	Date now = new Date();
    	CharSequence nextStep ="";
    	if(didBoard){
    		long minutesUntilNext = ((((leg)usherRoute.legs.get(currentLeg)).disembarkTime.getTime() - now.getTime())/(1000*60));
    		if(currentLeg == usherRoute.legs.size())
    		nextStep = "Get off at "+ TheActivity.REVERSE_STATION_MAP.get(((leg)usherRoute.legs.get(currentLeg)).disembarkStation.toLowerCase()) + " train in " + String.valueOf(minutesUntilNext) + "m";
    	}
    	else{
    		long minutesUntilNext = ((((leg)usherRoute.legs.get(currentLeg)).boardTime.getTime() - now.getTime())/(1000*60));
    		nextStep = "Board "+ TheActivity.REVERSE_STATION_MAP.get(((leg)usherRoute.legs.get(currentLeg)).trainHeadStation.toLowerCase()) + " train in " + String.valueOf(minutesUntilNext) + "m";
    	}
        CharSequence currentStationText = "At " + TheActivity.REVERSE_STATION_MAP.get(((leg)usherRoute.legs.get(currentLeg)).boardStation.toLowerCase());
        
        /*
        if(usherRoute.legs.size()>1){
        	nextStep = "Transfer at " + TheActivity.REVERSE_STATION_MAP.get(((leg)usherRoute.legs.get(0)).disembarkStation.toLowerCase()) + " in "+((leg)usherRoute.legs.get(0)).disembarkTime.toString();
        }
        else{
        	nextStep = "Arriving at " + TheActivity.REVERSE_STATION_MAP.get(((leg)usherRoute.legs.get(0)).disembarkStation.toLowerCase()) + " at "+ ((leg)usherRoute.legs.get(0)).disembarkTime.toString();
        }
        */

        notification.setLatestEventInfo(this, currentStationText,
        		nextStep, contentIntent);
        mNM.notify(NOTIFICATION, notification);
    }
    
    private void makeLegCountdownTimer(long msUntilNext){
    	timer = new CountDownTimer(msUntilNext, 60000){
            //new CountDownTimer(5000, 1000){

    			@Override
    			public void onFinish() {
    				// TODO Auto-generated method stub
    				Vibrator v = (Vibrator) getSystemService(c.VIBRATOR_SERVICE);
    				long[] vPattern = {0,200,100,200,50,100,50,100};
    				v.vibrate(vPattern,-1);
    				//if(didBoard) // if we've boarded, we're handling the last leg
    				//	currentLeg ++;
    				didBoard = !didBoard;
    				
    				if ((TheActivity.usherRoute.legs.size() == currentLeg+1) && !didBoard){
    					notification = new Notification(R.drawable.ic_launcher, "This is your stop! Take Care!",
    			                System.currentTimeMillis());
    					notification.setLatestEventInfo(c, "You're here",
    			        		"Take it easy", contentIntent);
    			        mNM.notify(NOTIFICATION, notification);
    					onDestroy(); // Is this the proper way to suicide a service?
    				}
    				else if(didBoard){ //Set timer for this leg's disembark time
    					Date now = new Date();
    			        long msUntilNext = ((((leg)TheActivity.usherRoute.legs.get(currentLeg)).disembarkTime.getTime() - now.getTime()));
    					makeLegCountdownTimer(msUntilNext);
    					updateNotification();
    				}
    				else{ // Set timer for next leg's board time
    					currentLeg ++;
    					Date now = new Date();
    			        long msUntilNext = ((((leg)TheActivity.usherRoute.legs.get(currentLeg)).boardTime.getTime() - now.getTime()));
    					makeLegCountdownTimer(msUntilNext);
    					updateNotification();
    						
    						
    				}
    				
    			}

    			@Override
    			public void onTick(long arg0) {
    				updateNotification();				
    			}
            	
            }.start();
    }
}