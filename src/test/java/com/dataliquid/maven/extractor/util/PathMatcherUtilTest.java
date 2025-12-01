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

import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

/**
 * Unit tests for PathMatcherUtil.
 */
public class PathMatcherUtilTest extends TestCase {

    public void testMatchesWithNullPatterns() {
        assertTrue("Null patterns should match all", PathMatcherUtil.matches("any/path/file.txt", null));
    }

    public void testMatchesWithEmptyPatterns() {
        assertTrue("Empty patterns should match all",
                PathMatcherUtil.matches("any/path/file.txt", Collections.emptyList()));
    }

    public void testMatchesSimpleGlob() {
        assertTrue("*.txt should match file.txt", PathMatcherUtil.matches("file.txt", Arrays.asList("*.txt")));
        assertFalse("*.txt should not match file.xml", PathMatcherUtil.matches("file.xml", Arrays.asList("*.txt")));
    }

    public void testMatchesDoubleStarGlob() {
        assertTrue("META-INF/** should match META-INF/LICENSE.txt",
                PathMatcherUtil.matches("META-INF/LICENSE.txt", Arrays.asList("META-INF/**")));
        assertTrue("META-INF/** should match META-INF/maven/commons-io/pom.xml",
                PathMatcherUtil.matches("META-INF/maven/commons-io/pom.xml", Arrays.asList("META-INF/**")));
        assertFalse("META-INF/** should not match org/apache/file.class",
                PathMatcherUtil.matches("org/apache/file.class", Arrays.asList("META-INF/**")));
    }

    public void testMatchesMetaInfManifest() {
        // This is the exact path from commons-io JAR
        assertTrue("META-INF/** should match META-INF/MANIFEST.MF",
                PathMatcherUtil.matches("META-INF/MANIFEST.MF", Arrays.asList("META-INF/**")));
    }

    public void testMatchesDoubleStarWithExtension() {
        assertTrue("**/*.txt should match META-INF/LICENSE.txt",
                PathMatcherUtil.matches("META-INF/LICENSE.txt", Arrays.asList("**/*.txt")));
        assertTrue("**/*.txt should match file.txt", PathMatcherUtil.matches("file.txt", Arrays.asList("**/*.txt")));
        assertFalse("**/*.txt should not match file.xml",
                PathMatcherUtil.matches("file.xml", Arrays.asList("**/*.txt")));
    }

    public void testIsExcluded() {
        assertTrue("**/*.class should exclude File.class",
                PathMatcherUtil.isExcluded("org/apache/commons/File.class", Arrays.asList("**/*.class")));
        assertFalse("**/*.class should not exclude file.txt",
                PathMatcherUtil.isExcluded("META-INF/LICENSE.txt", Arrays.asList("**/*.class")));
    }

    public void testIsExcludedWithNullPatterns() {
        assertFalse("Null exclude patterns should not exclude anything",
                PathMatcherUtil.isExcluded("any/path/file.txt", null));
    }

    public void testIsExcludedWithEmptyPatterns() {
        assertFalse("Empty exclude patterns should not exclude anything",
                PathMatcherUtil.isExcluded("any/path/file.txt", Collections.emptyList()));
    }
}
