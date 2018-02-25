/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package richtercloud.jhbuild.java.wrapper.download;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.utils.Charsets;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.jhbuild.java.wrapper.ExtractionException;
import richtercloud.jhbuild.java.wrapper.ExtractionMode;
import richtercloud.jhbuild.java.wrapper.MD5SumCheckUnequalsCallback;
import richtercloud.jhbuild.java.wrapper.MD5SumCheckUnequalsCallbackReaction;

/**
 *
 * @author richter
 */
public class AutoDownloader implements Downloader {
    private final static Logger LOGGER = LoggerFactory.getLogger(AutoDownloader.class);

    /**
     * Downloads a file a file located at {@code donwloadURL}, compares its MD5
     * checksum with {@code md5Sum} and extracts it into
     * {@code extractionLocation}.If the download or the verification failed
 the caller can take actions in {@code mD5SumCheckUnequalsCallback} and
    {@code downloadFailureCallback} callbacks.
     *
     * @param downloadCombi the download combi containing all relevant
     * information for the download and the extraction
     * @param skipMD5SumCheck a flag indicating that the MD5 checksum check
     * should be skipped
     * @param downloadFailureCallback the download failure callback to retrieve
     * the {@link DownloadFailureCallbackReation}
     * @param mD5SumCheckUnequalsCallback the MD5 checksum check failure
     * callback to retrieve the {@link MD5SumCheckUnequalsCallbackReaction}
     * @return {@code false} if the validation, download or extraction have been
     * canceled, otherwise {@code true}, but exception might have been thrown
     * @throws IOException
     * @throws ExtractionException
     * @throws IllegalArgumentException if the extraction directory specified in
     * {@code downloadCombi} is an existing path which doesn't point to a
     * directory
     */
    @Override
    public boolean downloadFile(DownloadCombi downloadCombi,
            boolean skipMD5SumCheck,
            DownloadFailureCallback downloadFailureCallback,
            MD5SumCheckUnequalsCallback mD5SumCheckUnequalsCallback,
            DownloadEmptyCallback downloadEmptyCallback) throws IOException,
            ExtractionException {
        if(downloadCombi == null) {
            throw new IllegalArgumentException("downloadCombi mustn't be null");
        }
        if(downloadCombi.getDownloadURL() == null
                || downloadCombi.getDownloadTarget() == null
                || downloadCombi.getExtractionMode() == null
                || downloadCombi.getExtractionLocation() == null
                || downloadCombi.getMd5Sum() == null) {
            throw new IllegalArgumentException(String.format("downloadURL, "
                    + "downloadTarget, extractionMode, extractionLocation and "
                    + "md5sum of downloadCombi need to be not null (were %s, "
                    + "%s, %s, %s and %s",
                    downloadCombi.getDownloadURL(),
                    downloadCombi.getDownloadTarget(),
                    downloadCombi.getExtractionMode(),
                    downloadCombi.getExtractionLocation(),
                    downloadCombi.getMd5Sum()));
        }
        DownloadCombi downloadCombi0 = downloadCombi;
        boolean success = false;
        int numberOfRetries = 0;
        while(!success) {
            try {
                boolean notCanceled = download(downloadCombi0,
                        skipMD5SumCheck,
                        downloadFailureCallback,
                        mD5SumCheckUnequalsCallback,
                        downloadEmptyCallback);
                if(!notCanceled) {
                    return false;
                }
                success = true;
            }catch (IOException | ExtractionException ex) {
                downloadCombi0 = handleDownloadException(ex,
                        downloadCombi0,
                        numberOfRetries,
                        downloadFailureCallback);
                    //no need to duplicate assertions at the beginning of
                    //download
                if(downloadCombi0 == null) {
                    //downloadFailureCallback decided that download ought to be
                    //canceled
                    return false;
                }
                numberOfRetries += 1;
            }
        }
        return true;
    }

