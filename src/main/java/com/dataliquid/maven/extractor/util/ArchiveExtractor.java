package com.dataliquid.maven.extractor.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for extracting files from archive files.
 */
public class ArchiveExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(ArchiveExtractor.class);
    private static final int BUFFER_SIZE = 4096;

    private ArchiveExtractor() {
        // Utility class
    }

    /**
     * Extract files from an archive file.
     *
     * @param  archiveFile     the archive file to extract from
     * @param  outputDir       the directory to extract files to
     * @param  includePatterns list of patterns to include (null = all)
     * @param  excludePatterns list of patterns to exclude
     * @param  overwrite       whether to overwrite existing files
     * @param  flatten         whether to flatten the directory structure
     * @param  filePrefix      prefix to add to extracted filenames
     * @param  fileSuffix      suffix to add to extracted filenames (before
     *                         extension)
     *
     * @return                 list of extracted file paths (relative to outputDir)
     *
     * @throws IOException     if extraction fails
     */
    @SuppressWarnings({ "PMD.AvoidFileStream", "PMD.AssignmentInOperand" })
    public static List<String> extract(File archiveFile, File outputDir, List<String> includePatterns,
            List<String> excludePatterns, boolean overwrite, boolean flatten, String filePrefix, String fileSuffix)
            throws IOException {

        validateArchiveFile(archiveFile);
        ensureDirectoryExists(outputDir);

        List<String> extractedFiles = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archiveFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entry.isDirectory()) {
                    continue;
                }

                if (!PathMatcherUtil.matches(entryName, includePatterns)) {
                    continue;
                }

                if (PathMatcherUtil.isExcluded(entryName, excludePatterns)) {
                    continue;
                }

                File outputFile = determineOutputFile(entryName, outputDir, flatten, filePrefix, fileSuffix);

                if (extractEntry(zis, outputFile, entryName, overwrite)) {
                    extractedFiles.add(entryName);
                }
            }
        }

        return extractedFiles;
    }

    private static void validateArchiveFile(File archiveFile) {
        if (!archiveFile.exists() || !archiveFile.isFile()) {
            throw new IllegalArgumentException("Archive file not found: " + archiveFile);
        }
    }

    private static void ensureDirectoryExists(File directory) throws IOException {
        if (!directory.exists()) {
            Files.createDirectories(directory.toPath());
        }
    }

    @SuppressWarnings("PMD.AvoidFileStream")
    private static boolean extractEntry(ZipInputStream zis, File outputFile, String entryName, boolean overwrite)
            throws IOException {

        if (outputFile.exists() && !overwrite) {
            LOG.debug("Skipping existing file (overwrite=false): {}", outputFile);
            return false;
        }

        ensureDirectoryExists(outputFile.getParentFile());
        writeFile(zis, outputFile);

        LOG.debug("Extracted: {} -> {}", entryName, outputFile);
        return true;
    }

    @SuppressWarnings({ "PMD.AvoidFileStream", "PMD.AssignmentInOperand" })
    private static void writeFile(ZipInputStream zis, File outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = zis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }

    private static File determineOutputFile(String entryName, File outputDir, boolean flatten, String filePrefix,
            String fileSuffix) throws IOException {
        String basePath = flatten ? new File(entryName).getName() : entryName;
        String outputPath = applyNaming(basePath, filePrefix, fileSuffix);
        File outputFile = new File(outputDir, outputPath);

        // ZIP Slip protection: ensure file stays within output directory
        String canonicalOutputDir = outputDir.getCanonicalPath();
        String canonicalOutputFile = outputFile.getCanonicalPath();

        if (!canonicalOutputFile.startsWith(canonicalOutputDir + File.separator)) {
            throw new IOException("Entry is outside of the target directory: " + entryName);
        }

        return outputFile;
    }

    private static String applyNaming(String filePath, String filePrefix, String fileSuffix) {
        if (filePrefix == null && fileSuffix == null) {
            return filePath;
        }

        String baseName = FilenameUtils.getBaseName(filePath);
        String extension = FilenameUtils.getExtension(filePath);
        String directory = FilenameUtils.getFullPathNoEndSeparator(filePath);

        StringBuilder newFileName = new StringBuilder();
        if (!directory.isEmpty()) {
            newFileName.append(directory).append('/');
        }

        if (filePrefix != null && !filePrefix.isEmpty()) {
            newFileName.append(filePrefix);
        }

        newFileName.append(baseName);

        if (fileSuffix != null && !fileSuffix.isEmpty()) {
            newFileName.append(fileSuffix);
        }

        if (!extension.isEmpty()) {
            newFileName.append('.').append(extension);
        }

        return newFileName.toString();
    }
}
