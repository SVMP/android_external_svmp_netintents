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

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.protocol.SVMPProtocol.Response;
import org.mitre.svmp.protocol.SVMPProtocol.Response.ResponseType;

/**
 * @author Colin Courtney
 */
public class NotificationBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = NotificationBroadcastReceiver.class.getName();

    public void onReceive(Context context, Intent intent) 
    {
        if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) 
        {
            Log.d(TAG, "Received system boot intent broadcast");

            // we have to send a handshake to the Netty server on the EventServer so it
            // is aware of our socket channel and can push messages to us
            sendMessage(context, Utility.buildHandshakeResponse());
        }
		else if(intent.getAction().equals("org.mitre.svmp.notify.intercept"))
		{
			Notification notification = intent.getParcelableExtra("notification");
	        Response response = Response.newBuilder()
	        			.setType(ResponseType.NOTIFICATION)
	        			.setNotification(
	        					SVMPProtocol.Notification.newBuilder()
	        					.setContentTitle("SVMP INTERCEPTED NOTIFICATION")
	        					.setContentText(notification.tickerText.toString())
	        					.build()
	        			).build();
	        BackgroundService.sendMessage(response);
		}
    }

    // send a Response to the EventServer
    private void sendMessage(Context context, Response response) {
        // create an intent to send to the BackgroundService
        Intent intent = new Intent(context, BackgroundService.class);

        // pack the Response in the intent as a byte array
        intent.putExtra("responseData", response.toByteArray());

        // start the BackgroundService if it hasn't been started, and send the intent to it
        context.startService(intent);
    }
}
