/*
 * AtomicFileWriter.java
 * Atomic CSV file writing with backup creation and restoration.
 */
package services;

import services.CsvFileUtilities.CsvTable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Provides atomic CSV file writing with automatic backup creation.
 *
 * <p>Every write follows the safe sequence: write to a temporary file,
 * create a {@code .bak} backup of the existing file, then atomically
 * replace the original with the temporary file. If the filesystem does
 * not support atomic moves, a standard replacement move is used as a
 * fallback.</p>
 *
 * <p>This module also provides backup restoration, allowing the GUI
 * to offer an "Undo Last Change" action.</p>
 *
 * <p>All methods are {@code static}; the class cannot be instantiated.</p>
 */
public final class AtomicFileWriter {

    /** The file extension appended to create backup files. */
    private static final String BACKUP_EXTENSION = ".bak";

    /** Private constructor prevents instantiation of this utility class. */
    private AtomicFileWriter() {
        // Utility class; prevent instantiation.
    }

    /**
     * Writes a {@link CsvTable} to the specified file atomically.
     *
     * <p>The write sequence is:</p>
     * <ol>
     *   <li>Write all data to a temporary file in the same directory.</li>
     *   <li>If the destination already exists, copy it to a {@code .bak}
     *       backup file (overwriting any previous backup).</li>
     *   <li>Atomically move the temporary file to the destination,
     *       falling back to a standard move if atomic moves are not
     *       supported by the filesystem.</li>
     *   <li>Delete the temporary file if any step fails.</li>
     * </ol>
     *
     * @param destination the target CSV file path
     * @param table       the CsvTable to write
     * @throws IOException if writing, backup creation, or file
     *                     replacement fails
     */
    public static void writeCsvTableAtomically(
            Path destination,
            CsvTable table) throws IOException {

        Path absoluteDestination = destination.toAbsolutePath();
        Path parent = absoluteDestination.getParent();

        // Ensure the parent directory exists for new files.
        if (parent != null) {
            Files.createDirectories(parent);
        }

        // Create a temporary file in the same directory as the destination.
        Path temporaryFile = Files.createTempFile(
                parent,
                destination.getFileName().toString(),
                ".tmp"
        );

        try {
            // Write all CSV data to the temporary file.
            try (BufferedWriter writer = Files.newBufferedWriter(
                    temporaryFile, StandardCharsets.UTF_8)) {

                // Write the header row first.
                CsvFileUtilities.writeCsvRow(writer, table.headers);

                // Write each data row, ensuring correct column count.
                for (List<String> row : table.rows) {
                    CsvFileUtilities.ensureRowSize(
                            row, table.headers.size()
                    );
                    CsvFileUtilities.writeCsvRow(writer, row);
                }
            }

            // Create a backup of the existing file before replacing it.
            if (Files.exists(absoluteDestination)) {
                Path backupFile = getBackupPath(absoluteDestination);

                // Copy with attributes preserved, overwriting previous backup.
                Files.copy(
                        absoluteDestination,
                        backupFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES
                );
            }

            // Attempt an atomic move for crash safety.
            try {
                Files.move(
                        temporaryFile,
                        absoluteDestination,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException exception) {
                // Fall back to a standard move on filesystems that do not
                // support atomic operations (e.g. some network drives).
                Files.move(
                        temporaryFile,
                        absoluteDestination,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } finally {
            // Clean up the temporary file if it still exists (e.g. on error).
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * Restores a file from its {@code .bak} backup.
     *
     * <p>Before restoring, the current version of the target file is
     * preserved as a new backup (so the user can undo the undo). Then
     * the backup is copied over the target file atomically.</p>
     *
     * @param targetFile the file to restore (its {@code .bak} sibling
     *                   is the source)
     * @return {@code true} if the backup existed and was restored,
     *         {@code false} if no backup was found
     * @throws IOException if copying or moving fails
     */
    public static boolean restoreBackup(Path targetFile) throws IOException {
        Path absoluteTarget = targetFile.toAbsolutePath();
        Path backupFile = getBackupPath(absoluteTarget);

        // Check that a backup file actually exists.
        if (!Files.isRegularFile(backupFile)) {
            return false;
        }

        Path parent = absoluteTarget.getParent();

        // Save the current file as a temporary file before overwriting.
        Path swapFile = null;
        if (Files.exists(absoluteTarget)) {
            swapFile = Files.createTempFile(
                    parent,
                    absoluteTarget.getFileName().toString(),
                    ".swap"
            );

            // Preserve the current version as the swap file.
            Files.copy(
                    absoluteTarget,
                    swapFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES
            );
        }

        try {
            // Copy the backup over the target file.
            Files.copy(
                    backupFile,
                    absoluteTarget,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES
            );

            // Make the old current file the new backup (undo-the-undo).
            if (swapFile != null) {
                Files.move(
                        swapFile,
                        backupFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return true;
        } finally {
            // Clean up the swap file if it still exists (e.g. on error).
            if (swapFile != null) {
                Files.deleteIfExists(swapFile);
            }
        }
    }

    /**
     * Checks whether a {@code .bak} backup file exists for the given
     * target file.
     *
     * @param targetFile the file whose backup to check for
     * @return {@code true} if a backup exists, {@code false} otherwise
     */
    public static boolean hasBackup(Path targetFile) {
        Path backupFile = getBackupPath(targetFile.toAbsolutePath());
        return Files.isRegularFile(backupFile);
    }

    /**
     * Returns the backup file path for a given target file by appending
     * the {@code .bak} extension.
     *
     * @param targetFile the original file path
     * @return the path to the corresponding backup file
     */
    public static Path getBackupPath(Path targetFile) {
        Path absoluteTarget = targetFile.toAbsolutePath();

        // The backup sits next to the original with a .bak extension.
        return absoluteTarget.resolveSibling(
                absoluteTarget.getFileName().toString()
                        + BACKUP_EXTENSION
        );
    }
}
