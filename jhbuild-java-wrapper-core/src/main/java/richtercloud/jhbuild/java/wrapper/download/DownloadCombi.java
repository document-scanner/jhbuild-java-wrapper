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
package richtercloud.jhbuild.java.wrapper.download;

import richtercloud.jhbuild.java.wrapper.ExtractionMode;

/**
 *
 * @author richter
 */
public class DownloadCombi {
    private final String downloadURL;
    private final String downloadTarget;
    private final ExtractionMode extractionMode;
    private final String extractionLocation;
    private final String md5Sum;

    public DownloadCombi(String downloadURL,
            String downloadTarget,
            ExtractionMode extractionMode,
            String extractionLocation,
            String md5Sum) {
        this.downloadURL = downloadURL;
        this.downloadTarget = downloadTarget;
        this.extractionMode = extractionMode;
        this.extractionLocation = extractionLocation;
        this.md5Sum = md5Sum;
    }

    public String getDownloadURL() {
        return downloadURL;
    }

    public String getDownloadTarget() {
        return downloadTarget;
    }

    public ExtractionMode getExtractionMode() {
        return extractionMode;
    }

    public String getExtractionLocation() {
        return extractionLocation;
    }

    public String getMd5Sum() {
        return md5Sum;
    }
}
