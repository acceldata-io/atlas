/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.web.integration;

import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static org.apache.atlas.kafka.KafkaNotification.ATLAS_HOOK_TOPIC;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class NotificationRestIT extends BaseResourceIT {

    @Test
    public void unAuthPostNotification() throws IOException {
        AtlasClientV2 unAuthClient  = new AtlasClientV2(atlasUrls, new String[]{"admin", "wr0ng_pa55w0rd"});

        try {
            unAuthClient.postNotificationToTopic(ATLAS_HOOK_TOPIC, new ArrayList<String>(Arrays.asList("Dummy")));
        } catch(AtlasServiceException e) {
            assertNotNull(e.getStatus(), "expected server error code in the status");
        }
    }

    @Test
    public void postNotificationBasicTest() throws Exception {
        String db_name            = "db_" + randomString();
        String cluster_name       = "cl" + randomString();
        String qualifiedName      = db_name + "@" + cluster_name;
        String notificationString = TestResourceFileUtils.getJson("notifications/create-db")
                .replaceAll("--name--", db_name).replaceAll("--clName--", cluster_name)
                .replace("\"--ts--\"", String.valueOf((new Date()).getTime()));

        try {
            atlasClientV2.postNotificationToTopic(ATLAS_HOOK_TOPIC, new ArrayList<String>(Arrays.asList(notificationString)));

            fail("Expected notification POST to fail — endpoint removed from main webapp (ATLAS-5377)");
        } catch (AtlasServiceException e) {
            assertNotNull(e.getStatus(), "expected HTTP error when posting to removed endpoint");

            assertTrue(e.getStatus().getStatusCode() != Response.Status.NO_CONTENT.getStatusCode(),
                    "notification POST must not return 204 on main webapp");
        }

    }
}
