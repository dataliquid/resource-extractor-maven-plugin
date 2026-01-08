package com.dataliquid.maven.extractor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
     * Test basic extraction of META-INF files from test JAR.
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

        DependencyConfig depConfig = new DependencyConfig("test", "test-resources");
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

        DependencyConfig depConfig = new DependencyConfig("test", "test-resources");
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

        DependencyConfig depConfig = new DependencyConfig("test", "test-resources");
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

        DependencyConfig depConfig = new DependencyConfig("test", "test-resources");
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

        DependencyConfig depConfig = new DependencyConfig("test", "test-resources");
        setVariableValueToObject(mojo, "dependencies", Arrays.asList(depConfig));
        setVariableValueToObject(mojo, "includes", Arrays.asList("META-INF/**"));
        setVariableValueToObject(mojo, "scope", "test");

        mojo.execute();

        File metaInfDir = new File(outputDirectory, "META-INF");
        assertTrue("META-INF directory should be created with test scope", metaInfDir.exists());
        assertTrue("LICENSE.txt should be extracted with test scope", new File(metaInfDir, "LICENSE.txt").exists());
    }

    private MavenProject createMockProject(String scope) throws IOException {
        MavenProject project = new MavenProject();
        project.setGroupId("com.dataliquid.maven.test");
        project.setArtifactId("test-project");
        project.setVersion("1.0.0");

        File testJar = createTestJar();
        DefaultArtifact artifact = new DefaultArtifact("test", "test-resources", "1.0.0", scope, "jar", null,
                new DefaultArtifactHandler("jar"));
        artifact.setFile(testJar);

        Set<Artifact> artifacts = new LinkedHashSet<>();
        artifacts.add(artifact);
        project.setArtifacts(artifacts);

        return project;
    }

    private File createTestJar() throws IOException {
        File testDir = new File(getBasedir(), "target/test-output");
        testDir.mkdirs();
        File testJar = new File(testDir, "test-resources.jar");

        if (testJar.exists()) {
            return testJar;
        }

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(testJar))) {
            addZipEntry(zos, "META-INF/LICENSE.txt", "Test License Content");
            addZipEntry(zos, "META-INF/NOTICE.txt", "Test Notice Content");
            addZipEntry(zos, "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n");
            addZipEntry(zos, "META-INF/maven/test/test-resources/pom.xml", "<project/>");
            addZipEntry(zos, "META-INF/maven/test/test-resources/pom.properties", "version=1.0.0");
            addZipEntry(zos, "org/example/Test.class", "fake class content");
        }
        return testJar;
    }

    private void addZipEntry(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes());
        zos.closeEntry();
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

    /**
     * Test that ZIP Slip path traversal attacks are blocked.
     */
    public void testZipSlipProtection() throws Exception {
        File pom = getTestFile("target/test-classes/test-poms/zip-slip-test-pom.xml");
        assertNotNull("POM file should not be null", pom);
        assertTrue("POM file should exist", pom.exists());

        ExtractMojo mojo = (ExtractMojo) lookupMojo("extract", pom);
        assertNotNull("Mojo should not be null", mojo);

        // Create a malicious JAR with path traversal entry
        File maliciousJar = createMaliciousJar();

        MavenProject project = createMockProjectWithJar(maliciousJar, "malicious", "zip-slip-jar");
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "outputDirectory", outputDirectory);

        DependencyConfig depConfig = new DependencyConfig("malicious", "zip-slip-jar");
        setVariableValueToObject(mojo, "dependencies", Arrays.asList(depConfig));
        setVariableValueToObject(mojo, "scope", "compile");

        // Execute should handle the malicious entry gracefully
        try {
            mojo.execute();
        } catch (Exception e) {
            // Expected: IOException wrapped in MojoExecutionException
            assertTrue("Should be caused by path traversal protection",
                    e.getMessage().contains("outside of the target directory")
                            || e.getCause().getMessage().contains("outside of the target directory"));
        }

        // Verify that the malicious file was NOT extracted outside outputDirectory
        File parentDir = outputDirectory.getParentFile();
        File escapedFile = new File(parentDir, "evil.txt");
        assertFalse("Malicious file should NOT be extracted outside output directory", escapedFile.exists());
    }

    private File createMaliciousJar() throws IOException {
        File tempDir = new File(getBasedir(), "target/test-output");
        tempDir.mkdirs();
        File maliciousJar = new File(tempDir, "malicious-zip-slip.jar");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(maliciousJar))) {
            // Add a normal entry
            ZipEntry normalEntry = new ZipEntry("normal.txt");
            zos.putNextEntry(normalEntry);
            zos.write("Normal content".getBytes());
            zos.closeEntry();

            // Add a malicious path traversal entry
            ZipEntry maliciousEntry = new ZipEntry("../evil.txt");
            zos.putNextEntry(maliciousEntry);
            zos.write("Malicious content".getBytes());
            zos.closeEntry();
        }

        return maliciousJar;
    }

    private MavenProject createMockProjectWithJar(File jarFile, String groupId, String artifactId) {
        MavenProject project = new MavenProject();
        project.setGroupId("com.dataliquid.maven.test");
        project.setArtifactId("test-project");
        project.setVersion("1.0.0");

        DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, "1.0.0", "compile", "jar", null,
                new DefaultArtifactHandler("jar"));
        artifact.setFile(jarFile);

        Set<Artifact> artifacts = new LinkedHashSet<>();
        artifacts.add(artifact);
        project.setArtifacts(artifacts);

        return project;
    }
}
