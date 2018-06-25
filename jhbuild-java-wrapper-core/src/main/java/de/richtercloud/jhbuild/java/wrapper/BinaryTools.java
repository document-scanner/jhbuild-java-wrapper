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
package de.richtercloud.jhbuild.java.wrapper;

import java.io.File;

/**
 *
 * @author richter
 */
public class BinaryTools {

    public static void validateBinary(String binary,
            String name) throws BinaryValidationException {
        validateBinary(binary,
                name,
                System.getenv("PATH"));
    }

    public static void validateBinary(String binary,
            String name,
            String path) throws BinaryValidationException {
        if(binary == null || binary.isEmpty()) {
            throw new IllegalArgumentException(String.format("%s binary path is null or empty",
                    name));
        }
        if(name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name mustn't be null or empty");
        }
        if(new File(binary).exists()) {
            return;
        }
        String[] pathSplits = path.split(File.pathSeparator);
        for(String pathSplit : pathSplits) {
            if(new File(pathSplit, binary).exists()) {
                return;
            }
        }
        throw new BinaryValidationException(String.format("%s binary path points to an inexisting location and can't be found in PATH",
                name));
    }

    private BinaryTools() {
    }
}
