//
// MessagePack for Java
//
// Copyright (C) 2009 - 2013 FURUHASHI Sadayuki
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package com.ipd.org.msgpack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import com.ipd.org.msgpack.template.Template;
import com.ipd.org.msgpack.template.TemplateRegistry;
import com.ipd.org.msgpack.type.Value;
import com.ipd.org.msgpack.unpacker.Converter;
import com.ipd.org.msgpack.unpacker.MessagePackBufferUnpacker;
import com.ipd.org.msgpack.unpacker.MessagePackUnpacker;
import com.ipd.org.msgpack.unpacker.Unpacker;
import com.ipd.org.msgpack.packer.BufferPacker;
import com.ipd.org.msgpack.packer.MessagePackBufferPacker;
import com.ipd.org.msgpack.packer.MessagePackPacker;
import com.ipd.org.msgpack.packer.Packer;
import com.ipd.org.msgpack.packer.Unconverter;
import com.ipd.org.msgpack.unpacker.BufferUnpacker;

/**
 * <p>
 * This is basic class to use MessagePack for Java. It creates serializers and
 * deserializers for objects of classes.
 * </p>
 * 
 * <p>
 * See <a
 * href="http://wiki.msgpack.org/display/MSGPACK/QuickStart+for+Java">Quick
 * Start for Java</a> on MessagePack wiki.
 * </p>
 * 
 */
public class MessagePack {
    private TemplateRegistry registry;
    
    private static final int POOL_SIZE = 500;

    private LinkedBlockingQueue<BufferPacker> packerPool = new LinkedBlockingQueue<BufferPacker>(POOL_SIZE);;
    
    private LinkedBlockingQueue<BufferUnpacker> unpackerPool = new LinkedBlockingQueue<BufferUnpacker>(POOL_SIZE);
    
    /**
     * 
     * @since 0.6.0
     */
    public MessagePack() {
        registry = new TemplateRegistry(null);
    }
    

    /**
     * 
     * @since 0.6.0
     * @param msgpack
     */
//    public MessagePack(MessagePack msgpack) {
//        registry = new TemplateRegistry(msgpack.registry);
//        this.poolSize = Runtime.getRuntime().availableProcessors() * 2;
//    }

    protected MessagePack(TemplateRegistry registry) {
        this.registry = registry;
    }
    
    /**
     * 
     * @since 0.6.0
     * @param cl
     */
    public void setClassLoader(final ClassLoader cl) {
        registry.setClassLoader(cl);
    }

    /**
     * Returns serializer that enables serializing objects into
     * {@link java.io.OutputStream} object.
     * 
     * @since 0.6.0
     * @param out
     *            output stream
     * @return stream-based serializer
     */
    public Packer createPacker(OutputStream out) {
        return new MessagePackPacker(this, out);
    }

    /**
     * Returns serializer that enables serializing objects into buffer.
     * 
     * @since 0.6.0
     * @return buffer-based serializer
     */
    public BufferPacker createBufferPacker() {//FIXME add cache
    	BufferPacker bufferPacker = packerPool.poll();
    	if(bufferPacker == null){
    		bufferPacker = new MessagePackBufferPacker(this);
    	}
        return bufferPacker;
    }

    /**
     * Returns serializer that enables serializing objects into buffer.
     * 
     * @since 0.6.0
     * @param bufferSize
     *            initial size of buffer
     * @return buffer-based serializer
     */
    public BufferPacker createBufferPacker(int bufferSize) {
        return new MessagePackBufferPacker(this, bufferSize);
    }

    /**
     * Returns deserializer that enables deserializing
     * {@link java.io.InputStream} object.
     * 
     * @since 0.6.0
     * @param in
     *            input stream
     * @return stream-based deserializer
     */
    public Unpacker createUnpacker(InputStream in) {
        return new MessagePackUnpacker(this, in);
    }

