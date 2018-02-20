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

/**
 *
 * @author richter
 */
public class MissingSystemBinaryException extends Exception {
    private static final long serialVersionUID = 1L;

    public MissingSystemBinaryException(String binaryName,
            Exception cause) {
        super(String.format("The important binary %s is missing on your system. JHBuild wrapper isn't capable of installing it automatically, so you have to install it yourself",
                binaryName),
                cause);
    }
}
