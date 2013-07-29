/*
 * Copyright 2013 The MITRE Corporation, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this work except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mitre.svmp.net_intents;

import android.util.Log;
import org.mitre.svmp.protocol.SVMPProtocol.Response;
import java.util.concurrent.BlockingQueue;

/**
 * @author Colin Courtney
 */
public class NettyClientSender extends Thread {
    private static final String TAG = NettyClientSender.class.getName();

    private BlockingQueue<Response> sendQueue;
    private NettyClientHandler handler;
    private boolean interrupt = false;

    public NettyClientSender(BlockingQueue<Response> sendQueue, NettyClientHandler handler) {
        this.sendQueue = sendQueue;
        this.handler = handler;
    }

    public void run() {
        while( !interrupt) {
            try {
                // wait and take the next available response from the send queue
                Response response = sendQueue.take();
                if( response != null )
                    handler.sendResponse(response);
            } catch( InterruptedException e ) {
                Log.d(TAG, "NettyClientSender.sendQueue.take() interrupted exception: " + e.getMessage());
            }
        }
    }

    public void setInterrupt(boolean interrupt) {
        this.interrupt = interrupt;
    }
}
