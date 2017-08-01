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
package richtercloud.jhbuild.java.wrapper;

import java.awt.Window;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.SwingWorker;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.swing.worker.get.wait.dialog.SwingWorkerGetWaitDialog;

/**
 *
 * @author richter
 */
public class DownloadTools {
    private final static Logger LOGGER = LoggerFactory.getLogger(DownloadTools.class);
    protected final static String MD5_SUM_CHECK_FAILED_RETRY = "Retry download";
    protected final static String MD5_SUM_CHECK_FAILED_ABORT = "Abort download";

    /**
     * Windows lies about 64-bit systems in order to make 32-bit programs work
     * on 64-bit systems. Taken from
     * http://stackoverflow.com/questions/4748673/how-can-i-check-the-bitness-of-my-os-using-java-j2se-not-os-arch.
     * @return {@link #WINDOWS_BITNESS_32} or {@link #WINDOWS_BITNESS_64}
     */
    public static WindowsBitness getWindowsBitness() {
        assert SystemUtils.IS_OS_WINDOWS;
        String arch = System.getenv("PROCESSOR_ARCHITECTURE");
        String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
        WindowsBitness realArch = arch != null && arch.endsWith("64")
                || wow64Arch != null && wow64Arch.endsWith("64")
                        ? WindowsBitness.WINDOWS_BITNESS_64
                        : WindowsBitness.WINDOWS_BITNESS_32;
        return realArch;
    }

    /**
     * Downloads a file a file located at {@code donwloadURL}, compares its MD5
     * checksum with {@code md5Sum} and extracts it into
     * {@code extractionLocation}. If the download or the verification failed
     * the caller can take actions in {@code mD5SumCheckUnequalsCallback} and
     * {@code downloadFailureCallback} callbacks.
     *
     * @param downloadURL
     * @param downloadTarget
     * @param extractionMode
     * @param extractionLocation
     * @param md5Sum
     * @param downloadDialogParent
     * @param downloadDialogTitle
     * @param downloadDialogLabelText
     * @param downloadDialogProgressBarText
     * @param skipMD5SumCheck
     * @param downloadFailureCallback
     * @param mD5SumCheckUnequalsCallback
     * @return {@code false} if the validation, download or extraction have been
     * canceled, otherwise {@code true}, but exception might have been thrown
     * @throws IOException
     * @throws ExtractionException
     */
    public static boolean downloadFile(DownloadCombi downloadCombi,
            Window downloadDialogParent,
            String downloadDialogTitle,
            String downloadDialogLabelText,
            String downloadDialogProgressBarText,
            boolean skipMD5SumCheck,
            DownloadFailureCallback downloadFailureCallback,
            MD5SumCheckUnequalsCallback mD5SumCheckUnequalsCallback) throws IOException,
            ExtractionException {
        DownloadCombi downloadCombi0 = downloadCombi;
        boolean success = false;
        while(!success) {
            try {
                boolean notCanceled = download(downloadCombi0,
                        downloadDialogParent,
                        downloadDialogTitle,
                        downloadDialogLabelText,
                        downloadDialogProgressBarText,
                        skipMD5SumCheck,
                        mD5SumCheckUnequalsCallback);
                if(!notCanceled) {
                    return false;
                }
                success = true;
            }catch (IOException | ExtractionException ex) {
                DownloadFailureCallbackReation reaction = downloadFailureCallback.run(ex);
                if(reaction == DownloadFailureCallbackReation.CANCEL) {
                    return false;
                }
                DownloadDialog downloadDialog = new DownloadDialog(downloadDialogParent);
                downloadDialog.setLocationRelativeTo(downloadDialogParent);
                downloadDialog.setVisible(true);
                if(downloadDialog.isCanceled()) {
                    return false;
                }
                downloadCombi0 = downloadDialog.getDownloadCombi();
            }
        }
        return true;
    }

