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
import java.io.InputStream;
import java.nio.file.Files;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.jhbuild.java.wrapper.ActionOnMissingBinary;
import richtercloud.jhbuild.java.wrapper.ArchitectureNotRecognizedException;
import richtercloud.jhbuild.java.wrapper.BuildFailureException;
import richtercloud.jhbuild.java.wrapper.ExtractionException;
import richtercloud.jhbuild.java.wrapper.JHBuildJavaWrapper;
import richtercloud.jhbuild.java.wrapper.MissingSystemBinaryException;
import richtercloud.jhbuild.java.wrapper.ModuleBuildFailureException;
import richtercloud.jhbuild.java.wrapper.OSNotRecognizedException;
import richtercloud.jhbuild.java.wrapper.download.AutoDownloader;
import richtercloud.jhbuild.java.wrapper.download.DownloadException;
import richtercloud.jhbuild.java.wrapper.download.Downloader;

/**
 *
 * @author richter
 */
public class JHBuildJavaWrapperIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(JHBuildJavaWrapperIT.class);

    @Test
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
                true, //silenceStdout (saves log capacity on Travis CI service
                    //with 4MB log size limit and doesn't hurt because stdout
                    //and stderr are printed in BuildFailureException message)
                true //silenceStderr
        );
        InputStream modulesetFileInputStream = JHBuildJavaWrapper.class.getResourceAsStream("/moduleset-default.xml");
        assert modulesetFileInputStream != null;
        jHBuildJavaWrapper.installModuleset(modulesetFileInputStream,
                "postgresql-9.6.3");
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
                true, //silenceStdout (saves log capacity on Travis CI service
                    //with 4MB log size limit and doesn't hurt because stdout
                    //and stderr are printed in BuildFailureException message)
                true //silenceStderr
        );
        jHBuildJavaWrapper.installModuleset("postgresql-9.6.3");
    }
}
