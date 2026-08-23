/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

package com.zhengshuyun.lava.http;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpUrlBuilderTest {

    @Test
    void fromPreservesCompleteUrl() {
        assertEquals("https://example.test/api/v1/messages?token=abc#section",
                HttpUrlBuilder.from("https://example.test/api?token=abc#section")
                        .appendPath("/v1/messages")
                        .build().toString());
        assertEquals("https://example.test/api/v1/messages",
                HttpUrlBuilder.from(URI.create("https://example.test/api/"))
                        .appendPath("v1/messages")
                        .build().toString());
    }

    @Test
    void createBuildsAllCommonUrlComponents() {
        assertEquals("https://user%20name:p%2Fa@example.test:8443/api%20root/model%2Fid"
                        + "?a=first&a=second&encoded=%2F#section%20one",
                HttpUrlBuilder.create("https", "example.test")
                        .username("user name")
                        .password("p/a")
                        .port(8443)
                        .path("api root")
                        .appendPathSegment("model/id")
                        .queryParam("a", "first")
                        .addQueryParam("a", "second")
                        .addEncodedQueryParam("encoded", "%2F")
                        .fragment("section one")
                        .build().toString());
    }

    @Test
    void authorityAndDefaultPortCanBeReplaced() {
        assertEquals("https://encoded%20user:encoded%2Fpassword@new.example.test/",
                HttpUrlBuilder.from("http://old.example.test:8080")
                        .scheme("https")
                        .encodedUsername("encoded%20user")
                        .encodedPassword("encoded%2Fpassword")
                        .host("new.example.test")
                        .defaultPort()
                        .build().toString());
    }

    @Test
    void appendPathPreservesBasePathAndNormalizesBoundarySlashes() {
        assertEquals("https://example.test/proxy/v1/messages/",
                HttpUrlBuilder.from("https://example.test/proxy///")
                        .appendPath("///v1/messages/")
                        .build().toString());
        assertEquals("https://example.test/proxy/v1/a%2Fb",
                HttpUrlBuilder.from("https://example.test/proxy")
                        .appendEncodedPath("/v1/a%2Fb")
                        .build().toString());
        assertEquals("https://example.test/encoded/%2F",
                HttpUrlBuilder.from("https://example.test/old")
                        .encodedPath("encoded/%2F")
                        .build().toString());
    }

    @Test
    void completePathPreservesEmptySegments() {
        assertEquals("https://example.test//api///models",
                HttpUrlBuilder.from("https://example.test/old")
                        .path("//api///models")
                        .build().toString());
        assertEquals("https://example.test//api///models",
                HttpUrlBuilder.from("https://example.test/old")
                        .encodedPath("//api///models")
                        .build().toString());
    }

    @Test
    void pathMethodsFollowOkHttpEncodingAndNormalizationRules() {
        assertEquals("https://example.test/%E6%A8%A1%E5%9E%8B%20%E5%88%97%E8%A1%A8/a%2Fb/%2F",
                HttpUrlBuilder.from("https://example.test/old")
                        .path("模型 列表")
                        .appendPathSegment("a/b")
                        .appendEncodedPathSegment("%2F")
                        .build().toString());
        assertEquals("https://example.test/api/models",
                HttpUrlBuilder.from("https://example.test/api/v1")
                        .appendEncodedPath("%2e%2e\\models")
                        .build().toString());
    }

    @Test
    void pathAppendHandlesEmptyWhitespaceAndExistingEmptySegments() {
        assertEquals("https://example.test/api",
                HttpUrlBuilder.from("https://example.test/api")
                        .appendPath("")
                        .appendEncodedPath("")
                        .build().toString());
        assertEquals("https://example.test/%20",
                HttpUrlBuilder.from("https://example.test")
                        .appendPath(" ")
                        .build().toString());
        assertEquals("https://example.test/api///users/%2F",
                HttpUrlBuilder.from("https://example.test/api///")
                        .appendPathSegment("users")
                        .appendEncodedPathSegment("%2F")
                        .build().toString());
    }

    @Test
    void queryMethodsSupportReplacementAppendEncodingAndRemoval() {
        assertEquals("https://example.test/path?a=new&flag&slash=%2F#new%20fragment",
                HttpUrlBuilder.from("https://example.test/path?a=old&a=older#old")
                        .queryParam("a", "new")
                        .addQueryParam("flag", null)
                        .encodedQueryParam("slash", "%2F")
                        .encodedFragment("new%20fragment")
                        .build().toString());
        assertEquals("https://example.test/path",
                HttpUrlBuilder.from("https://example.test/path?a=1#part")
                        .query(null)
                        .fragment(null)
                        .build().toString());
        assertEquals("https://example.test/path?a=1&a=%2F&flag",
                HttpUrlBuilder.from("https://example.test/path")
                        .encodedQuery("a=1&a=%2F&flag")
                        .build().toString());
        assertEquals("https://example.test/path?keep=1",
                HttpUrlBuilder.from("https://example.test/path?remove=1&remove=2&encoded%20name=3&keep=1")
                        .removeQueryParam("remove")
                        .removeEncodedQueryParam("encoded%20name")
                        .build().toString());
        assertEquals("https://example.test/path?=value&%20=blank",
                HttpUrlBuilder.from("https://example.test/path")
                        .addQueryParam("", "value")
                        .addQueryParam(" ", "blank")
                        .build().toString());
    }

    @Test
    void invalidUrlComponentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> HttpUrlBuilder.from(" "));
        assertThrows(IllegalArgumentException.class, () -> HttpUrlBuilder.from((String) null));
        assertThrows(IllegalArgumentException.class, () -> HttpUrlBuilder.from((URI) null));
        assertThrows(IllegalArgumentException.class, () -> HttpUrlBuilder.from("/relative"));
        assertThrows(IllegalArgumentException.class, () -> HttpUrlBuilder.from("ftp://example.test"));
        assertThrows(IllegalArgumentException.class, () -> HttpUrlBuilder.from(" https://example.test "));
        assertThrows(IllegalArgumentException.class, () -> HttpUrlBuilder.create("ftp", "example.test"));
        assertThrows(IllegalArgumentException.class, () -> HttpUrlBuilder.create("https", " "));
        assertThrows(IllegalArgumentException.class,
                () -> HttpUrlBuilder.from("https://example.test").port(0));
        assertThrows(IllegalArgumentException.class,
                () -> HttpUrlBuilder.from("https://example.test").appendPath(null));
        assertThrows(IllegalArgumentException.class,
                () -> HttpUrlBuilder.from("https://example.test").appendPathSegment(null));
        assertThrows(IllegalArgumentException.class,
                () -> HttpUrlBuilder.from("https://example.test").addQueryParam(null, "value"));
    }
}
