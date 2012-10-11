package com.codedemigod.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.IBinder;
import android.os.PatternMatcher;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.telephony.IExtendedNetworkService;
import com.codedemigod.ussdinterceptor.R;

public class CDUSSDService extends Service{

	    private String TAG = CDUSSDService.class.getSimpleName();
	    private boolean mActive = false;  //we will only activate this "USSD listener" when we want it
	    
	    BroadcastReceiver receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals(Intent.ACTION_INSERT)){
					//activity wishes to listen to USSD returns, so activate this
					mActive = true;
					Log.d(TAG, "activate ussd listener");
				}
				else if(intent.getAction().equals(Intent.ACTION_DELETE)){
					mActive = false;
					Log.d(TAG, "deactivate ussd listener");
				}
			}
		};
	    
		private final IExtendedNetworkService.Stub mBinder = new IExtendedNetworkService.Stub () {
			public void clearMmiString() throws RemoteException {
				Log.d(TAG, "called clear");
			}

			public void setMmiString(String number) throws RemoteException {
				Log.d (TAG, "setMmiString:" + number);
			}

			public CharSequence getMmiRunningText() throws RemoteException {
				if(mActive == true){
					return null;
				}
				
				return "USSD Running";
			}

			public CharSequence getUserMessage(CharSequence text)
					throws RemoteException {
				Log.d(TAG, "get user messagedss " + text);
				
				if(mActive == false){
					//listener is still inactive, so return whatever we got
					Log.d(TAG, "inactive " + text);
					return text;
				}
				
				//listener is active, so broadcast data and suppress it from default behavior
				
				//build data to send with intent for activity, format URI as per RFC 2396
				Uri ussdDataUri = new Uri.Builder()
				.scheme(getBaseContext().getString(R.string.uri_scheme))
				.authority(getBaseContext().getString(R.string.uri_authority))
				.path(getBaseContext().getString(R.string.uri_path))
				.appendQueryParameter(getBaseContext().getString(R.string.uri_param_name), text.toString())
				.build();
				
				sendBroadcast(new Intent(Intent.ACTION_GET_CONTENT, ussdDataUri));
				
				mActive = false;
				return null;
			}
		};

	    @Override
	    public IBinder onBind(Intent intent) {
	    	Log.i(TAG, "called onbind");
	    	
	    	//the insert/delete intents will be fired by activity to activate/deactivate listener since service cannot be stopped
	    	IntentFilter filter = new IntentFilter();
	    	filter.addAction(Intent.ACTION_INSERT);
	    	filter.addAction(Intent.ACTION_DELETE);
	    	filter.addDataScheme(getBaseContext().getString(R.string.uri_scheme));
	    	filter.addDataAuthority(getBaseContext().getString(R.string.uri_authority), null);
	    	filter.addDataPath(getBaseContext().getString(R.string.uri_path), PatternMatcher.PATTERN_LITERAL);
			registerReceiver(receiver, filter);
	    	
	        return mBinder;
	    }	
}