    /**
     * Returns empty deserializer that enables deserializing buffer.
     * 
     * @since 0.6.0
     * @return buffer-based deserializer
     */
    public BufferUnpacker createBufferUnpacker() {//FIXME add cache
    	BufferUnpacker bufferUnpacker = unpackerPool.poll();
    	if(bufferUnpacker == null){
    		bufferUnpacker = new MessagePackBufferUnpacker(this);
    	}
        return bufferUnpacker;
    }

    /**
     * Returns deserializer that enables deserializing buffer.
     * 
     * @since 0.6.0
     * @param bytes
     *            input byte array
     * @return buffer-based deserializer
     */
    public BufferUnpacker createBufferUnpacker(byte[] bytes) {
        return createBufferUnpacker().wrap(bytes);
    }

    /**
     * Returns deserializer that enables deserializing buffer.
     * 
     * @since 0.6.0
     * @param bytes
     * @param off
     * @param len
     * @return buffer-based deserializer
     */
    public BufferUnpacker createBufferUnpacker(byte[] bytes, int off, int len) {
        return createBufferUnpacker().wrap(bytes, off, len);
    }

    /**
     * Returns deserializer that enables deserializing buffer.
     * 
     * @since 0.6.0
     * @param buffer
     *            input {@link java.nio.ByteBuffer} object
     * @return buffer-based deserializer
     */
    public BufferUnpacker createBufferUnpacker(ByteBuffer buffer) {
        return createBufferUnpacker().wrap(buffer);
    }

    /**
     * Serializes specified object.
     * 
     * @since 0.6.0
     * @param v
     *            serialized object
     * @return output byte array
     * @throws IOException
     */
    public <T> byte[] write(T v) throws IOException {
        BufferPacker pk = createBufferPacker();
        byte[] data;
        try {
	        if (v == null) {
	            pk.writeNil();
	        } else {
	            @SuppressWarnings("unchecked")
                Template<T> tmpl = registry.lookup(v.getClass());
	            tmpl.write(pk, v);
	        }
	        data = pk.toByteArray();
        } finally {
        	pk.clear();
        	this.packerPool.offer(pk);
        }
        return data;
    }

    /**
     * Serializes specified object. It allows serializing object by specified
     * template.
     * 
     * @since 0.6.0
     * @param v
     * @param template
     * @return
     * @throws IOException
     */
    public <T> byte[] write(T v, Template<T> template) throws IOException {
        BufferPacker pk = createBufferPacker();
        byte data[];
        try {
        	template.write(pk, v);
        	data = pk.toByteArray();
        } finally {
        	pk.clear();
        	this.packerPool.offer(pk);
        }
        return data;
    }

    /**
     * Serializes specified object to output stream.
     * 
     * @since 0.6.0
     * @param out
     *            output stream
     * @param v
     *            serialized object
     * @throws IOException
     */
    public <T> void write(OutputStream out, T v) throws IOException {
        Packer pk = createPacker(out);
        if (v == null) {
            pk.writeNil();
        } else {
            @SuppressWarnings("unchecked")
            Template<T> tmpl = registry.lookup(v.getClass());
            tmpl.write(pk, v);
        }
    }

    /**
     * Serializes object to output stream by specified template.
     * 
     * @since 0.6.0
     * @param out
     *            output stream
     * @param v
     *            serialized object
     * @param template
     *            serializer/deserializer for the object
     * @throws IOException
     */
    public <T> void write(OutputStream out, T v, Template<T> template)
            throws IOException {
        Packer pk = createPacker(out);
        template.write(pk, v);
    }

    /**
     * Serializes {@link Value} object to byte array.
     * 
     * @since 0.6.0
     * @param v
     *            serialized {@link Value} object
     * @return output byte array
     * @throws IOException
     */
    public byte[] write(Value v) throws IOException {
        // FIXME ValueTemplate should do this
        BufferPacker pk = createBufferPacker();
        byte data[];
        try {
        	pk.write(v);
        	data = pk.toByteArray();
        } finally {
        	pk.clear();
        	this.packerPool.offer(pk);
        }
        return data;
    }

