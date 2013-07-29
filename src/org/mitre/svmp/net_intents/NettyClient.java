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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.protocol.SVMPProtocol.Request;
import org.mitre.svmp.protocol.SVMPProtocol.Response;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Colin Courtney
 */
public class NettyClient extends Thread {
    private static final String TAG = NettyClient.class.getName();

    private Context context;
    private String host;
    private int port;
    protected BlockingQueue<Response> sendQueue = new LinkedBlockingQueue<Response>();
    protected BlockingQueue<Request> receiveQueue = new LinkedBlockingQueue<Request>();
    private NettyClientSender sender;
    private boolean interrupt;

    public NettyClient(Context context, int port) {
        this.context = context;
        this.host = context.getString(R.string.send_host);
        this.port = port;
    }

    protected void setInterrupt( boolean interrupt ) {
        this.interrupt = interrupt;
        sender.setInterrupt(interrupt);
    }

    public void run() {
        // Create one EventLoopGroup (resource intensive)
        EventLoopGroup group = new NioEventLoopGroup();

        // Try to send the LocationRequest
        try {
            // create a new Bootstrap
            Bootstrap b = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new NettyClientInitializer(receiveQueue));

            // Make a new Channel (connection)
            Channel ch = b.connect(host, port).sync().channel();

            // Get the handler instance to initiate the request
            NettyClientHandler handler = ch.pipeline().get(NettyClientHandler.class);

            // the sender will watch the Blocking Queue and send a Response when one is added
            sender = new NettyClientSender(sendQueue, handler);
            sender.start();

            while(!interrupt) {
                try {
                    Request request = receiveQueue.take();
                    if( request.getType() == Request.RequestType.INTENT)
                        handleMessage(request);
                } catch( InterruptedException e ) {
                    Log.d(TAG, "NettyClient.run() interrupted exception: " + e.getMessage());
                }
            }

            // Close the connection.
            ch.close();
        } catch( Exception e ) {
            e.printStackTrace();
        } finally {
            // Free resources from EventLoopGroup
            group.shutdownGracefully();
        }
    }

    private void handleMessage(Request request) {
        if( request.hasIntent() ) {
            Log.d(TAG, "Received Intent from EventServer");
            SVMPProtocol.Intent intentRequest = request.getIntent();
            if(intentRequest.getAction().equals(SVMPProtocol.IntentAction.ACTION_VIEW))
            {
            	Intent intent = new Intent(Intent.ACTION_VIEW);
            	intent.setData(Uri.parse(intentRequest.getData()));
            	context.startActivity(intent);
            }
        }
    }

    protected void sendMessage(Response response) {
        Log.d(TAG, "Sending Response to Event Server");
        try {
            sendQueue.put(response);
        } catch( InterruptedException e ) {
            Log.d(TAG, "sendQueue.put() interrupted: " + e.getMessage());
        }
    }
}
