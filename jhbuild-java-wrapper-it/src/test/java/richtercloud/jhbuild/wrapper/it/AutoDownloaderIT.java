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
package richtercloud.jhbuild.wrapper.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.GregorianCalendar;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.jhbuild.java.wrapper.ExtractionException;
import richtercloud.jhbuild.java.wrapper.ExtractionMode;
import richtercloud.jhbuild.java.wrapper.MD5SumCheckUnequalsCallback;
import richtercloud.jhbuild.java.wrapper.download.AutoDownloader;
import richtercloud.jhbuild.java.wrapper.download.DownloadCombi;
import richtercloud.jhbuild.java.wrapper.download.DownloadEmptyCallback;
import richtercloud.jhbuild.java.wrapper.download.DownloadFailureCallback;
import richtercloud.jhbuild.java.wrapper.download.Downloader;

/**
 *
 * @author richter
 */
public class AutoDownloaderIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(AutoDownloaderIT.class);

    @Test
    public void testTarballExtraction() throws IOException, ExtractionException {
        File installationPrefixDir = Files.createTempDirectory(String.format("%s-prefix",
                JHBuildJavaWrapperIT.class.getSimpleName())).toFile();
        LOGGER.debug(String.format("installation prefix directory: %s",
                installationPrefixDir.getAbsolutePath()));
        File downloadDir = Files.createTempDirectory(String.format("%s-download",
                JHBuildJavaWrapperIT.class.getSimpleName())).toFile();
        LOGGER.debug(String.format("download directory: %s",
                downloadDir.getAbsolutePath()));
        Downloader downloader = new AutoDownloader();
        //test that local resource can be "downloaded"
        File downloadTargetFile = new File(downloadDir,
                "libxml2-2.9.7.tar.gz");
        File extractionLocationFile = new File(downloadDir,
                "libxml2-2.9.7");
        assert !downloadTargetFile.exists();
        assert !extractionLocationFile.exists();
        DownloadCombi downloadCombi = new DownloadCombi(AutoDownloaderIT.class.getResource("/libxml2-2.9.7.tar.gz").toExternalForm(),
                downloadTargetFile.getAbsolutePath(),
                ExtractionMode.EXTRACTION_MODE_TAR_GZ,
                extractionLocationFile.getAbsolutePath(),
                "896608641a08b465098a40ddf51cefba");
        boolean result = downloader.downloadFile(downloadCombi,
                false,
                DownloadFailureCallback.RETRY_5_TIMES,
                MD5SumCheckUnequalsCallback.RETRY_5_TIMES,
                DownloadEmptyCallback.RETRY_5_TIMES);
        boolean expResult = true;
        assertEquals(expResult,
                result);
        assertTrue(downloadTargetFile.exists());
        assertTrue(extractionLocationFile.list().length > 0);
        Files.walk(extractionLocationFile.toPath())
                .filter(file -> !file.toFile().equals(extractionLocationFile)
                            //avoid checking source root because timestamp on it doesn't
                            //matter, only of files under it
                        && !file.toFile().isDirectory()
                            //@TODO: timestamps on directories aren't set
                            //correctly, but afaik that doesn't have influence
                            //on build processes, like make because they involve
                            //files only
                )
                .forEach(file ->
                assertTrue(String.format("last modified time %s of file or directory '%s' doesn't lie before 2018-01-01",
                        new Date(file.toFile().lastModified()),
                        file.toFile().getAbsolutePath()),
                        new Date(file.toFile().lastModified()).before(new GregorianCalendar(2018, 1, 1).getTime())));
        //with remote download
        File gzipDownloadTargetFile = new File(downloadDir,
                "gzip-1.9.zip");
        File gzipExtractionLocationFile = new File(downloadDir,
                "gzip-1.9");
        assert !gzipDownloadTargetFile.exists();
        assert !gzipExtractionLocationFile.exists();
        downloadCombi = new DownloadCombi("https://ftp.gnu.org/gnu/gzip/gzip-1.9.zip",
                gzipDownloadTargetFile.getAbsolutePath(),
                ExtractionMode.EXTRACTION_MODE_ZIP,
                gzipExtractionLocationFile.getAbsolutePath(),
                "80ba904f3efb4d12c0f20116ade885c8");
        result = downloader.downloadFile(downloadCombi,
                false,
                DownloadFailureCallback.RETRY_5_TIMES,
                MD5SumCheckUnequalsCallback.RETRY_5_TIMES,
                DownloadEmptyCallback.RETRY_5_TIMES);
        expResult = true;
        assertEquals(expResult,
                result);
        assertTrue(gzipDownloadTargetFile.exists());
        assertTrue(gzipExtractionLocationFile.list().length > 0);
        Files.walk(gzipExtractionLocationFile.toPath())
                .filter(file -> !file.toFile().equals(gzipExtractionLocationFile)
                            //avoid checking source root because timestamp on it doesn't
                            //matter, only of files under it
                        && !file.toFile().isDirectory()
                            //@TODO: timestamps on directories aren't set
                            //correctly, but afaik that doesn't have influence
                            //on build processes, like make because they involve
                            //files only
                )
                .forEach(file ->
                assertTrue(String.format("last modified time %s of file or directory '%s' doesn't lie before 2018-01-01",
                        new Date(file.toFile().lastModified()),
                        file.toFile().getAbsolutePath()),
                        new Date(file.toFile().lastModified()).before(new GregorianCalendar(2018, 1, 1).getTime())));
    }
}
