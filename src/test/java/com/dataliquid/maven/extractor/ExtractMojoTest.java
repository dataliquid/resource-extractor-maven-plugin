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
package com.dataliquid.maven.extractor;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;

import com.dataliquid.maven.extractor.model.DependencyConfig;

/**
 * Integration tests for ExtractMojo.
 */
public class ExtractMojoTest extends AbstractMojoTestCase {

    private File outputDirectory;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        outputDirectory = new File(getBasedir(), "target/test-output/mojo");
        if (outputDirectory.exists()) {
            deleteDirectory(outputDirectory);
        }
        outputDirectory.mkdirs();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test basic extraction of META-INF files from commons-io JAR.
     */
    public void testBasicExtraction() throws Exception {
        File pom = getTestFile("target/test-classes/test-poms/basic-extract-test-pom.xml");
        assertNotNull("POM file should not be null", pom);
        assertTrue("POM file should exist: " + pom.getAbsolutePath(), pom.exists());

        ExtractMojo mojo = (ExtractMojo) lookupMojo("extract", pom);
        assertNotNull("Mojo should not be null", mojo);

        MavenProject project = createMockProject("compile");
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "outputDirectory", outputDirectory);

        DependencyConfig depConfig = new DependencyConfig("commons-io", "commons-io");
        setVariableValueToObject(mojo, "dependencies", Arrays.asList(depConfig));
        setVariableValueToObject(mojo, "includes", Arrays.asList("META-INF/**"));
        setVariableValueToObject(mojo, "scope", "compile");

        mojo.execute();

