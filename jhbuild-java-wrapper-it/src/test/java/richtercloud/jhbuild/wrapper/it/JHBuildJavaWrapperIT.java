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
import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import richtercloud.jhbuild.java.wrapper.ActionOnMissingBinary;
import richtercloud.jhbuild.java.wrapper.ArchitectureNotRecognizedException;
import richtercloud.jhbuild.java.wrapper.BuildFailureException;
import richtercloud.jhbuild.java.wrapper.ExtractionException;
import richtercloud.jhbuild.java.wrapper.JHBuildJavaWrapper;
import richtercloud.jhbuild.java.wrapper.MissingSystemBinary;
import richtercloud.jhbuild.java.wrapper.OSNotRecognizedException;

/**
 *
 * @author richter
 */
public class JHBuildJavaWrapperIT {

    @Test
    public void testExampleBuild() throws IOException,
            OSNotRecognizedException,
            ArchitectureNotRecognizedException,
            ExtractionException,
            InterruptedException,
            MissingSystemBinary,
            BuildFailureException {
        File installationPrefixDir = Files.createTempDirectory(JHBuildJavaWrapperIT.class.getSimpleName()).toFile();
        File downloadDir = new File(SystemUtils.getUserHome(), "sources");
        JHBuildJavaWrapper jHBuildJavaWrapper = new JHBuildJavaWrapper(installationPrefixDir,
                downloadDir,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                null, //downloadDialogParent
                false, //skipMD5SumCheck
                false, //silenceStdout
                false //silenceStderr
        );
        InputStream modulesetFileInputStream = JHBuildJavaWrapper.class.getResourceAsStream("/moduleset-default.xml");
        assert modulesetFileInputStream != null;
        jHBuildJavaWrapper.installModuleset(modulesetFileInputStream,
                "postgresql-9.6.3");
        //one stream silenced (might block if output retrieval isn't handled
        //correctly) + omitted parameter causing fallback to default moduleset
        jHBuildJavaWrapper = new JHBuildJavaWrapper(installationPrefixDir,
                downloadDir,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                null, //downloadDialogParent
                false, //skipMD5SumCheck
                true, //silenceStdout
                false //silenceStderr
        );
        jHBuildJavaWrapper.installModuleset("postgresql-9.6.3");
    }
}