    /**
     * Deserializes specified byte array to {@link Value}
     * object.
     * 
     * @since 0.6.0
     * @param bytes
     *            input byte array
     * @return
     * @throws IOException
     */
    public Value read(byte[] bytes) throws IOException {
        return read(bytes, 0, bytes.length);
    }

    /**
     * Deserializes byte array to {@link Value} object.
     * 
     * @since 0.6.0
     * @param bytes
     * @param off
     * @param len
     * @return
     * @throws IOException
     */
    public Value read(byte[] bytes, int off, int len) throws IOException {
    	BufferUnpacker bufferUnpacker = createBufferUnpacker(bytes, off, len);
    	Value value;
    	try {
    		value = bufferUnpacker.readValue();
    	} finally {
    		bufferUnpacker.cachingClear();
    		this.unpackerPool.offer(bufferUnpacker);
    	}
        return value;
    }

    /**
     * Deserializes {@link java.nio.ByteBuffer} object to
     * {@link Value} object.
     * 
     * @since 0.6.0
     * @param buffer
     *            input buffer
     * @return
     * @throws IOException
     */
    public Value read(ByteBuffer buffer) throws IOException {
    	BufferUnpacker bufferUnpacker = createBufferUnpacker(buffer);
    	Value value;
    	try {
    		value = bufferUnpacker.readValue();
    	} finally {
    		bufferUnpacker.cachingClear();
    		this.unpackerPool.offer(bufferUnpacker);
    	}
        return value;
    }

    /**
     * Deserializes input stream to {@link Value} object.
     * 
     * @since 0.6.0
     * @param in
     *            input stream
     * @return deserialized {@link Value} object
     * @throws IOException
     */
    public Value read(InputStream in) throws IOException {
        return createUnpacker(in).readValue();
    }

    /**
     * Deserializes byte array to object.
     * 
     * @since 0.6.0
     * @param bytes
     *            input byte array
     * @param v
     * @return
     * @throws IOException
     */
    public <T> T read(byte[] bytes, T v) throws IOException {
        @SuppressWarnings("unchecked")
        Template<T> tmpl = registry.lookup(v.getClass());
        return read(bytes, v, tmpl);
    }

    /**
     * Deserializes byte array to object according to template.
     * 
     * @since 0.6.0
     * @param bytes
     *            input byte array
     * @param tmpl
     *            template
     * @return
     * @throws IOException
     */
    public <T> T read(byte[] bytes, Template<T> tmpl) throws IOException {
        return read(bytes, null, tmpl);
    }

    /**
     * Deserializes byte array to object of specified class.
     * 
     * @since 0.6.0
     * @param bytes
     *            input byte array
     * @param c
     * @return
     * @throws IOException
     */
    public <T> T read(byte[] bytes, Class<T> c) throws IOException {
        @SuppressWarnings("unchecked")
        Template<T> tmpl = registry.lookup(c);
        return read(bytes, null, tmpl);
    }

    /**
     * Deserializes byte array to object according to specified template.
     * 
     * @since 0.6.0
     * @param bytes
     *            input byte array
     * @param v
     * @param tmpl
     *            template
     * @return
     * @throws IOException
     */
    public <T> T read(byte[] bytes, T v, Template<T> tmpl) throws IOException {
        BufferUnpacker u = createBufferUnpacker(bytes);
        T result;
        try {
        	result = tmpl.read(u, v);
        } finally {
        	u.cachingClear();
        	this.unpackerPool.offer(u);
        }
        return result;
    }

