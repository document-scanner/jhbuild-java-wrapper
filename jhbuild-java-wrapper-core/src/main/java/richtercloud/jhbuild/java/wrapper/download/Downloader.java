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

import java.io.IOException;
import richtercloud.jhbuild.java.wrapper.ExtractionException;
import richtercloud.jhbuild.java.wrapper.MD5SumCheckUnequalsCallback;

/**
 *
 * @author richter
 */
public interface Downloader {

    /**
     * Downloads a file a file located at {@code donwloadURL}, compares its MD5
     * checksum with {@code md5Sum} and extracts it into
     * {@code extractionLocation}. If the download or the verification failed
     * the caller can take actions in {@code mD5SumCheckUnequalsCallback} and
     * {@code downloadFailureCallback} callbacks.
     *
     * @param downloadURL
     * @param downloadTarget
     * @param extractionMode
     * @param extractionLocation
     * @param md5Sum
     * @param downloadDialogParent
     * @param downloadDialogTitle
     * @param downloadDialogLabelText
     * @param downloadDialogProgressBarText
     * @param skipMD5SumCheck
     * @param downloadFailureCallback
     * @param mD5SumCheckUnequalsCallback
     * @return {@code false} if the validation, download or extraction have been
     * canceled, otherwise {@code true}, but exception might have been thrown
     * @throws IOException
     * @throws ExtractionException
     * @throws DownloadException wraps any unexpected exception which might be
     * of various kind given the fact that subclasses might be GUI based and
     * execute on different threads
     */
    boolean downloadFile(DownloadCombi downloadCombi,
            boolean skipMD5SumCheck,
            DownloadFailureCallback downloadFailureCallback,
            MD5SumCheckUnequalsCallback mD5SumCheckUnequalsCallback,
            DownloadEmptyCallback downloadEmptyCallback) throws IOException,
            ExtractionException,
            DownloadException;
}
