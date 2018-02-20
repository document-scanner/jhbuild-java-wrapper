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
import richtercloud.jhbuild.java.wrapper.download.AutoDownloader;

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

    public JHBuildJavaWrapperTest() {
    }

    @Test
    public void testCalculateParallelism() {
        LOGGER.info("testCalculateParallelism");
        int result = JHBuildJavaWrapper.calculateParallelism();
        assertTrue(result >= 1);
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testInit() throws IOException {
        LOGGER.info("testInit");
        File installationPrefixDir = Files.createTempDirectory("jhbuild-java-wrapper-test-prefix" //prefix
                ).toFile();
        File downloadDir = Files.createTempDirectory("jhbuild-java-wrapper-test-download" //prefix
                ).toFile();
        //test IllegalArgumentException for invalid parallelism values
        try {
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
                    true,//silenceStdout
                    true,//silenceStderr,
                    ActionOnMissingBinary.DOWNLOAD,
                    ActionOnMissingBinary.DOWNLOAD,
                    ActionOnMissingBinary.DOWNLOAD,
                    ActionOnMissingBinary.DOWNLOAD,
                    ActionOnMissingBinary.DOWNLOAD,
                    ActionOnMissingBinary.DOWNLOAD,
                    ActionOnMissingBinary.DOWNLOAD,
                    0);
            fail("IllegalArgumentException expected");
        }catch(IllegalArgumentException expected) {
        }
        int parallelism = RANDOM.nextInt();
        while(parallelism >= 1) {
            parallelism = RANDOM.nextInt();
        }
        LOGGER.debug(String.format("testing parallelism %d",
                parallelism));
        try {
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
                    true,//silenceStdout
                    true,//silenceStderr,
                    ActionOnMissingBinary.DOWNLOAD,
                    ActionOnMissingBinary.DOWNLOAD,
                    ActionOnMissingBinary.DOWNLOAD,
                    ActionOnMissingBinary.DOWNLOAD,
                    ActionOnMissingBinary.DOWNLOAD,
                    ActionOnMissingBinary.DOWNLOAD,
                    ActionOnMissingBinary.DOWNLOAD,
                    parallelism);
            fail("expected IllegalArgumentException for invalid parallelism value");
        }catch(IllegalArgumentException expected) {
        }
    }

    /**
     * Test of cancelInstallModuleset method, of class JHBuildJavaWrapper.
     */
    @Test
    public void testCancelInstallModuleset() {
//        LOGGER.info("testCancelInstallModuleset");
//        System.out.println("cancelInstallModuleset");
//        JHBuildJavaWrapper instance = null;
//        instance.cancelInstallModuleset();
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of installModuleset method, of class JHBuildJavaWrapper.
     */
    @Test
    @PrepareForTest(BinaryTools.class)
    public void testInstallModulesetStringMissingInitBinaries() throws Exception {
        LOGGER.info("testInstallModulesetStringMissingInitBinaries");
        //null and empty module name tested in testInstallModulesetInputStreamString
        JHBuildJavaWrapper instance = generateDefaultTestInstance();
        mockStatic(BinaryTools.class);
        //don't throw BinaryValidationException automake check in order to be
        //able to go through version check (@TODO: requires more responses from
        //mock in order to test all branches)
        doThrow(new BinaryValidationException("unimportant message")).when(BinaryTools.class,
                "validateBinary",
                eq("cpan"),
                anyString(),
                anyString());
        doThrow(new BinaryValidationException("unimportant message")).when(BinaryTools.class,
                "validateBinary",
                eq("msgfmt"),
                anyString(),
                anyString());
        doThrow(new BinaryValidationException("unimportant message")).when(BinaryTools.class,
                "validateBinary",
                eq("git"),
                anyString(),
                anyString());
        //doThrow(new BinaryValidationException("unimportant message")).when(BinaryTools.class,
        //        "validateBinary",
        //        eq("python"),
        //        anyString(),
        //        anyString());
            //need to provide python binary by package manager or otherwise on
            //the system or CI service where the unit test are running because
            //Python can't be built correctly (`jhbuild` downloads fail due to
            //`<urlopen error [SSL: NO_CIPHERS_AVAILABLE] no ciphers available (_ssl.c:661)>`
            //asked
            //https://stackoverflow.com/questions/48941028/python-https-download-fails-due-to-urlopen-error-ssl-no-ciphers-available-n
            //for input)
        doThrow(new BinaryValidationException("unimportant message")).when(BinaryTools.class,
                "validateBinary",
                eq("jhbuild"),
                anyString(),
                anyString());
            //not necessary to configure mocked throwing of
            //BinaryValidationException for JHBuildJavaWrapper.checkLibPresence
            //since it works on the generated prefix which is an empty temporary
            //directory
        String moduleName = "postgresql-9.6.3";
        boolean result = instance.installModuleset(moduleName);
        boolean expResult = true;
        assertEquals(expResult,
                result);
    }

    /**
     * Tests failure on missing CC binary. Needs to be a separate method in
     * order to allow static mocking with PowerMockito which cannot be reset
     * (see
     * https://stackoverflow.com/questions/48831432/how-to-undo-reset-powermockito-mockstatic
     * for eventual solutions).
     */
    @Test
    @PrepareForTest(BinaryTools.class)
    public void testInstallModulesetStringInexistingCC() throws Exception {
        LOGGER.info("testInstallModulesetStringInexistingCC");
        JHBuildJavaWrapper instance = generateDefaultTestInstance();
        mockStatic(BinaryTools.class);
        doThrow(new BinaryValidationException("unimportant message")).when(BinaryTools.class,
                "validateBinary",
                eq("gcc"),
                anyString(),
                anyString());
        String moduleName = "postgresql-9.6.3";
        try {
            instance.installModuleset(moduleName);
            fail("expected MissingSystemBinaryException for inexisting C compiler");
        }catch(MissingSystemBinaryException expected) {
        }
    }

    @Test
    @PrepareForTest(BinaryTools.class)
    public void testInstallModulesetStringInexistingMake() throws Exception {
        LOGGER.info("testInstallModulesetStringInexistingMake");
        JHBuildJavaWrapper instance = generateDefaultTestInstance();
        mockStatic(BinaryTools.class);
        doThrow(new BinaryValidationException("unimportant message")).when(BinaryTools.class,
                "validateBinary",
                eq("make"),
                anyString(),
                anyString());
        String moduleName = "postgresql-9.6.3";
        try {
            instance.installModuleset(moduleName);
            fail("expected MissingSystemBinary for inexisting make");
        }catch(MissingSystemBinaryException expected) {
        }
    }

    /**
     * Test of installModuleset method, of class JHBuildJavaWrapper.
     */
    @Test
    public void testInstallModulesetInputStreamString() throws Exception {
        LOGGER.info("testInstallModulesetInputStreamString");
        InputStream modulesetInputStream = null;
        //test null and empty module name
        String moduleName = null;
        File installationPrefixDir = Files.createTempDirectory("jhbuild-java-wrapper-test-prefix" //prefix
                ).toFile();
        File downloadDir = Files.createTempDirectory("jhbuild-java-wrapper-test-download" //prefix
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
                true,//silenceStdout
                true,//silenceStderr,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                JHBuildJavaWrapper.calculateParallelism());
        try {
            instance.installModuleset(modulesetInputStream,
                    moduleName);
            fail("expected IllegalArgumentException for null modulesetInputStream");
        }catch(IllegalArgumentException expected) {
        }
        modulesetInputStream = mock(InputStream.class);
        try {
            instance.installModuleset(modulesetInputStream,
                    moduleName);
            fail("expected IllegalArgumentException for null moduleName");
        }catch(IllegalArgumentException expected) {
        }
        moduleName = "";
        try {
            instance.installModuleset(modulesetInputStream,
                    moduleName);
            fail("expected IllegalArgumentException for empty moduleName");
        }catch(IllegalArgumentException expected) {
        }
    }

    private JHBuildJavaWrapper generateDefaultTestInstance() throws IOException {
        File installationPrefixDir = Files.createTempDirectory("jhbuild-java-wrapper-test-prefix" //prefix
                ).toFile();
        LOGGER.debug(String.format("installationPrefixDir: %s",
                installationPrefixDir.getAbsolutePath()));
        File downloadDir = Files.createTempDirectory("jhbuild-java-wrapper-test-download" //prefix
                ).toFile();
        LOGGER.debug(String.format("downloadDir: %s",
                downloadDir.getAbsolutePath()));
        JHBuildJavaWrapper retValue = generateDefaultTestInstance(installationPrefixDir,
                downloadDir);
        return retValue;
    }

    private JHBuildJavaWrapper generateDefaultTestInstance(File installationPrefixDir,
            File downloadDir) throws IOException {
        JHBuildJavaWrapper retValue = new JHBuildJavaWrapper(installationPrefixDir,
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
                true,//silenceStdout
                true,//silenceStderr,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                ActionOnMissingBinary.DOWNLOAD,
                JHBuildJavaWrapper.calculateParallelism());
        return retValue;
    }
}
