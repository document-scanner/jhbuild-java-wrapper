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

/**
 *
 * @author richter
 */
public enum ExtractionMode {
    EXTRACTION_MODE_TAR_GZ(".tar.gz"),
    EXTRACTION_MODE_TAR_XZ(".tar.xz"),
    EXTRACTION_MODE_ZIP(".zip"),
    EXTRACTION_MODE_NONE("none");

    private final String label;

    private ExtractionMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