    public static SupportedOS getCurrentOS() throws OSNotRecognizedException,
            ArchitectureNotRecognizedException {
        LOGGER.debug(String.format("system properties os.name is '%s' and "
                + "os.arch is '%s'",
                System.getProperty("os.name"),
                System.getProperty("os.arch")));
        if(SystemUtils.IS_OS_LINUX) {
            if("amd64".equals(SystemUtils.OS_ARCH)) {
                LOGGER.debug("assuming Linux 64-bit");
                return SupportedOS.LINUX_64;
            }else if("i386".equals(SystemUtils.OS_ARCH)) {
                LOGGER.debug("assuming Linux 32-bit");
                return SupportedOS.LINUX_32;
            }else {
                throw new ArchitectureNotRecognizedException("Linux");
            }
        }else if(SystemUtils.IS_OS_WINDOWS) {
            if(DownloadTools.getWindowsBitness() == WindowsBitness.WINDOWS_BITNESS_64) {
                LOGGER.debug("assuming Windows 64-bit");
                return SupportedOS.WINDOWS_64;
            }else if(DownloadTools.getWindowsBitness() == WindowsBitness.WINDOWS_BITNESS_32) {
                LOGGER.debug("assuming Windows 32-bit");
                return SupportedOS.WINDOWS_32;
            }else {
                throw new ArchitectureNotRecognizedException("Windows");
            }
        }else if(SystemUtils.IS_OS_MAC) {
            if("x84_86".equals(SystemUtils.OS_ARCH)) {
                LOGGER.debug("assuming Mac OSX 64-bit");
                return SupportedOS.MAC_OSX_64;
            }else {
                throw new ArchitectureNotRecognizedException("Mac OSX");
            }
        }
        throw new OSNotRecognizedException();
    }

