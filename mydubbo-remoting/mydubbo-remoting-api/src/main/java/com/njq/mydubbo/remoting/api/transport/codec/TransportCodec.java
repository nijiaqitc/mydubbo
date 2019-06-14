/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.njq.mydubbo.remoting.api.transport.codec;

import com.njq.mydubbo.common.utils.StringUtils;
import com.njq.mydubbo.remoting.api.Channel;
import com.njq.mydubbo.remoting.api.buffer.ChannelBuffer;
import com.njq.mydubbo.remoting.api.buffer.ChannelBufferInputStream;
import com.njq.mydubbo.remoting.api.buffer.ChannelBufferOutputStream;
import com.njq.mydubbo.remoting.api.transport.AbstractCodec;
import com.njq.mydubbo.serialization.api.Cleanable;
import com.njq.mydubbo.serialization.api.ObjectInput;
import com.njq.mydubbo.serialization.api.ObjectOutput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * TransportCodec
 */
public class TransportCodec extends AbstractCodec {

    @Override
    public void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException {
        OutputStream output = new ChannelBufferOutputStream(buffer);
        ObjectOutput objectOutput = getSerialization(channel).serialize(channel.getUrl(), output);
        encodeData(channel, objectOutput, message);
        objectOutput.flushBuffer();
        if (objectOutput instanceof Cleanable) {
            ((Cleanable) objectOutput).cleanup();
        }
    }

    @Override
    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {
        InputStream input = new ChannelBufferInputStream(buffer);
        ObjectInput objectInput = getSerialization(channel).deserialize(channel.getUrl(), input);
        Object object = decodeData(channel, objectInput);
        if (objectInput instanceof Cleanable) {
            ((Cleanable) objectInput).cleanup();
        }
        return object;
    }

    protected void encodeData(Channel channel, ObjectOutput output, Object message) throws IOException {
        encodeData(output, message);
    }

    protected Object decodeData(Channel channel, ObjectInput input) throws IOException {
        return decodeData(input);
    }

    protected void encodeData(ObjectOutput output, Object message) throws IOException {
        output.writeObject(message);
    }

    protected Object decodeData(ObjectInput input) throws IOException {
        try {
            return input.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("ClassNotFoundException: " + StringUtils.toString(e));
        }
    }
}