        File metaInfDir = new File(outputDirectory, "META-INF");
        assertTrue("META-INF directory should be created", metaInfDir.exists());
        assertTrue("LICENSE.txt should be extracted", new File(metaInfDir, "LICENSE.txt").exists());
        assertTrue("NOTICE.txt should be extracted", new File(metaInfDir, "NOTICE.txt").exists());
    }

    /**
     * Test that missing dependency logs a warning but does not fail.
     */
    public void testMissingDependency() throws Exception {
        File pom = getTestFile("target/test-classes/test-poms/missing-dependency-test-pom.xml");
        assertNotNull("POM file should not be null", pom);
        assertTrue("POM file should exist", pom.exists());

        ExtractMojo mojo = (ExtractMojo) lookupMojo("extract", pom);
        assertNotNull("Mojo should not be null", mojo);

        MavenProject project = createMockProject("compile");
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "outputDirectory", outputDirectory);

        DependencyConfig depConfig = new DependencyConfig("non.existent", "missing-artifact");
        setVariableValueToObject(mojo, "dependencies", Arrays.asList(depConfig));
        setVariableValueToObject(mojo, "scope", "compile");

        mojo.execute();

        String[] files = outputDirectory.list();
        assertTrue("Output directory should be empty", files == null || files.length == 0);
    }

    /**
     * Test include pattern to extract only .txt files.
     */
    public void testIncludePattern() throws Exception {
        File pom = getTestFile("target/test-classes/test-poms/include-pattern-test-pom.xml");
        assertNotNull("POM file should not be null", pom);
        assertTrue("POM file should exist", pom.exists());

        ExtractMojo mojo = (ExtractMojo) lookupMojo("extract", pom);
        assertNotNull("Mojo should not be null", mojo);

        MavenProject project = createMockProject("compile");
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "outputDirectory", outputDirectory);

        DependencyConfig depConfig = new DependencyConfig("commons-io", "commons-io");
        setVariableValueToObject(mojo, "dependencies", Arrays.asList(depConfig));
        setVariableValueToObject(mojo, "includes", Arrays.asList("**/*.txt"));
        setVariableValueToObject(mojo, "scope", "compile");

        mojo.execute();

        File metaInfDir = new File(outputDirectory, "META-INF");
        assertTrue("META-INF directory should be created", metaInfDir.exists());
        assertTrue("LICENSE.txt should be extracted", new File(metaInfDir, "LICENSE.txt").exists());
        assertTrue("NOTICE.txt should be extracted", new File(metaInfDir, "NOTICE.txt").exists());
        assertFalse("MANIFEST.MF should NOT be extracted", new File(metaInfDir, "MANIFEST.MF").exists());
    }

    /**
     * Test exclude pattern to exclude .class files.
     */
    public void testExcludePattern() throws Exception {
        File pom = getTestFile("target/test-classes/test-poms/exclude-pattern-test-pom.xml");
        assertNotNull("POM file should not be null", pom);
        assertTrue("POM file should exist", pom.exists());

        ExtractMojo mojo = (ExtractMojo) lookupMojo("extract", pom);
        assertNotNull("Mojo should not be null", mojo);

        MavenProject project = createMockProject("compile");
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "outputDirectory", outputDirectory);

        DependencyConfig depConfig = new DependencyConfig("commons-io", "commons-io");
        setVariableValueToObject(mojo, "dependencies", Arrays.asList(depConfig));
        setVariableValueToObject(mojo, "includes", Arrays.asList("META-INF/**"));
        setVariableValueToObject(mojo, "excludes", Arrays.asList("**/*.class"));
        setVariableValueToObject(mojo, "scope", "compile");

        mojo.execute();

        File metaInfDir = new File(outputDirectory, "META-INF");
        assertTrue("META-INF directory should be created", metaInfDir.exists());
        assertTrue("LICENSE.txt should be extracted", new File(metaInfDir, "LICENSE.txt").exists());
        assertNoClassFiles(metaInfDir);
    }

    /**
     * Test flatten structure option.
     */
    public void testFlattenStructure() throws Exception {
        File pom = getTestFile("target/test-classes/test-poms/flatten-test-pom.xml");
        assertNotNull("POM file should not be null", pom);
        assertTrue("POM file should exist", pom.exists());

        ExtractMojo mojo = (ExtractMojo) lookupMojo("extract", pom);
        assertNotNull("Mojo should not be null", mojo);

        MavenProject project = createMockProject("compile");
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "outputDirectory", outputDirectory);

        DependencyConfig depConfig = new DependencyConfig("commons-io", "commons-io");
        setVariableValueToObject(mojo, "dependencies", Arrays.asList(depConfig));
        setVariableValueToObject(mojo, "includes", Arrays.asList("META-INF/*.txt"));
        setVariableValueToObject(mojo, "flattenStructure", true);
        setVariableValueToObject(mojo, "scope", "compile");

        mojo.execute();

        assertTrue("LICENSE.txt should be in root (flattened)", new File(outputDirectory, "LICENSE.txt").exists());
        assertTrue("NOTICE.txt should be in root (flattened)", new File(outputDirectory, "NOTICE.txt").exists());
        assertFalse("META-INF should not exist when flattened", new File(outputDirectory, "META-INF").exists());
    }

    /**
     * Test scope filtering with test scope.
     */
    public void testScopeFilter() throws Exception {
        File pom = getTestFile("target/test-classes/test-poms/scope-test-pom.xml");
        assertNotNull("POM file should not be null", pom);
        assertTrue("POM file should exist", pom.exists());

        ExtractMojo mojo = (ExtractMojo) lookupMojo("extract", pom);
        assertNotNull("Mojo should not be null", mojo);

        MavenProject project = createMockProject("test");
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "outputDirectory", outputDirectory);

        DependencyConfig depConfig = new DependencyConfig("commons-io", "commons-io");
        setVariableValueToObject(mojo, "dependencies", Arrays.asList(depConfig));
        setVariableValueToObject(mojo, "includes", Arrays.asList("META-INF/**"));
        setVariableValueToObject(mojo, "scope", "test");

        mojo.execute();

        File metaInfDir = new File(outputDirectory, "META-INF");
        assertTrue("META-INF directory should be created with test scope", metaInfDir.exists());
        assertTrue("LICENSE.txt should be extracted with test scope", new File(metaInfDir, "LICENSE.txt").exists());
    }

    private MavenProject createMockProject(String scope) {
        MavenProject project = new MavenProject();
        project.setGroupId("com.dataliquid.maven.test");
        project.setArtifactId("test-project");
        project.setVersion("1.0.0");

        File commonsIoJar = findCommonsIoJar();
        if (commonsIoJar != null && commonsIoJar.exists()) {
            DefaultArtifact artifact = new DefaultArtifact("commons-io", "commons-io", "2.21.0", scope, "jar", null,
                    new DefaultArtifactHandler("jar"));
            artifact.setFile(commonsIoJar);

            Set<Artifact> artifacts = new LinkedHashSet<>();
            artifacts.add(artifact);
            project.setArtifacts(artifacts);
        }

        return project;
    }

    private File findCommonsIoJar() {
        String userHome = System.getProperty("user.home");
        File localRepo = new File(userHome, ".m2/repository");
        File commonsIoJar = new File(localRepo, "commons-io/commons-io/2.21.0/commons-io-2.21.0.jar");
        return commonsIoJar.exists() ? commonsIoJar : null;
    }

    private void assertNoClassFiles(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        assertNoClassFiles(file);
                    } else {
                        assertFalse("Should not have .class files: " + file.getName(),
                                file.getName().endsWith(".class"));
                    }
                }
            }
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }
}
