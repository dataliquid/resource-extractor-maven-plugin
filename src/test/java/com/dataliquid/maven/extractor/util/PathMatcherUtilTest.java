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

    public void testWildcardBacktrackingOnMultipleOccurrences() {
        assertTrue("*-backup.dat should match server-config-backup.dat",
                PathMatcherUtil.matches("server-config-backup.dat", Arrays.asList("*-backup.dat")));
        assertTrue("*-drop.sql should match schema-admin-drop.sql",
                PathMatcherUtil.matches("schema-admin-drop.sql", Arrays.asList("*-drop.sql")));
        assertTrue("*-drop.sql should match simple-drop.sql",
                PathMatcherUtil.matches("simple-drop.sql", Arrays.asList("*-drop.sql")));
        assertFalse("*-drop.sql should not match schema-admin-drop.txt",
                PathMatcherUtil.matches("schema-admin-drop.txt", Arrays.asList("*-drop.sql")));
    }

    public void testWildcardBacktrackingWithPath() {
        assertTrue("jdbc/schema/*-drop.sql should match jdbc/schema/schema-admin-drop.sql",
                PathMatcherUtil.matches("jdbc/schema/schema-admin-drop.sql", Arrays.asList("jdbc/schema/*-drop.sql")));
        assertFalse("jdbc/schema/*-drop.sql should not match jdbc/other/schema-admin-drop.sql",
                PathMatcherUtil.matches("jdbc/other/schema-admin-drop.sql", Arrays.asList("jdbc/schema/*-drop.sql")));
    }

    public void testIsExcludedBacktracking() {
        assertTrue("jdbc/schema/*-drop.sql should exclude jdbc/schema/schema-admin-drop.sql", PathMatcherUtil
                .isExcluded("jdbc/schema/schema-admin-drop.sql", Arrays.asList("jdbc/schema/*-drop.sql")));
        assertFalse("jdbc/schema/*-drop.sql should not exclude jdbc/schema/schema-admin-create.sql", PathMatcherUtil
                .isExcluded("jdbc/schema/schema-admin-create.sql", Arrays.asList("jdbc/schema/*-drop.sql")));
    }
}