    /**
     * One step in a download loop.
     * @param downloadURL
     * @param extractionDirPath the directory where the directory contained in the
     * MySQL tarball ought to be placed
     * @param md5Sum
     * @return {@code false} if the validation, download or extraction have been
     * canceled, otherwise {@code true}, but exception might have been thrown
     */
    private static boolean download(DownloadCombi downloadCombi,
            Window downloadDialogParent,
            String downloadDialogTitle,
            String downloadDialogLabelText,
            String downloadDialogProgressBarText,
            boolean skipMD5SumCheck,
            MD5SumCheckUnequalsCallback mD5SumCheckUnequalsCallback) throws IOException,
            ExtractionException {
        final SwingWorkerGetWaitDialog dialog = new SwingWorkerGetWaitDialog(downloadDialogParent,
                downloadDialogTitle,
                downloadDialogLabelText,
                downloadDialogProgressBarText);
        SwingWorker<Boolean, Void> downloadWorker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws FileNotFoundException,
                    IOException,
                    ExtractionException {
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
                if(dialog.isCanceled()) {
                    return false;
                }
                if(needDownload) {
                    boolean success = false;
                    while(!success) {
                        URL downloadURLURL = new URL(downloadCombi.getDownloadURL());
                        FileOutputStream out =
                                new FileOutputStream(downloadCombi.getDownloadTarget());
                        InputStream downloadURLInputStream = downloadURLURL.openStream();
                        LOGGER.debug(String.format("downloading from URL '%s' into file '%s'",
                                downloadCombi.getDownloadURL(),
                                downloadCombi.getDownloadTarget()));
                        IOUtils.copy(downloadURLInputStream,
                                out);
                        out.flush();
                        out.close();
                        downloadURLInputStream.close();
                        if(dialog.isCanceled()) {
                            return false;
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
                                        md5 //actualMD5Sum
                                );
                                if(reaction == MD5SumCheckUnequalsCallbackReaction.CANCEL) {
                                    return false;
                                }
                            }
                        }
                    }
                }
                if(dialog.isCanceled()) {
                    return false;
                }
                if(downloadCombi.getExtractionMode() == ExtractionMode.EXTRACTION_MODE_NONE) {
                    return true;
                }
                File extractionDir = new File(downloadCombi.getExtractionLocation());
                if(!extractionDir.exists() || (extractionDir.exists() && extractionDir.list().length == 0)) {
                    FileInputStream fileInputStream = new FileInputStream(downloadCombi.getDownloadTarget());
                    if(downloadCombi.getExtractionMode() == ExtractionMode.EXTRACTION_MODE_TAR_GZ) {
                        GZIPInputStream gZIPInputStream = new GZIPInputStream(fileInputStream);
                        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(gZIPInputStream);
                        String extractionDirTar = extractionDir.getParent();
                        LOGGER.debug(String.format("extracting .tar.gz archive into '%s'", extractionDirTar));
                        TarArchiveEntry entry = null;
                        while ((entry = (TarArchiveEntry)tarArchiveInputStream.getNextEntry()) != null) {
                            final File outputFile = new File(extractionDirTar, entry.getName());
                            if (entry.isDirectory()) {
                                LOGGER.trace(String.format("Attempting to write output directory %s.", outputFile.getAbsolutePath()));
                                if (!outputFile.exists()) {
                                    LOGGER.trace(String.format("Attempting to create output directory %s.", outputFile.getAbsolutePath()));
                                    if (!outputFile.mkdirs()) {
                                        throw new IllegalStateException(String.format("Couldn't create directory %s.", outputFile.getAbsolutePath()));
                                    }
                                }
                            } else {
                                LOGGER.trace(String.format("Creating output file %s.", outputFile.getAbsolutePath()));
                                final File outputFileParent = outputFile.getParentFile();
                                if (!outputFileParent.exists()) {
                                    if(!outputFileParent.mkdirs()) {
                                        throw new IOException(String.format("Couldn't create directory %s.", outputFileParent.getAbsolutePath()));
                                    }
                                }
                                final OutputStream outputFileStream = new FileOutputStream(outputFile);
                                IOUtils.copy(tarArchiveInputStream, outputFileStream);
                                outputFileStream.close();
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
                        }
                        tarArchiveInputStream.close();
                    }else if(downloadCombi.getExtractionMode() == ExtractionMode.EXTRACTION_MODE_ZIP) {
                        FileUtils.forceMkdir(extractionDir);
                        LOGGER.debug(String.format("extracting .zip archive into '%s'", extractionDir));
                        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(downloadCombi.getDownloadTarget()));
                        ZipEntry entry = zipIn.getNextEntry();
                        // iterates over entries in the zip file
                        while (entry != null) {
                            String filePath = extractionDir.getParent() + File.separator + entry.getName();
                            File fileParent = new File(filePath).getParentFile();
                            if(!fileParent.exists()) {
                                FileUtils.forceMkdir(fileParent);
                            }
                            if (!entry.isDirectory()) {
                                // if the entry is a file, extracts it
                                extractFile(zipIn, filePath);
                            } else {
                                // if the entry is a directory, make the directory
                                File dir = new File(filePath);
                                dir.mkdir();
                            }
                            zipIn.closeEntry();
                            entry = zipIn.getNextEntry();
                        }
                        zipIn.close();
                    }else {
                        //if extractionMode was EXTRACTION_MODE_NONE the method
                        //would already have returned
                        throw new IllegalArgumentException(String.format(
                                "extractionMode %s isn't supported",
                                downloadCombi.getExtractionMode().getLabel()));
                    }
                }else {
                    if(!extractionDir.isDirectory()) {
                        throw new ExtractionException(extractionDir);
                    }
                }
                return true;
            }

            @Override
            protected void done() {
                dialog.setVisible(false);
            }
        };
        downloadWorker.execute();
        dialog.setVisible(true);
        if(dialog.isCanceled()) {
            return false;
                //returning false here will result in another
                //MySQLDownloadDialog being displayed in which the whole
                //download action can be canceled
        }
        try {
            return downloadWorker.get();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }catch(ExecutionException ex) {
            if(ex.getCause() instanceof IOException) {
                throw (IOException)ex.getCause();
            }
            if(ex.getCause() instanceof ExtractionException) {
                throw (ExtractionException)ex.getCause();
            }
            throw new RuntimeException(ex);
        }
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        IOUtils.copy(zipIn, bos);
        bos.flush();
        bos.close();
    }

    private DownloadTools() {
    }
}
