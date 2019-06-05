/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.httpcache.util;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilties tied to caching keys / values.
 */
public class CacheUtils {

    static final String COOKIEPREFIX_HOST = "__Host-";
    static final String COOKIEPREFIX_SECURE = "__Secure-";
    static final String HEADERKEY_COOKIE = "Set-Cookie";

    private CacheUtils() {}


    public static Map<String, List<String>> extractHeaders(Collection<Pattern> excludedHeaderRegexList, Collection<String> excludedCookieKeyList, SlingHttpServletResponse response, HttpCacheConfig cacheConfig) {

        List<Pattern> excludedHeaders = Stream.concat(excludedHeaderRegexList.stream(), cacheConfig.getExcludedResponseHeaderPatterns().stream())
                .collect(Collectors.toList());

        List<String> excludedCookieKeys = Stream.concat(excludedCookieKeyList.stream(), cacheConfig.getExcludedCookieKeys().stream())
                .collect(Collectors.toList());

        return response.getHeaderNames().stream()
                .filter(headerName -> excludedHeaders.stream()
                        .noneMatch(pattern -> pattern.matcher(headerName).matches())
                ).collect(
                        Collectors.toMap(headerName -> headerName, headerName -> filterCookieHeaders(response, excludedCookieKeys, headerName)
                        ));
    }

    public static List<String> filterCookieHeaders(SlingHttpServletResponse response, List<String> excludedCookieKeys, String headerName) {
        if(!headerName.equals(HEADERKEY_COOKIE)){
            return new ArrayList<>(response.getHeaders(headerName));
        }
        //for set-cookie we apply another exclusion filter.
        return new ArrayList<>(response.getHeaders(headerName)).stream().filter(
                header -> {
                    String key;
                    if(header.startsWith(COOKIEPREFIX_HOST)){
                        key = StringUtils.removeStart( header,COOKIEPREFIX_HOST);
                    }else if(header.startsWith(COOKIEPREFIX_SECURE)){
                        key = StringUtils.removeStart( header, COOKIEPREFIX_SECURE);
                    }else{
                        key = header;
                    }
                    key = StringUtils.substringBefore(key, "=");

                    return !excludedCookieKeys.contains(key);
                }
        ).collect(Collectors.toList());
    }
}
