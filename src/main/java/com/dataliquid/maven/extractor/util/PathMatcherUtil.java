/*
 * Copyright © 2024 dataliquid GmbH | www.dataliquid.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.dataliquid.maven.extractor.util;

import java.util.List;

/**
 * Utility for pattern matching against file paths. Supports glob patterns like
 * *.xml, config/**, etc.
 */
public class PathMatcherUtil {

    private static final char GLOB_STAR = '*';
    private static final String GLOBSTAR_PATTERN = "**";
    private static final char PATH_SEPARATOR = '/';

    private PathMatcherUtil() {
        // Utility class
    }

    /**
     * Check if a path matches any of the given patterns. Returns true if no
     * patterns are provided (match all).
     *
     * @param  path     the file path to check
     * @param  patterns list of glob patterns to match against
     *
     * @return          true if path matches any pattern or patterns is empty/null
     */
    public static boolean matches(String path, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return true;
        }

        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }

            if (simpleGlobMatch(path, pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Simple glob pattern matcher supporting * and ** wildcards.
     */
    @SuppressWarnings("PMD.CognitiveComplexity")
    private static boolean simpleGlobMatch(String path, String pattern) {
        // Handle ** globstar pattern
        if (pattern.contains(GLOBSTAR_PATTERN)) {
            String beforeGlobstar = pattern.substring(0, pattern.indexOf(GLOBSTAR_PATTERN));
            String afterGlobstar = pattern.substring(pattern.indexOf(GLOBSTAR_PATTERN) + 2);

            // Remove leading / from afterGlobstar
            if (afterGlobstar.startsWith(Character.toString(PATH_SEPARATOR))) {
                afterGlobstar = afterGlobstar.substring(1);
            }

            // Check if path starts with beforeGlobstar
            if (!path.startsWith(beforeGlobstar)) {
                return false;
            }

            // If afterGlobstar is empty, globstar matches remaining path
            if (afterGlobstar.isEmpty()) {
                return true;
            }

            // If afterGlobstar contains *, use simpleMatch
            if (afterGlobstar.contains(Character.toString(GLOB_STAR))) {
                String remainder = path.substring(beforeGlobstar.length());
                return simpleGlobMatch(remainder, afterGlobstar);
            }

            // Simple suffix match
            return path.endsWith(afterGlobstar);
        }

        // Handle simple * wildcard
        return simpleMatch(path, pattern);
    }

    /**
     * Simple pattern matcher supporting * wildcard.
     */
    private static boolean simpleMatch(String path, String pattern) {
        int pathIdx = 0;
        int patternIdx = 0;
        int pathLen = path.length();
        int patternLen = pattern.length();

        while (pathIdx < pathLen && patternIdx < patternLen) {
            if (pattern.charAt(patternIdx) == GLOB_STAR) {
                patternIdx++;
                if (patternIdx >= patternLen) {
                    return true;
                }
                char nextPatChar = pattern.charAt(patternIdx);
                while (pathIdx < pathLen && path.charAt(pathIdx) != nextPatChar) {
                    pathIdx++;
                }
                continue;
            }

            if (path.charAt(pathIdx) != pattern.charAt(patternIdx)) {
                return false;
            }

            pathIdx++;
            patternIdx++;
        }

        while (patternIdx < patternLen && pattern.charAt(patternIdx) == GLOB_STAR) {
            patternIdx++;
        }

        return pathIdx == pathLen && patternIdx == patternLen;
    }

    /**
     * Check if a path matches any of the given patterns (exclude patterns). Returns
     * true if path should be excluded. Returns false if no exclude patterns are
     * provided.
     *
     * @param  path            the file path to check
     * @param  excludePatterns list of glob patterns for exclusion
     *
     * @return                 true if path matches any exclude pattern, false if no
     *                         patterns or no match
     */
    public static boolean isExcluded(String path, List<String> excludePatterns) {
        if (excludePatterns == null || excludePatterns.isEmpty()) {
            return false;
        }

        for (String pattern : excludePatterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }

            if (simpleGlobMatch(path, pattern)) {
                return true;
            }
        }

        return false;
    }
}
