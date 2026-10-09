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

package org.apache.atlas.notification;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasBaseClient;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.AtlasException;
import org.apache.atlas.kafka.NotificationProvider;
import org.apache.atlas.notification.rest.RestNotification;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import static org.apache.atlas.kafka.KafkaNotification.ATLAS_HOOK_TOPIC;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class RestNotificationTest {

    private NotificationInterface notifier;
    private Configuration conf;

    @Mock
    private WebTarget service;

    @Mock
    private Invocation.Builder resourceBuilderMock;

    @BeforeClass
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        conf = ApplicationProperties.get();

        conf.setProperty(AtlasConfiguration.NOTIFICATION_HOOK_REST_ENABLED.getPropertyName(), true);
        conf.setProperty(NotificationProvider.CONF_ATLAS_HOOK_SPOOL_ENABLED, false);

        notifier = NotificationProvider.get();

    }

    @Test
    public void testNotificationProvider () throws Exception {
        assertEquals(notifier.getClass(), RestNotification.class);
    }

    @Test
    public void testPostNotificationToTopic () throws Exception {
        AtlasClientV2       client   = new AtlasClientV2(service, conf);
        AtlasBaseClient.API api      = client.formatPathWithParameter(AtlasClientV2.API_V2.POST_NOTIFICATIONS_TO_TOPIC, ATLAS_HOOK_TOPIC);
        Invocation.Builder builder  = setupBuilder(api, service);
        Response      response = mock(Response.class);

        when(response.getStatus()).thenReturn(Response.Status.NO_CONTENT.getStatusCode());
        when(builder.method(anyString(), (Entity<?>) anyObject())).thenReturn(response);

        ((RestNotification)notifier).atlasClientV2 = client;

        try {
            ((RestNotification)notifier).sendInternal(NotificationInterface.NotificationType.HOOK, new ArrayList<String>(Arrays.asList("Dummy")));
        } catch (NotificationException e) {
            Assert.fail("Failed with Exception");
        }
    }

    @Test
    public void testNotificationException () throws Exception {
        AtlasClientV2       client   = new AtlasClientV2(service, conf);
        AtlasBaseClient.API api      = client.formatPathWithParameter(AtlasClientV2.API_V2.POST_NOTIFICATIONS_TO_TOPIC, ATLAS_HOOK_TOPIC);
        Invocation.Builder builder  = setupBuilder(api, service);
        Response      response = mock(Response.class);

        when(response.getStatus()).thenReturn(AtlasErrorCode.NOTIFICATION_EXCEPTION.getHttpCode().getStatusCode());
        when(response.readEntity((Class<String>) anyObject())).thenReturn(AtlasErrorCode.NOTIFICATION_EXCEPTION.getErrorCode());
        when(builder.method(anyString(), (Entity<?>) anyObject())).thenReturn(response);

        ((RestNotification)notifier).atlasClientV2 = client;

        try {
            ((RestNotification)notifier).sendInternal(NotificationInterface.NotificationType.HOOK, new ArrayList<String>(Arrays.asList("Dummy")));
        } catch (NotificationException e) {
            Assert.assertTrue(e.getMessage().contains(AtlasErrorCode.NOTIFICATION_EXCEPTION.getErrorCode()));
        }
    }

    private Invocation.Builder setupBuilder(AtlasClientV2.API api, WebTarget webResource) {
        when(webResource.path(api.getPath())).thenReturn(service);
        when(webResource.path(api.getNormalizedPath())).thenReturn(service);

        return getBuilder(service);
    }

    private Invocation.Builder getBuilder(WebTarget resourceObject) {
        when(resourceObject.request()).thenReturn(resourceBuilderMock);
        when(resourceObject.path(anyString())).thenReturn(resourceObject);
        when(resourceBuilderMock.accept(MediaType.APPLICATION_JSON)).thenReturn(resourceBuilderMock);
        when(resourceBuilderMock.accept(MediaType.MULTIPART_FORM_DATA)).thenReturn(resourceBuilderMock);
        when(resourceBuilderMock.accept(MediaType.APPLICATION_JSON + "; charset=UTF-8")).thenReturn(resourceBuilderMock);

        return resourceBuilderMock;
    }

    @Test
    public void testRestNotificationEndpointPrefersHookRestAddress() throws Exception {
        Configuration localConf = new PropertiesConfiguration();
        localConf.setProperty("atlas.hook.rest.notification.address", "http://atlas-rest.example.com:41000/rest");
        localConf.setProperty("atlas.rest.address", "http://atlas-main.example.com:21000");
        localConf.setProperty("atlas.rest.basic.auth.username", "admin");
        localConf.setProperty("atlas.rest.basic.auth.password", "admin123");

        RestNotification restNotification = new RestNotification(localConf);

        String[] configuredEndpoints = getConfiguredBaseUrls(restNotification.atlasClientV2);

        assertEquals(configuredEndpoints.length, 1);
        assertEquals(configuredEndpoints[0], "http://atlas-rest.example.com:41000/rest");
    }

    private String[] getConfiguredBaseUrls(AtlasClientV2 client) throws Exception {
        Field atlasServerEnsembleField = AtlasBaseClient.class.getDeclaredField("atlasServerEnsemble");
        atlasServerEnsembleField.setAccessible(true);
        Object atlasServerEnsemble = atlasServerEnsembleField.get(client);

        if (atlasServerEnsemble == null) {
            return new String[0];
        }

        Field membersField = atlasServerEnsemble.getClass().getDeclaredField("members");
        membersField.setAccessible(true);
        String[] members = (String[]) membersField.get(atlasServerEnsemble);

        return members;
    }

    @Test(expectedExceptions = AtlasException.class)
    public void testRestNotificationFailsFastWhenHookAddressNotConfigured() throws Exception {
        Configuration localConf = new PropertiesConfiguration();
        localConf.setProperty("atlas.rest.basic.auth.username", "admin");
        localConf.setProperty("atlas.rest.basic.auth.password", "admin123");

        new RestNotification(localConf);
    }

}