    @SuppressWarnings("NestedAssignment")
    protected boolean download(DownloadCombi downloadCombi,
            boolean skipMD5SumCheck,
            DownloadFailureCallback downloadFailureCallback,
            MD5SumCheckUnequalsCallback mD5SumCheckUnequalsCallback,
            DownloadEmptyCallback downloadEmptyCallback) throws IOException,
            ExtractionException {
        assert downloadCombi != null;
        assert downloadCombi.getDownloadURL() != null;
        assert downloadCombi.getDownloadTarget() != null;
        assert downloadCombi.getExtractionMode() != null;
        assert downloadCombi.getExtractionLocation() != null;
        assert downloadCombi.getMd5Sum() != null;
            //all 5 values have been checked in downloadFile with helpful
            //feedback in case they're null
        boolean needDownload;
        if(skipMD5SumCheck) {
            needDownload = !new File(downloadCombi.getDownloadTarget()).exists();
        }else {
            needDownload = true;
            if(!downloadCombi.getMd5Sum().isEmpty() && new File(downloadCombi.getDownloadTarget()).exists()) {
                LOGGER.debug(String.format("reading download file '%s' for MD5 sum calculation",
                        downloadCombi.getDownloadTarget()));
                String md5 = DigestUtils.md5Hex(new BufferedInputStream(new FileInputStream(downloadCombi.getDownloadTarget())));
                if(downloadCombi.getMd5Sum().equals(md5)) {
                    LOGGER.debug(String.format("MD5 sum %s of download file '%s' matches",
                            downloadCombi.getMd5Sum(),
                            downloadCombi.getDownloadTarget()));
                    needDownload = false;
                }else {
                    LOGGER.debug(String.format("MD5 sum %s of download file '%s' doesn't match (should be %s), requesting new download",
                            md5,
                            downloadCombi.getDownloadTarget(),
                            downloadCombi.getMd5Sum()));
                }
            }
        }
        if(isCanceled()) {
            LOGGER.debug(String.format("canceling download of %s because the downloader has been canceled",
                    downloadCombi.getDownloadURL()));
            return false;
        }
        LOGGER.debug(String.format("needDownload: %s",
                String.valueOf(needDownload)));
        if(needDownload) {
            boolean success = false;
            int numberOfRetriesMD5Sum = 0;
            int numberOfRetriesEmpty = 0;
            while(!success) {
                URL downloadURLURL = new URL(downloadCombi.getDownloadURL());
                try (FileOutputStream out = new FileOutputStream(downloadCombi.getDownloadTarget());
                        InputStream downloadURLInputStream = downloadURLURL.openStream();
                ) {
                    LOGGER.debug(String.format("downloading from URL '%s' into file '%s'",
                            downloadCombi.getDownloadURL(),
                            downloadCombi.getDownloadTarget()));
                    IOUtils.copy(downloadURLInputStream,
                            out);
                }
                if(isCanceled()) {
                    LOGGER.debug(String.format("canceling download of %s because the downloader has been canceled",
                            downloadCombi.getDownloadURL()));
                    return false;
                }
                String downloadTargetContent = IOUtils.toString(new FileInputStream(downloadCombi.getDownloadTarget()),
                        Charsets.UTF_8);
                if(downloadTargetContent.isEmpty()) {
                    DownloadEmptyCallbackReation reaction = downloadEmptyCallback.run(numberOfRetriesEmpty);
                    if(reaction == DownloadEmptyCallbackReation.CANCEL) {
                        LOGGER.debug(String.format("canceling download of %s because the downloader has been canceled based on predefined decision for empty download",
                                downloadCombi.getDownloadURL()));
                        return false;
                    }
                    numberOfRetriesEmpty += 1;
                    continue;
                }
                if(downloadCombi.getMd5Sum().isEmpty()) {
                    success = true;
                }else {
                    LOGGER.debug(String.format("calculating MD5 checksum for download target file '%s'",
                            downloadCombi.getDownloadTarget()));
                    String md5 = DigestUtils.md5Hex(new BufferedInputStream(new FileInputStream(downloadCombi.getDownloadTarget())));
                    if(downloadCombi.getMd5Sum().equals(md5)) {
                        success = true;
                    }else {
                        MD5SumCheckUnequalsCallbackReaction reaction = mD5SumCheckUnequalsCallback.run(downloadCombi.getMd5Sum(), //expectedMD5Sum
                                md5, //actualMD5Sum
                                numberOfRetriesMD5Sum //numberOfRetries
                        );
                        if(reaction == MD5SumCheckUnequalsCallbackReaction.CANCEL) {
                            LOGGER.debug(String.format("canceling download of %s because the downloader has been canceled based on predefined decision for md5 checksum mismatch",
                                    downloadCombi.getDownloadURL()));
                            return false;
                        }
                    }
                }
                numberOfRetriesMD5Sum += 1;
            }
        }
        if(isCanceled()) {
            LOGGER.debug(String.format("canceling download of %s because the downloader has been canceled",
                    downloadCombi.getDownloadURL()));
            return false;
        }
        if(downloadCombi.getExtractionMode() == ExtractionMode.EXTRACTION_MODE_NONE) {
            LOGGER.debug(String.format("nothing to do for extraction mode '%s', returning successfully",
                    ExtractionMode.EXTRACTION_MODE_NONE.getLabel()));
            return true;
        }
        File extractionDir = new File(downloadCombi.getExtractionLocation());
        if(extractionDir.exists() && !extractionDir.isDirectory()) {
            throw new IllegalArgumentException(String.format("extraction "
                    + "directory '%s' is an existing path which doesn't point "
                    + "to a directory",
                    extractionDir.getAbsolutePath()));
        }
        if(!extractionDir.exists() || (extractionDir.exists() && extractionDir.list().length == 0)) {
            FileInputStream fileInputStream = new FileInputStream(downloadCombi.getDownloadTarget());
            if(null == downloadCombi.getExtractionMode()) {
                //if extractionMode was EXTRACTION_MODE_NONE the method
                //would already have returned
                throw new IllegalArgumentException(String.format(
                        "extractionMode mustn't be null",
                        downloadCombi.getExtractionMode().getLabel()));
            }else {
                if(downloadCombi.getExtractionMode() == ExtractionMode.EXTRACTION_MODE_TAR_GZ
                        || downloadCombi.getExtractionMode() == ExtractionMode.EXTRACTION_MODE_TAR_XZ) {
                    InputStream compressedInputStream;
                    if(downloadCombi.getExtractionMode() == ExtractionMode.EXTRACTION_MODE_TAR_GZ) {
                        compressedInputStream = new GZIPInputStream(fileInputStream);
                    }else {
                        compressedInputStream = new XZCompressorInputStream(fileInputStream);
                    }
                    try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(compressedInputStream)) {
                        String extractionDirTar = extractionDir.getParent();
                        LOGGER.debug(String.format("extracting .tar.gz archive into '%s'",
                                extractionDirTar));
                        TarArchiveEntry entry;
                        while ((entry = (TarArchiveEntry)tarArchiveInputStream.getNextEntry()) != null) {
                            final File outputFile = new File(extractionDirTar, entry.getName());
                            if (entry.isDirectory()) {
                                LOGGER.trace(String.format("Attempting to write output directory %s.",
                                        outputFile.getAbsolutePath()));
                                if (!outputFile.exists()) {
                                    LOGGER.trace(String.format("Attempting to create output directory %s.",
                                            outputFile.getAbsolutePath()));
                                    Files.createDirectories(outputFile.toPath());
                                }
                            } else {
                                LOGGER.trace(String.format("Creating output file %s.",
                                        outputFile.getAbsolutePath()));
                                final File outputFileParent = outputFile.getParentFile();
                                if (!outputFileParent.exists()) {
                                    Files.createDirectories(outputFileParent.toPath());
                                }
                                try (OutputStream outputFileStream = new FileOutputStream(outputFile)) {
                                    IOUtils.copy(tarArchiveInputStream, outputFileStream);
                                }
                            }
                            //not the most efficient way, but certainly a
                            //comprehensive one
                            int modeOctal = Integer.parseInt(Integer.toOctalString(entry.getMode()));
                            Path outputFilePath = Paths.get(outputFile.getAbsolutePath());
                            StringBuilder permStringBuilder = new StringBuilder(9);
                            int modeUser = modeOctal / 100;
                            int modeGroup = (modeOctal % 100) / 10;
                            int modeOthers = modeOctal % 10;
                            //from http://stackoverflow.com/questions/34234598/how-to-convert-an-input-of-3-octal-numbers-into-chmod-permissions-into-binary
                            permStringBuilder.append((modeUser & 4) == 0 ? '-' : 'r')
                                    .append((modeUser & 2) == 0 ? '-' : 'w')
                                    .append((modeUser & 1) == 0 ? '-' : 'x')
                                    .append((modeGroup & 4) == 0 ? '-' : 'r')
                                    .append((modeGroup & 2) == 0 ? '-' : 'w')
                                    .append((modeGroup & 1) == 0 ? '-' : 'x')
                                    .append((modeOthers & 4) == 0 ? '-' : 'r')
                                    .append((modeOthers & 2) == 0 ? '-' : 'w')
                                    .append((modeOthers & 1) == 0 ? '-' : 'x');
                            String permString = permStringBuilder.toString();
                            Files.setPosixFilePermissions(outputFilePath, PosixFilePermissions.fromString(permString));
                            Files.setLastModifiedTime(outputFile.toPath(),
                                    FileTime.fromMillis(entry.getLastModifiedDate().getTime()));
                            LOGGER.trace(String.format("last modified time of file or directory '%s' is %s",
                                    outputFile.getAbsolutePath(),
                                    Files.getLastModifiedTime(outputFilePath)));
                        }
                    }
                }else if(downloadCombi.getExtractionMode() == ExtractionMode.EXTRACTION_MODE_ZIP) {
                    Files.createDirectories(extractionDir.toPath());
                    LOGGER.debug(String.format("extracting .zip archive into '%s'",
                            extractionDir));
                    try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(downloadCombi.getDownloadTarget()))) {
                        ZipEntry entry = zipIn.getNextEntry();
                        // iterates over entries in the zip file
                        while (entry != null) {
                            String filePath = extractionDir.getParent() + File.separator + entry.getName();
                            File fileParent = new File(filePath).getParentFile();
                            if(!fileParent.exists()) {
                                Files.createDirectories(fileParent.toPath());
                            }
                            if (!entry.isDirectory()) {
                                // if the entry is a file, extracts it
                                DownloadTools.extractFile(zipIn, filePath);
                            } else {
                                // if the entry is a directory, make the directory
                                File dir = new File(filePath);
                                Files.createDirectories(dir.toPath());
                            }
                            Files.setLastModifiedTime(Paths.get(filePath),
                                    entry.getLastModifiedTime());
                            LOGGER.trace(String.format("last modified time of file or directory '%s' is %s",
                                    filePath,
                                    Files.getLastModifiedTime(Paths.get(filePath))));
                            zipIn.closeEntry();
                            entry = zipIn.getNextEntry();
                        }
                    }
                }else {
                    //if extractionMode was EXTRACTION_MODE_NONE the method
                    //would already have returned
                    throw new IllegalArgumentException(String.format(
                            "extractionMode %s isn't supported",
                            downloadCombi.getExtractionMode().getLabel()));
                }
            }
        }else {
            if(!extractionDir.isDirectory()) {
                throw new ExtractionException(extractionDir);
            }
        }
        return true;
    }

    /**
     * Possibility for subclasses to a cancelation check.
     *
     * This implementation always returns {@code false}.
     *
     * @return {@code true} if the download ought to be canceled as soon as
     * possible, {@code false} otherwise
     */
    protected boolean isCanceled() {
        return false;
    }

    /**
     * Possibility for subclasses to retrieve data for a new
     * {@link DownloadCombi} (from the user, from a website, etc.).This implementation always returns {@code null}.
     *
     *
     * @param ex a reference to the exception which caused the failure
     * @param previousDownloadCombi a reference to the previous download combi
     * so that it can eventually be returned to indicate that download ought to
     * be retried
     * @param numberOfRetries the number of retries of the download after
     * previous failures
     * @param downloadFailureCallback a download failure callback to retrieve
     * the {@link DownloadFailureCallbackReation}
     * @return a new or the same {@link DownloadCombi} or {@code null} if the
     * download ought to be canceled
     */
    protected DownloadCombi handleDownloadException(Exception ex,
            DownloadCombi previousDownloadCombi,
            int numberOfRetries,
            DownloadFailureCallback downloadFailureCallback) {
        if(downloadFailureCallback.run(ex,
                numberOfRetries) == DownloadFailureCallbackReation.CANCEL) {
            return null;
        }else {
            return previousDownloadCombi;
        }
    }
}
