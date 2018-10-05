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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.Test;

/**
 *
 * @author richter
 */
public class BinaryUtilsTest {
    private static final String SOME_BINARY = "someBinary";

    @Test(expected = IllegalArgumentException.class)
    public void testValidateBinaryBinaryNull() throws BinaryValidationException {
        String binary = null;
        String name = "";
        String path = "";
        BinaryUtils.validateBinary(binary, name, path);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateBinaryBinaryEmpty() throws BinaryValidationException {
        String binary = "";
        String name = "";
        String path = "";
        BinaryUtils.validateBinary(binary, name, path);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateBinaryNameNull() throws BinaryValidationException {
        String binary = "validBinary";
        String name = null;
        String path = "";
        BinaryUtils.validateBinary(binary, name, path);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateBinaryNameEmpty() throws BinaryValidationException {
        String binary = "validBinary";
        String name = "";
        String path = "";
        BinaryUtils.validateBinary(binary, name, path);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateBinaryExistingPathNotFile() throws BinaryValidationException,
            IOException {
        Path existingBinaryNoFile = Files.createTempDirectory(BinaryUtilsTest.class.getSimpleName() // prefix
                );
        String binary = existingBinaryNoFile.toString();
        String name = SOME_BINARY;
        String path = "";
        BinaryUtils.validateBinary(binary, name, path);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateBinaryExistingPathCannotRead() throws BinaryValidationException,
            IOException {
        Path existingBinaryNoFile = Files.createTempFile(BinaryUtilsTest.class.getSimpleName(), // prefix
                "existingBinaryCannotRead", //suffix
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r--r--r--")));
        String binary = existingBinaryNoFile.toString();
        String name = SOME_BINARY;
        String path = "";
        BinaryUtils.validateBinary(binary, name, path);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void testValidateBinaryExistingPath() throws BinaryValidationException,
            IOException {
        Path existingBinaryNoFile = Files.createTempFile(BinaryUtilsTest.class.getSimpleName(), // prefix
                "existingBinaryCannotRead", //suffix
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r-xr--r--")));
        String binary = existingBinaryNoFile.toString();
        String name = SOME_BINARY;
        String path = "";
        BinaryUtils.validateBinary(binary, name, path);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void testValidateBinaryFoundInPath() throws BinaryValidationException,
            IOException {
        Path binaryFoundInPath = Files.createTempFile(BinaryUtilsTest.class.getSimpleName(), // prefix
                "foundInPath", //suffix
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r-xr--r--")));
        String binary = binaryFoundInPath.toString();
        String name = SOME_BINARY;
        String path = String.join(File.pathSeparator,
                Paths.get("/", "bin").toString(),
                binaryFoundInPath.getParent().toString());
        BinaryUtils.validateBinary(binary, name, path);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void testValidateBinaryNotFoundInPath() throws BinaryValidationException,
            IOException {
        Path binaryNotFoundInPath = Files.createTempFile(BinaryUtilsTest.class.getSimpleName(), // prefix
                "notFoundInPath", //suffix
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r-xr--r--")));
        String binary = binaryNotFoundInPath.toString();
        String name = SOME_BINARY;
        String path = Paths.get("/", "bin").toString();
        BinaryUtils.validateBinary(binary, name, path);
    }
}
