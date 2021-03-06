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

/**
 * The list of supported OS is finite in order to avoid over-complicated testing
 * and match the usual offers of binaries available for download.
 *
 * @author richter
 */
public enum SupportedOS {
    LINUX_32,
    LINUX_64,
    WINDOWS_32,
    WINDOWS_64,
    MAC_OSX_64
}
