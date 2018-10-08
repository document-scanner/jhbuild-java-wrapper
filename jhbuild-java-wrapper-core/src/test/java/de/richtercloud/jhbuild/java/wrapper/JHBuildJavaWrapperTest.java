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
package de.richtercloud.jhbuild.java.wrapper;

import de.richtercloud.jhbuild.java.wrapper.download.AutoDownloader;
import de.richtercloud.jhbuild.java.wrapper.download.DownloadException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Random;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richter
 */
@RunWith(PowerMockRunner.class)
public class JHBuildJavaWrapperTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(JHBuildJavaWrapperTest.class);
    private final static Random RANDOM;
    static {
        long randomSeed = System.currentTimeMillis();
        LOGGER.debug(String.format("using random seed %d",
                randomSeed));
        RANDOM = new Random(randomSeed);
    }
    private static final String TEST_PREFIX = "jhbuild-java-wrapper-test-prefix";
    private static final String DOWNLOAD_PREFIX = "jhbuild-java-wrapper-test-download";
    private static final String VALIDATE_BINARY = "validateBinary";

    @Test
    public void testCalculateParallelism() {
        LOGGER.info("testCalculateParallelism");
        int result = JHBuildJavaWrapper.calculateParallelism();
        assertTrue(result >= 1);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testInitInvalidParallelismZero() throws IOException {
        File installationPrefixDir = Files.createTempDirectory(TEST_PREFIX //prefix
        ).toFile();
        File downloadDir = Files.createTempDirectory(DOWNLOAD_PREFIX //prefix
        ).toFile();
        //test IllegalArgumentException for invalid parallelism values
        new JHBuildJavaWrapper(installationPrefixDir,
                downloadDir,
                JHBuildJavaWrapper.GIT_DEFAULT,
                JHBuildJavaWrapper.JHBUILD_DEFAULT,
                JHBuildJavaWrapper.SH_DEFAULT,
                JHBuildJavaWrapper.MAKE_DEFAULT,
                JHBuildJavaWrapper.PYTHON_DEFAULT,
                JHBuildJavaWrapper.CC_DEFAULT,
                JHBuildJavaWrapper.MSGFMT_DEFAULT,
                JHBuildJavaWrapper.CPAN_DEFAULT,
                JHBuildJavaWrapper.PATCH_DEFAULT,
                JHBuildJavaWrapper.OPENSSL_DEFAULT,
                new AutoDownloader(),
                false, //skipMD5SumCheck
                new ByteArrayOutputStream(), //stdoutOutputStream
                new ByteArrayOutputStream(), //stderrOutputStream
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                0);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testInitInvalidParallelismNegative() throws IOException {
        File installationPrefixDir = Files.createTempDirectory(TEST_PREFIX //prefix
        ).toFile();
        File downloadDir = Files.createTempDirectory(DOWNLOAD_PREFIX //prefix
        ).toFile();
        int parallelism = RANDOM.nextInt();
        while(parallelism >= 1) {
            parallelism = RANDOM.nextInt();
        }
        LOGGER.debug(String.format("testing parallelism %d",
                parallelism));
        new JHBuildJavaWrapper(installationPrefixDir,
                downloadDir,
                JHBuildJavaWrapper.GIT_DEFAULT,
                JHBuildJavaWrapper.JHBUILD_DEFAULT,
                JHBuildJavaWrapper.SH_DEFAULT,
                JHBuildJavaWrapper.MAKE_DEFAULT,
                JHBuildJavaWrapper.PYTHON_DEFAULT,
                JHBuildJavaWrapper.CC_DEFAULT,
                JHBuildJavaWrapper.MSGFMT_DEFAULT,
                JHBuildJavaWrapper.CPAN_DEFAULT,
                JHBuildJavaWrapper.PATCH_DEFAULT,
                JHBuildJavaWrapper.OPENSSL_DEFAULT,
                new AutoDownloader(),
                false, //skipMD5SumCheck
                new ByteArrayOutputStream(), //stdoutOutputStream
                new ByteArrayOutputStream(), //stderrOutputStream
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                parallelism);
    }

    /**
     * Tests failure on missing CC binary. Needs to be a separate method in
     * order to allow static mocking with PowerMockito which cannot be reset
     * (see
     * https://stackoverflow.com/questions/48831432/how-to-undo-reset-powermockito-mockstatic
     * for eventual solutions).
     *
     * @throws IOException if such an exception occurs
     * @throws Exception if such an exception occurs
     */
    @Test(expected = MissingSystemBinaryException.class)
    @PrepareForTest(BinaryUtils.class)
    public void testInstallModulesetStringInexistingCC() throws IOException,
            Exception {
        LOGGER.info("testInstallModulesetStringInexistingCC");
        JHBuildJavaWrapper instance = generateDefaultTestInstance();
        mockStatic(BinaryUtils.class);
        doThrow(new BinaryValidationException("simulating gcc not present")).when(BinaryUtils.class,
                VALIDATE_BINARY,
                eq("gcc"),
                anyString(),
                anyString());
        String moduleName = "postgresql-9.6.3";
        instance.installModuleset(moduleName);
    }

    @Test(expected = MissingSystemBinaryException.class)
    @PrepareForTest(BinaryUtils.class)
    public void testInstallModulesetStringInexistingMake() throws Exception {
        LOGGER.info("testInstallModulesetStringInexistingMake");
        JHBuildJavaWrapper instance = generateDefaultTestInstance();
        mockStatic(BinaryUtils.class);
        doThrow(new BinaryValidationException("simulating make not present")).when(BinaryUtils.class,
                VALIDATE_BINARY,
                eq("make"),
                anyString(),
                anyString());
        String moduleName = "postgresql-9.6.3";
        instance.installModuleset(moduleName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInstallModulesetInputStreamStringNull() throws IOException,
            ExtractionException,
            InterruptedException,
            MissingSystemBinaryException,
            BuildFailureException,
            ModuleBuildFailureException,
            DownloadException {
        LOGGER.info("testInstallModulesetInputStreamString");
        InputStream modulesetInputStream = null;
        //test null and empty module name
        String moduleName = null;
        File installationPrefixDir = Files.createTempDirectory(TEST_PREFIX //prefix
        ).toFile();
        File downloadDir = Files.createTempDirectory(DOWNLOAD_PREFIX //prefix
        ).toFile();
        JHBuildJavaWrapper instance = new JHBuildJavaWrapper(installationPrefixDir,
                downloadDir,
                JHBuildJavaWrapper.GIT_DEFAULT,
                JHBuildJavaWrapper.JHBUILD_DEFAULT,
                JHBuildJavaWrapper.SH_DEFAULT,
                JHBuildJavaWrapper.MAKE_DEFAULT,
                JHBuildJavaWrapper.PYTHON_DEFAULT,
                JHBuildJavaWrapper.CC_DEFAULT,
                JHBuildJavaWrapper.MSGFMT_DEFAULT,
                JHBuildJavaWrapper.CPAN_DEFAULT,
                JHBuildJavaWrapper.PATCH_DEFAULT,
                JHBuildJavaWrapper.OPENSSL_DEFAULT,
                new AutoDownloader(),
                false, //skipMD5SumCheck
                new ByteArrayOutputStream(), //stdoutOutputStream
                new ByteArrayOutputStream(), //stderrOutputStream
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                JHBuildJavaWrapper.calculateParallelism());
        instance.installModuleset(modulesetInputStream,
                moduleName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInstallModulesetInputStreamStringModuleNameNull() throws IOException,
            InterruptedException,
            BuildFailureException,
            ModuleBuildFailureException,
            DownloadException,
            MissingSystemBinaryException,
            ExtractionException {
        String moduleName = null;
        File installationPrefixDir = Files.createTempDirectory(TEST_PREFIX //prefix
        ).toFile();
        File downloadDir = Files.createTempDirectory(DOWNLOAD_PREFIX //prefix
        ).toFile();
        InputStream modulesetInputStream = mock(InputStream.class);
        JHBuildJavaWrapper instance = new JHBuildJavaWrapper(installationPrefixDir,
                downloadDir,
                JHBuildJavaWrapper.GIT_DEFAULT,
                JHBuildJavaWrapper.JHBUILD_DEFAULT,
                JHBuildJavaWrapper.SH_DEFAULT,
                JHBuildJavaWrapper.MAKE_DEFAULT,
                JHBuildJavaWrapper.PYTHON_DEFAULT,
                JHBuildJavaWrapper.CC_DEFAULT,
                JHBuildJavaWrapper.MSGFMT_DEFAULT,
                JHBuildJavaWrapper.CPAN_DEFAULT,
                JHBuildJavaWrapper.PATCH_DEFAULT,
                JHBuildJavaWrapper.OPENSSL_DEFAULT,
                new AutoDownloader(),
                false, //skipMD5SumCheck
                new ByteArrayOutputStream(), //stdoutOutputStream
                new ByteArrayOutputStream(), //stderrOutputStream
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                JHBuildJavaWrapper.calculateParallelism());
        instance.installModuleset(modulesetInputStream,
                moduleName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInstallModulesetInputStreamStringModuleNameEmpty() throws InterruptedException,
            BuildFailureException,
            IOException,
            ModuleBuildFailureException,
            DownloadException,
            MissingSystemBinaryException,
            ExtractionException {
        File installationPrefixDir = Files.createTempDirectory(TEST_PREFIX //prefix
        ).toFile();
        File downloadDir = Files.createTempDirectory(DOWNLOAD_PREFIX //prefix
        ).toFile();
        String moduleName = "";
        InputStream modulesetInputStream = mock(InputStream.class);
        JHBuildJavaWrapper instance = new JHBuildJavaWrapper(installationPrefixDir,
                downloadDir,
                JHBuildJavaWrapper.GIT_DEFAULT,
                JHBuildJavaWrapper.JHBUILD_DEFAULT,
                JHBuildJavaWrapper.SH_DEFAULT,
                JHBuildJavaWrapper.MAKE_DEFAULT,
                JHBuildJavaWrapper.PYTHON_DEFAULT,
                JHBuildJavaWrapper.CC_DEFAULT,
                JHBuildJavaWrapper.MSGFMT_DEFAULT,
                JHBuildJavaWrapper.CPAN_DEFAULT,
                JHBuildJavaWrapper.PATCH_DEFAULT,
                JHBuildJavaWrapper.OPENSSL_DEFAULT,
                new AutoDownloader(),
                false, //skipMD5SumCheck
                new ByteArrayOutputStream(), //stdoutOutputStream
                new ByteArrayOutputStream(), //stderrOutputStream
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                JHBuildJavaWrapper.calculateParallelism());
        instance.installModuleset(modulesetInputStream,
                moduleName);
    }

    private JHBuildJavaWrapper generateDefaultTestInstance() throws IOException {
        File installationPrefixDir = Files.createTempDirectory(TEST_PREFIX //prefix
                ).toFile();
        LOGGER.debug(String.format("installationPrefixDir: %s",
                installationPrefixDir.getAbsolutePath()));
        File downloadDir = Files.createTempDirectory(DOWNLOAD_PREFIX //prefix
                ).toFile();
        LOGGER.debug(String.format("downloadDir: %s",
                downloadDir.getAbsolutePath()));
        return generateDefaultTestInstance(installationPrefixDir,
                downloadDir);
    }

    private JHBuildJavaWrapper generateDefaultTestInstance(File installationPrefixDir,
            File downloadDir) throws IOException {
        return new JHBuildJavaWrapper(installationPrefixDir,
                downloadDir,
                JHBuildJavaWrapper.GIT_DEFAULT,
                JHBuildJavaWrapper.JHBUILD_DEFAULT,
                JHBuildJavaWrapper.SH_DEFAULT,
                JHBuildJavaWrapper.MAKE_DEFAULT,
                JHBuildJavaWrapper.PYTHON_DEFAULT,
                JHBuildJavaWrapper.CC_DEFAULT,
                JHBuildJavaWrapper.MSGFMT_DEFAULT,
                JHBuildJavaWrapper.CPAN_DEFAULT,
                JHBuildJavaWrapper.PATCH_DEFAULT,
                JHBuildJavaWrapper.OPENSSL_DEFAULT,
                new AutoDownloader(),
                false, //skipMD5SumCheck
                new ByteArrayOutputStream(), //stdoutOutputStream
                new ByteArrayOutputStream(), //stderrOutputStream
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                JHBuildJavaWrapper.calculateParallelism());
    }
}
