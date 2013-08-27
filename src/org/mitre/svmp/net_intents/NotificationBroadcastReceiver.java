/*
 Copyright 2013 The MITRE Corporation, All Rights Reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this work except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package org.mitre.svmp.net_intents;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import org.mitre.svmp.net_intents.R;

import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.protocol.SVMPProtocol.Response;
import org.mitre.svmp.protocol.SVMPProtocol.Response.ResponseType;

import com.google.protobuf.ByteString;

/**
 * @author Colin Courtney
 */
public class NotificationBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = NotificationBroadcastReceiver.class.getName();

    public void onReceive(Context context, Intent intent) 
    {
        if(intent.getAction().equals("org.mitre.svmp.action.BOOT_COMPLETED"))
        {
            Log.d(TAG, "Received SVMP boot intent broadcast");

            // we have to send a handshake to the Netty server on the EventServer so it
            // is aware of our socket channel and can push messages to us
            sendMessage(context, Utility.buildHandshakeResponse());
        }
		else if(intent.getAction().equals("org.mitre.svmp.notify.intercept"))
		{
			try
			{	
				//Build and send the notification protobuf.
		        sendMessage(context,getNotificationData((Notification)intent.getParcelableExtra("notification"),context));
			}catch(Exception e){e.printStackTrace();}
		}
    }
    
    //Pull out whatever info we can from the Notification.
    @SuppressWarnings("unchecked")
	private Response getNotificationData(Notification notification,Context context)
	{
		RemoteViews view = notification.tickerView;
		Class<? extends RemoteViews> secretClass = view.getClass();

		try {
		    Map<Integer, String> text = new HashMap<Integer, String>();

		    Field outerFields[] = secretClass.getDeclaredFields();
		    for (int i = 0; i < outerFields.length; i++) {
		        if (!outerFields[i].getName().equals("mActions")) continue;

		        outerFields[i].setAccessible(true);

		        ArrayList<Object> actions = (ArrayList<Object>) outerFields[i]
		        .get(view);
		        for (Object action : actions) {
		            Field innerFields[] = action.getClass().getDeclaredFields();

		            Object value = null;
		            Integer type = null;
		            Integer viewId = null;
		            for (Field field : innerFields) {
		                field.setAccessible(true);
		                if (field.getName().equals("value")) {
		                    value = field.get(action);
		                } else if (field.getName().equals("type")) {
		                    type = field.getInt(action);
		                } else if (field.getName().equals("viewId")) {
		                    viewId = field.getInt(action);
		                }
		            }

		            if (type == 9 || type == 10) {
		                text.put(viewId, value.toString());
		            }
		        }
		        String title_details =  text.get(16908310),
		        	   info_details = text.get(16909082),
		        	   inner_text_details = text.get(16908358);

		        //Inflate ticker layout from pre-defined xml into a view.
		        LayoutInflater inflater = (LayoutInflater)context.getSystemService
		        	      (Context.LAYOUT_INFLATER_SERVICE);
		        View root = inflater.inflate(R.layout.status_bar_latest_event_ticker,null);
		        
		        //Set the view's various items from the notification data.
		        TextView title = (TextView)root.findViewById(R.id.title),
		        		 info = (TextView)root.findViewById(R.id.info),
		        		 inner_text = (TextView)root.findViewById(R.id.text);
		        title.setText(title_details);
		        info.setText(info_details);
		        inner_text.setText(inner_text_details);
		        
		        //Get image from system resources.
		        Context foreignContext = context.createPackageContext(notification.tickerView.getPackage(),
		        		Context.CONTEXT_IGNORE_SECURITY);
		        Bitmap bitmap = BitmapFactory.decodeResource(foreignContext.getResources(),notification.icon);
				ImageView img = (ImageView)root.findViewById(R.id.icon); 
				img.setImageBitmap(bitmap);
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
				byte[] byteArray = stream.toByteArray();
			    //img.setImageResource(notification.icon); 
			    
			    //Set the re-compiled notification ticker layout view as a toast, and display.
			    Toast toast = new Toast(context);
			    toast.setView(root); 
			    toast.show();
			    
			    //Build the response and return it.
			    return Response.newBuilder()
	        			.setType(ResponseType.NOTIFICATION)
	        			.setNotification(
	        					SVMPProtocol.Notification.newBuilder()
	        					.setContentTitle(title != null?title.getText().toString():"SVMP INTERCEPTED NOTIFICATION")
	        					.setContentText(inner_text != null?inner_text.getText().toString():"No text.")
	        					.setSmallIcon(ByteString.copyFrom(byteArray))
	        					.build()
	        			).build();
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}
		return null;
	}

    // send a Response to the EventServer
    public static void sendMessage(Context context, Response response) {
        // create an intent to send to the BackgroundService
        Intent intent = new Intent(context, BackgroundService.class);

        // pack the Response in the intent as a byte array
        intent.putExtra("responseData", response.toByteArray());

        // start the BackgroundService if it hasn't been started, and send the intent to it
        context.startService(intent);
    }
    
    //Keep this for later just in case:
     				/* Re-create a 'local' view group from the info contained in the remote view
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				ViewGroup localView = (ViewGroup) inflater.inflate(notification.tickerView.getLayoutId(), null);
				notification.tickerView.reapply(context, localView);
				
				//Pull out requisite views.
				TextView title = (TextView)localView.findViewById(R.id.title),
					text    = (TextView)localView.findViewById(R.id.text);
				ImageView icon = (ImageView)localView.findViewById(R.id.icon);*/
				
				/*//Get icon byte array from 'large icon' field in notification.
				byte[] byteArray = new byte[256];
				if(notification.largeIcon != null)
				{
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					notification.largeIcon.compress(Bitmap.CompressFormat.PNG, 100, stream);
					byteArray = stream.toByteArray();
				}*/
				/*//Get icon byte array from ImageView.
				byte[] byteArray = new byte[256];
				Bitmap bitmap = ((BitmapDrawable)icon.getDrawable()).getBitmap();
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
				byteArray = stream.toByteArray();*/

}
