/**
 * Copyright 2004-2048 .
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ipd.jsf.gd.codec.msgpack;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ipd.org.msgpack.MessageTypeException;
import com.ipd.org.msgpack.packer.Packer;
import com.ipd.org.msgpack.template.AbstractTemplate;
import com.ipd.org.msgpack.unpacker.Unpacker;

/**
 * Title: <br>
 * <p/>
 * Description: <br>
 * <p/>
 */
public class JSFFileTemplate extends AbstractTemplate<File> {

    private static final Logger logger = LoggerFactory.getLogger(JSFFileTemplate.class);

    @Override
    public void write(Packer pk, File v, boolean required) throws IOException {
        if (v == null) {
            if (required) {
                logger.error("Attempted to write null");
                throw new MessageTypeException("Attempted to write null");
            }
            pk.writeNil();
            return;
        }
        pk.write(v.getPath());
    }

    @Override
    public File read(Unpacker u, File to, boolean required) throws IOException {
        if (!required && u.trySkipNil()) {
            return null;
        }
        String path = u.readString();
        return new File(path);
    }

    private JSFFileTemplate(){}

    private static JSFFileTemplate instance = new JSFFileTemplate();

    public static JSFFileTemplate getInstance(){
        return instance;
    }
}