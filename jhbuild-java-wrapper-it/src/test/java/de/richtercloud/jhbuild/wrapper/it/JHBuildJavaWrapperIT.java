/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.richtercloud.jhbuild.wrapper.it;

import de.richtercloud.jhbuild.java.wrapper.ActionOnMissingBinary;
import de.richtercloud.jhbuild.java.wrapper.ArchitectureNotRecognizedException;
import de.richtercloud.jhbuild.java.wrapper.BuildFailureException;
import de.richtercloud.jhbuild.java.wrapper.ExtractionException;
import de.richtercloud.jhbuild.java.wrapper.JHBuildJavaWrapper;
import de.richtercloud.jhbuild.java.wrapper.MissingSystemBinaryException;
import de.richtercloud.jhbuild.java.wrapper.ModuleBuildFailureException;
import de.richtercloud.jhbuild.java.wrapper.OSNotRecognizedException;
import de.richtercloud.jhbuild.java.wrapper.download.AutoDownloader;
import de.richtercloud.jhbuild.java.wrapper.download.DownloadException;
import de.richtercloud.jhbuild.java.wrapper.download.Downloader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richter
 */
public class JHBuildJavaWrapperIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(JHBuildJavaWrapperIT.class);

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void testExampleBuild() throws IOException,
            OSNotRecognizedException,
            ArchitectureNotRecognizedException,
            ExtractionException,
            InterruptedException,
            MissingSystemBinaryException,
            BuildFailureException,
            ModuleBuildFailureException,
            DownloadException {
        File installationPrefixDir = Files.createTempDirectory(String.format("%s-prefix",
                JHBuildJavaWrapperIT.class.getSimpleName())).toFile();
        LOGGER.debug(String.format("installation prefix directory: %s",
                installationPrefixDir.getAbsolutePath()));
        File downloadDir = Files.createTempDirectory(String.format("%s-download",
                JHBuildJavaWrapperIT.class.getSimpleName())).toFile();
        LOGGER.debug(String.format("download directory: %s",
                downloadDir.getAbsolutePath()));
        Downloader downloader = new AutoDownloader();
        JHBuildJavaWrapper jHBuildJavaWrapper = new JHBuildJavaWrapper(installationPrefixDir,
                downloadDir,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                downloader, //downloader
                false, //skipMD5SumCheck
                new ByteArrayOutputStream(), //stdoutOutputStream (saves log
                    //capacity on Travis CI service with 4MB log size limit and
                    //doesn't hurt because stdout and stderr are printed in
                    //BuildFailureException message)
                new ByteArrayOutputStream() //stderrOutputStream
        );
        InputStream modulesetFileInputStream = JHBuildJavaWrapper.class.getResourceAsStream("/moduleset-default.xml");
        assert modulesetFileInputStream != null;
        jHBuildJavaWrapper.installModuleset(modulesetFileInputStream,
                "postgresql-10.5");
        //omitted parameter causing fallback to default moduleset
        jHBuildJavaWrapper = new JHBuildJavaWrapper(installationPrefixDir,
                downloadDir,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                downloader, //downloader
                false, //skipMD5SumCheck
                new ByteArrayOutputStream(), //stdoutOutputStream (saves log
                    //capacity on Travis CI service with 4MB log size limit and
                    //doesn't hurt because stdout and stderr are printed in
                    //BuildFailureException message)
                new ByteArrayOutputStream() //stderrOutputStream
        );
        jHBuildJavaWrapper.installModuleset("postgresql-10.5");
    }
}
