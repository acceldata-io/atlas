/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


@Provider
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Component
public class AtlasJsonProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasJsonProvider.class);

    private static final ObjectMapper mapper = new ObjectMapper()
                                                    .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);

    public AtlasJsonProvider() {
        LOG.info("AtlasJsonProvider() instantiated");
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType != null && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)
                && !String.class.isAssignableFrom(type);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                           MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        try {
            return mapper.readValue(entityStream, mapper.constructType(genericType));
        } catch (JsonParseException jpe) {
            LOG.error("Malformed json passed to server", jpe);
            throw new WebApplicationException(Servlets.getErrorResponse(jpe.getMessage(), Response.Status.BAD_REQUEST));
        } catch (JsonMappingException jme) {
            LOG.error("Malformed json passed to server, incorrect data type used", jme);
            throw new WebApplicationException(Servlets.getErrorResponse(jme.getMessage(), Response.Status.BAD_REQUEST));
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType != null && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)
                && !String.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException {
        mapper.writeValue(entityStream, o);
    }
}
