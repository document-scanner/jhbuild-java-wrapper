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

/**
 *
 * @author richter
 */
public final class BinaryUtils {

    /**
     * Does what
     * {@link #validateBinary(java.lang.String, java.lang.String, java.lang.String) }
     * while using the environment variable {@code PATH} for {@code path}.
     * @param binary the binary to validate
     * @param name the name of the binary (used to provide comprehensive error
     *     messages)
     * @throws BinaryValidationException if the validation failed
     */
    public static void validateBinary(String binary,
            String name) throws BinaryValidationException {
        validateBinary(binary,
                name,
                System.getenv("PATH"));
    }

    /**
     * Checks a binary to be either a specification of a valid binary or a
     * binary name which can be found in the specified path. Valid means that it
     * is a file and can be executed. The check is performed for the binary
     * specification first, then it's tried as name with every element of
     * {@code path} which is split with {@link File#pathSeparator}.
     * @param binary the binary specification (path or name)
     * @param name the name of the binary (used to provide comprehensive error
     *     messages)
     * @param path the search path in the OS format (pathes separated with the
     *     OS' path separator)
     * @throws BinaryValidationException if the binary can't be found or is
     *     invalid
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public static void validateBinary(String binary,
            String name,
            String path) throws BinaryValidationException {
        if(binary == null || binary.isEmpty()) {
            throw new IllegalArgumentException(String.format("%s binary is null or empty",
                    name));
        }
        if(name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name mustn't be null or empty");
        }
        File binaryFile = new File(binary);
        if(binaryFile.exists()) {
            if(!binaryFile.isFile()) {
                throw new IllegalArgumentException(String.format("%s binary '%s' points to an existing location which is not a file",
                        name,
                        binary));
            }
            if(!binaryFile.canExecute()) {
                throw new IllegalArgumentException(String.format("%s binary '%s' points to an existing file can't be executed",
                        name,
                        binary));
            }
        }
        String[] pathSplits = path.split(File.pathSeparator);
        for(String pathSplit : pathSplits) {
            binaryFile = new File(pathSplit, binary);
            if(binaryFile.exists()
                    && binaryFile.isFile()
                    && binaryFile.canExecute()) {
                return;
            }
        }
        throw new BinaryValidationException(String.format("%s binary path '%s' points to an inexisting location or to a location which is not a file or can't be executed (directly or in path '%s')",
                name,
                binary,
                path));
    }

    private BinaryUtils() {
    }
}
