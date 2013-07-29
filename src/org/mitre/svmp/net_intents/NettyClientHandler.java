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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.MessageList;
import org.mitre.svmp.protocol.SVMPProtocol.Request;
import org.mitre.svmp.protocol.SVMPProtocol.Response;
import java.util.concurrent.BlockingQueue;

/**
 * @author Colin Courtney
 */
public class NettyClientHandler extends ChannelInboundHandlerAdapter {
    private static final String TAG = NettyClientHandler.class.getName();

    // Stateful properties
    private volatile Channel channel;
    protected BlockingQueue<Request> receiveQueue;

    public NettyClientHandler(BlockingQueue<Request> receiveQueue) {
        this.receiveQueue = receiveQueue;
    }

    public boolean sendResponse(Response response) {
        boolean success = true;
        try {
            // write the Response to the channel
            channel.write(response);
        } catch( Exception e ) {
            Log.d(TAG, "sendResponse failed: " + e.getMessage());
            success = false;
        }

        return success;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageList<Object> msgs) throws Exception {
        for (int i = 0; i < msgs.size(); i++) {
            receiveQueue.add((Request) msgs.get(i));
        }
        msgs.recycle();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Log.d(TAG, "Unexpected exception from downstream.", cause);
        ctx.close();
    }
}