    /**
     * Deserializes byte array to object.
     *
     * @since 0.6.8
     * @param bytes
     *            input byte array
     * @param v
     * @return
     * @throws IOException
     */
    public <T> T read(byte[] bytes, int off, int len, Class<T> c) throws IOException {
        @SuppressWarnings("unchecked")
        Template<T> tmpl = registry.lookup(c);
        BufferUnpacker u = createBufferUnpacker(bytes, off, len);
        T result;
        try {
        	result = tmpl.read(u, null);
        } finally {
        	u.cachingClear();
        	this.unpackerPool.offer(u);
        }
        return result;
    }

    /**
     * Deserializes buffer to object.
     * 
     * @since 0.6.0
     * @param b
     *            input {@link java.nio.ByteBuffer} object
     * @param v
     * @return
     * @throws IOException
     */
    public <T> T read(ByteBuffer b, T v) throws IOException {
        @SuppressWarnings("unchecked")
        Template<T> tmpl = registry.lookup(v.getClass());
        return read(b, v, tmpl);
    }

    /**
     * Deserializes buffer to object according to template.
     * 
     * @since 0.6.0
     * @param b
     *            input buffer object
     * @param tmpl
     * @return
     * @throws IOException
     */
    public <T> T read(ByteBuffer b, Template<T> tmpl) throws IOException {
        return read(b, null, tmpl);
    }

    /**
     * Deserializes buffer to object of specified class.
     * 
     * @since 0.6.0
     * @param b
     * @param c
     * @return
     * @throws IOException
     */
    public <T> T read(ByteBuffer b, Class<T> c) throws IOException {
        @SuppressWarnings("unchecked")
        Template<T> tmpl = registry.lookup(c);
        return read(b, null, tmpl);
    }

    /**
     * Deserializes buffer to object according to template.
     * 
     * @since 0.6.0
     * @param b
     *            input buffer object
     * @param v
     * @param tmpl
     * @return
     * @throws IOException
     */
    public <T> T read(ByteBuffer b, T v, Template<T> tmpl) throws IOException {
        BufferUnpacker u = createBufferUnpacker(b);
        T result;
        try {
        	result = tmpl.read(u, v);
        } finally {
        	u.cachingClear();
        	this.unpackerPool.offer(u);
        }
        return result;
    }

    /**
     * Deserializes input stream to object.
     * 
     * @since 0.6.0
     * @param in
     *            input stream
     * @param v
     * @return
     * @throws IOException
     */
    public <T> T read(InputStream in, T v) throws IOException {
        @SuppressWarnings("unchecked")
        Template<T> tmpl = registry.lookup(v.getClass());
        return read(in, v, tmpl);
    }

    /**
     * Deserializes input stream to object according to template.
     * 
     * @since 0.6.0
     * @param in
     *            input stream
     * @param tmpl
     * @return
     * @throws IOException
     */
    public <T> T read(InputStream in, Template<T> tmpl) throws IOException {
        return read(in, null, tmpl);
    }

    /**
     * Deserializes input stream to object of specified class.
     * 
     * @since 0.6.0
     * @param in
     * @param c
     * @return
     * @throws IOException
     */
    public <T> T read(InputStream in, Class<T> c) throws IOException {
        @SuppressWarnings("unchecked")
        Template<T> tmpl = registry.lookup(c);
        return read(in, null, tmpl);
    }

    /**
     * Deserializes input stream to object according to template
     * 
     * @since 0.6.0
     * @param in
     *            input stream
     * @param v
     * @param tmpl
     * @return
     * @throws IOException
     */
    public <T> T read(InputStream in, T v, Template<T> tmpl) throws IOException {
        Unpacker u = createUnpacker(in);
        return tmpl.read(u, v);
    }

    /**
     * Converts specified {@link Value} object to object.
     * 
     * @since 0.6.0
     * @param v
     * @param to
     * @return
     * @throws IOException
     */
    public <T> T convert(Value v, T to) throws IOException {
        @SuppressWarnings("unchecked")
        Template<T> tmpl = registry.lookup(to.getClass());
        return tmpl.read(new Converter(this, v), to);
    }

