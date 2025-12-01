package com.dataliquid.maven.extractor;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dataliquid.maven.extractor.model.DependencyConfig;
import com.dataliquid.maven.extractor.util.ArchiveExtractor;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Maven Mojo to extract resources from JAR dependencies. Goal: extract
 */
@Mojo(name = "extract", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class ExtractMojo extends AbstractMojo {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractMojo.class);
    private static final String SCOPE_COMPILE = "compile";
    private static final String SCOPE_PROVIDED = "provided";
    private static final String SCOPE_RUNTIME = "runtime";
    private static final String SCOPE_TEST = "test";
    private static final String SCOPE_SYSTEM = "system";
    private static final List<String> VALID_SCOPES = Arrays
            .asList(SCOPE_COMPILE, SCOPE_PROVIDED, SCOPE_RUNTIME, SCOPE_TEST, SCOPE_SYSTEM);

    /**
     * Output directory where resources will be extracted.
     */
    @Parameter(property = "resource.extractor.outputDirectory", required = true)
    private File outputDirectory;

    /**
     * List of dependencies to extract from. Format: groupId:artifactId (version is
     * optional)
     */
    @Parameter(property = "resource.extractor.dependencies")
    private List<DependencyConfig> dependencies;

    /**
     * Patterns to include (glob patterns). If empty, all files are included.
     */
    @Parameter(property = "resource.extractor.includes")
    private List<String> includes;

    /**
     * Patterns to exclude (glob patterns).
     */
    @Parameter(property = "resource.extractor.excludes")
    private List<String> excludes;

    /**
     * Whether to overwrite existing files.
     */
    @Parameter(property = "resource.extractor.overwrite", defaultValue = "true")
    private boolean overwrite = true;

    /**
     * Flatten the directory structure.
     */
    @Parameter(property = "resource.extractor.flattenStructure", defaultValue = "false")
    private boolean flattenStructure;

    /**
     * Prefix to add to extracted filenames.
     */
    @Parameter(property = "resource.extractor.filePrefix")
    private String filePrefix;

    /**
     * Suffix to add to extracted filenames (before extension).
     */
    @Parameter(property = "resource.extractor.fileSuffix")
    private String fileSuffix;

    /**
     * The dependency scope to use for artifact resolution. Valid values: compile,
     * provided, runtime, test, system. Default: compile
     */
    @Parameter(property = "resource.extractor.scope", defaultValue = SCOPE_COMPILE)
    private String scope;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            LOG.info("Starting resource extraction");

            // Validate configuration
            if (outputDirectory == null) {
                throw new MojoExecutionException("outputDirectory parameter is required");
            }

            // Validate scope
            if (scope != null && !VALID_SCOPES.contains(scope.toLowerCase(Locale.ROOT))) {
                LOG.warn("Invalid scope '{}', using default 'compile'", scope);
                scope = SCOPE_COMPILE;
            }

            if (dependencies == null || dependencies.isEmpty()) {
                LOG.warn("No dependencies configured for extraction");
                return;
            }

            // Extract from each configured dependency
            for (DependencyConfig depConfig : dependencies) {
                extractFromDependency(depConfig);
            }

            LOG.info("Resource extraction completed successfully");

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to extract resources", e);
        }
    }

    /**
     * Extract resources from a single dependency.
     */
    @SuppressWarnings("PMD.GuardLogStatement")
    private void extractFromDependency(DependencyConfig depConfig) throws Exception {
        LOG.info("Extracting from dependency: {} (scope: {})", depConfig, scope);

        // Find the artifact in project dependencies filtered by scope
        Set<Artifact> artifacts = new LinkedHashSet<>(project.getArtifacts());
        artifacts = filterByScope(artifacts, scope);
        Artifact targetArtifact = null;

        for (Artifact artifact : artifacts) {
            if (matches(artifact, depConfig)) {
                targetArtifact = artifact;
                break;
            }
        }

        if (targetArtifact == null) {
            LOG.warn("Dependency not found in project: {}", depConfig);
            return;
        }

        File jarFile = targetArtifact.getFile();
        if (jarFile == null || !jarFile.exists()) {
            LOG.warn("JAR file not found for artifact: {}", targetArtifact);
            return;
        }

        LOG.info("Extracting from JAR: {}", jarFile);

        // Extract files
        List<String> extractedFiles = ArchiveExtractor
                .extract(jarFile, outputDirectory, includes, excludes, overwrite, flattenStructure, filePrefix,
                        fileSuffix);

        if (LOG.isInfoEnabled()) {
            if (extractedFiles.isEmpty()) {
                LOG.info("No files extracted from {}", depConfig);
            } else {
                LOG.info("Extracted {} file(s) from {}:", extractedFiles.size(), depConfig);
                for (String file : extractedFiles) {
                    LOG.info("  - {}", file);
                }
            }
        }
    }

    /**
     * Check if an artifact matches the dependency configuration.
     */
    private boolean matches(Artifact artifact, DependencyConfig depConfig) {
        if (!artifact.getGroupId().equals(depConfig.getGroupId())) {
            return false;
        }

        if (!artifact.getArtifactId().equals(depConfig.getArtifactId())) {
            return false;
        }

        // Version is optional - if not specified in config, match any version
        if (depConfig.getVersion() != null && !artifact.getVersion().equals(depConfig.getVersion())) {
            return false;
        }

        return true;
    }

    /**
     * Filter artifacts by the configured scope.
     *
     * @param  artifacts   all project artifacts
     * @param  targetScope the scope to filter for
     *
     * @return             filtered set of artifacts matching the scope
     */
    private Set<Artifact> filterByScope(Set<Artifact> artifacts, String targetScope) {
        String effectiveScope = targetScope;
        if (effectiveScope == null || effectiveScope.isEmpty()) {
            effectiveScope = Artifact.SCOPE_COMPILE;
        }

        Set<Artifact> filtered = new LinkedHashSet<>();
        for (Artifact artifact : artifacts) {
            if (matchesScope(artifact.getScope(), effectiveScope)) {
                filtered.add(artifact);
            }
        }
        return filtered;
    }

    /**
     * Check if artifact scope matches or is included in target scope.
     *
     * @param  artifactScope the scope of the artifact
     * @param  targetScope   the configured target scope
     *
     * @return               true if the artifact scope matches the target scope
     */
    private boolean matchesScope(String artifactScope, String targetScope) {
        String effectiveArtifactScope = artifactScope;
        if (effectiveArtifactScope == null) {
            effectiveArtifactScope = Artifact.SCOPE_COMPILE;
        }

        switch (targetScope.toLowerCase(Locale.ROOT)) {
        case SCOPE_COMPILE:
            return Artifact.SCOPE_COMPILE.equals(effectiveArtifactScope);
        case SCOPE_PROVIDED:
            return Artifact.SCOPE_PROVIDED.equals(effectiveArtifactScope);
        case SCOPE_RUNTIME:
            return Artifact.SCOPE_COMPILE.equals(effectiveArtifactScope)
                    || Artifact.SCOPE_RUNTIME.equals(effectiveArtifactScope);
        case SCOPE_TEST:
            return true; // test scope includes all
        case SCOPE_SYSTEM:
            return Artifact.SCOPE_SYSTEM.equals(effectiveArtifactScope);
        default:
            LOG.warn("Unknown scope '{}', using compile", targetScope);
            return Artifact.SCOPE_COMPILE.equals(effectiveArtifactScope);
        }
    }
}
