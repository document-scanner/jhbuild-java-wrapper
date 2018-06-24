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
package de.richtercloud.jhbuild.java.wrapper.download;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.richtercloud.jhbuild.java.wrapper.ArchitectureNotRecognizedException;
import de.richtercloud.jhbuild.java.wrapper.OSNotRecognizedException;
import de.richtercloud.jhbuild.java.wrapper.SupportedOS;
import de.richtercloud.jhbuild.java.wrapper.WindowsBitness;

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

    public static void extractFile(ZipInputStream zipInputStream, String filePath) throws IOException {
        if(zipInputStream == null) {
            throw new IllegalArgumentException("zipInputStream mustn't be null");
        }
        if(filePath == null) {
            throw new IllegalArgumentException("filePath mustn't be null");
        }
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            IOUtils.copy(zipInputStream, bos);
            bos.flush();
        }
    }

    private DownloadTools() {
    }
}