    /**
     * Converts {@link Value} object to object specified class.
     * 
     * @since 0.6.0
     * @param v
     * @param c
     * @return
     * @throws IOException
     */
    public <T> T convert(Value v, Class<T> c) throws IOException {
        @SuppressWarnings("unchecked")
        Template<T> tmpl = registry.lookup(c);
        return tmpl.read(new Converter(this, v), null);
    }
        
    /**
     * Converts {@link Value} object to object according to template
     * 
     * @since 0.6.8
     * @param v
     * @param tmpl
     * @return
     * @throws IOException
     */
    public <T> T convert(Value v, Template<T> tmpl) throws IOException {
        return tmpl.read(new Converter(this, v), null);
    }

    /**
     * Unconverts specified object to {@link Value} object.
     * 
     * @since 0.6.0
     * @param v
     * @return
     * @throws IOException
     */
    public <T> Value unconvert(T v) throws IOException {
        Unconverter pk = new Unconverter(this);
        if (v == null) {
            pk.writeNil();
        } else {
            @SuppressWarnings("unchecked")
            Template<T> tmpl = registry.lookup(v.getClass());
            tmpl.write(pk, v);
        }
        return pk.getResult();
    }

    /**
     * Registers {@link Template} object for objects of
     * specified class. <tt>Template</tt> object is a pair of serializer and
     * deserializer for object serialization. It is generated automatically.
     * 
     * @since 0.6.0
     * @param type
     */
    public void register(Class<?> type) {
        registry.register(type);
    }

    /**
     * Registers specified {@link Template} object
     * associated by class.
     * 
     * @see #register(Class)
     * @since 0.6.0
     * @param type
     * @param template
     */
    public <T> void register(Class<T> type, Template<T> template) {
        registry.register(type, template);
    }

    /**
     * Unregisters {@link Template} object for objects of
     * specified class.
     * 
     * @since 0.6.0
     * @param type
     * @return
     */
    public boolean unregister(Class<?> type) {
        return registry.unregister(type);
    }

    /**
     * Unregisters all {@link Template} objects that have
     * been registered in advance.
     * 
     * @since 0.6.0
     */
    public void unregister() {
        registry.unregister();
    }

    /**
     * Looks up a {@link Template} object, which is
     * serializer/deserializer associated by specified class.
     * 
     * @since 0.6.0
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> Template<T> lookup(Class<T> type) {
        return registry.lookup(type);
    }

    public Template<?> lookup(Type type) {
        return registry.lookup(type);
    }

//    private static final MessagePack globalMessagePack = new MessagePack();
//
//    /**
//     * Serializes specified object and returns the byte array.
//     * 
//     * @deprecated {@link MessagePack#write(Object)}
//     * @param v
//     * @return
//     * @throws IOException
//     */
//    @Deprecated
//    public static byte[] pack(Object v) throws IOException {
//        return globalMessagePack.write(v);
//    }
//
//    /**
//     * Serializes specified object to output stream.
//     * 
//     * @deprecated {@link MessagePack#write(OutputStream, Object)}
//     * @param out
//     * @param v
//     * @throws IOException
//     */
//    @Deprecated
//    public static void pack(OutputStream out, Object v) throws IOException {
//        globalMessagePack.write(out, v);
//    }
//
//    /**
//     * Serializes object by specified template and return the byte array.
//     * 
//     * @deprecated {@link MessagePack#write(Object, Template)}
//     * @param v
//     * @param template
//     * @return
//     * @throws IOException
//     */
//    @Deprecated
//    public static <T> byte[] pack(T v, Template<T> template) throws IOException {
//        return globalMessagePack.write(v, template);
//    }
//
//    /**
//     * Serializes object to output stream. The object is serialized by specified
//     * template.
//     * 
//     * @deprecated {@link MessagePack#write(OutputStream, Object, Template)}
//     * @param out
//     * @param v
//     * @param template
//     * @throws IOException
//     */
//    @Deprecated
//    public static <T> void pack(OutputStream out, T v, Template<T> template)
//            throws IOException {
//        globalMessagePack.write(out, v, template);
//    }
//
//    /**
//     * Converts byte array to {@link Value} object.
//     * 
//     * @deprecated {@link MessagePack#read(byte[])}
//     * @param bytes
//     * @return
//     * @throws IOException
//     */
//    @Deprecated
//    public static Value unpack(byte[] bytes) throws IOException {
//        return globalMessagePack.read(bytes);
//    }
//
//    @Deprecated
//    public static <T> T unpack(byte[] bytes, Template<T> template) throws IOException {
//        BufferUnpacker u = new MessagePackBufferUnpacker(globalMessagePack).wrap(bytes);
//        return template.read(u, null);
//    }
//
//    @Deprecated
//    public static <T> T unpack(byte[] bytes, Template<T> template, T to) throws IOException {
//        BufferUnpacker u = new MessagePackBufferUnpacker(globalMessagePack).wrap(bytes);
//        return template.read(u, to);
//    }
//
//    /**
//     * Deserializes byte array to object of specified class.
//     * 
//     * @deprecated {@link MessagePack#read(byte[], Class)}
//     * @param bytes
//     * @param klass
//     * @return
//     * @throws IOException
//     */
//    @Deprecated
//    public static <T> T unpack(byte[] bytes, Class<T> klass) throws IOException {
//        return globalMessagePack.read(bytes, klass);
//    }
//
//    /**
//     * Deserializes byte array to object.
//     * 
//     * @param bytes
//     * @param to
//     * @return
//     * @throws IOException
//     */
//    @Deprecated
//    public static <T> T unpack(byte[] bytes, T to) throws IOException {
//        return globalMessagePack.read(bytes, to);
//    }
//
//    /**
//     * Converts input stream to {@link Value} object.
//     * 
//     * @deprecated {@link MessagePack#read(InputStream)}
//     * @param in
//     * @return
//     * @throws IOException
//     */
//    @Deprecated
//    public static Value unpack(InputStream in) throws IOException {
//        return globalMessagePack.read(in);
//    }
//
//    /**
//     * @deprecated
//     * @param in
//     * @param tmpl
//     * @return
//     * @throws IOException
//     * @throws MessageTypeException
//     */
//    @Deprecated
//    public static <T> T unpack(InputStream in, Template<T> tmpl)
//            throws IOException, MessageTypeException {
//        return tmpl.read(new MessagePackUnpacker(globalMessagePack, in), null);
//    }
//
//    /**
//     * @deprecated
//     * @param in
//     * @param tmpl
//     * @param to
//     * @return
//     * @throws IOException
//     * @throws MessageTypeException
//     */
//    @Deprecated
//    public static <T> T unpack(InputStream in, Template<T> tmpl, T to)
//            throws IOException, MessageTypeException {
//        return (T) tmpl.read(new MessagePackUnpacker(globalMessagePack, in), to);
//    }
//
//    /**
//     * Deserializes input stream to object of specified class.
//     * 
//     * @deprecated {@link MessagePack#read(InputStream, Class)}
//     * @param in
//     * @param klass
//     * @return
//     * @throws IOException
//     */
//    @Deprecated
//    public static <T> T unpack(InputStream in, Class<T> klass)
//            throws IOException {
//        return globalMessagePack.read(in, klass);
//    }
//
//    /**
//     * Deserializes input stream to object.
//     * 
//     * @deprecated {@link MessagePack#read(InputStream, Object)}
//     * @param in
//     * @param to
//     * @return
//     * @throws IOException
//     */
//    @Deprecated
//    public static <T> T unpack(InputStream in, T to) throws IOException {
//        return globalMessagePack.read(in, to);
//    }
}
