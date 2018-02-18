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

import java.awt.Window;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.jhbuild.java.wrapper.ExtractionException;
import richtercloud.jhbuild.java.wrapper.MD5SumCheckUnequalsCallback;
import richtercloud.swing.worker.get.wait.dialog.SwingWorkerGetWaitDialog;

/**
 *
 * @author richter
 */
public class GUIDownloader extends AutoDownloader {
    private final static Logger LOGGER = LoggerFactory.getLogger(GUIDownloader.class);
    private final Window downloadDialogParent;
    private final String downloadDialogTitle;
    private final String downloadDialogLabelText;
    private final String downloadDialogProgressBarText;
    /**
     * The currently displayed download progress dialog.
     */
    private SwingWorkerGetWaitDialog dialog;

    public GUIDownloader(Window downloadDialogParent,
            String downloadDialogTitle,
            String downloadDialogLabelText,
            String downloadDialogProgressBarText) {
        this.downloadDialogParent = downloadDialogParent;
        this.downloadDialogTitle = downloadDialogTitle;
        this.downloadDialogLabelText = downloadDialogLabelText;
        this.downloadDialogProgressBarText = downloadDialogProgressBarText;
    }

    /**
     * One step in a download loop.
     * @param downloadURL
     * @param extractionDirPath the directory where the directory contained in the
     * MySQL tarball ought to be placed
     * @param md5Sum
     * @return {@code false} if the validation, download or extraction have been
     * canceled, otherwise {@code true}, but exception might have been thrown
     */
    @Override
    protected boolean download(DownloadCombi downloadCombi,
            boolean skipMD5SumCheck,
            DownloadFailureCallback downloadFailureCallback,
            MD5SumCheckUnequalsCallback mD5SumCheckUnequalsCallback) throws IOException,
            ExtractionException {
        dialog = new SwingWorkerGetWaitDialog(downloadDialogParent,
                downloadDialogTitle,
                downloadDialogLabelText,
                downloadDialogProgressBarText);
        SwingWorker<Boolean, Void> downloadWorker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws FileNotFoundException,
                    IOException,
                    ExtractionException {
                boolean retValue = GUIDownloader.super.download(downloadCombi,
                        skipMD5SumCheck,
                        downloadFailureCallback,
                        mD5SumCheckUnequalsCallback);
                return retValue;
            }

            @Override
            protected void done() {
                dialog.setVisible(false);
            }
        };
        downloadWorker.execute();
        dialog.setVisible(true);
        if(dialog.isCanceled()) {
            return false;
                //returning false here will result in another
                //MySQLDownloadDialog being displayed in which the whole
                //download action can be canceled
        }
        try {
            return downloadWorker.get();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }catch(ExecutionException ex) {
            if(ex.getCause() instanceof IOException) {
                throw (IOException)ex.getCause();
            }
            if(ex.getCause() instanceof ExtractionException) {
                throw (ExtractionException)ex.getCause();
            }
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected boolean isCanceled() {
        boolean retValue = dialog != null && dialog.isCanceled();
        return retValue;
    }

    @Override
    protected DownloadCombi handleDownloadException(Exception ex,
            DownloadCombi previousDownloadCombi,
            int numberOfRetries,
            DownloadFailureCallback downloadFailureCallback) {
        DownloadFailureCallbackReation reaction = downloadFailureCallback.run(ex,
                numberOfRetries);
        if(reaction == DownloadFailureCallbackReation.CANCEL) {
            return null;
        }
        DownloadDialog downloadDialog = new DownloadDialog(downloadDialogParent);
        downloadDialog.setLocationRelativeTo(downloadDialogParent);
        downloadDialog.setVisible(true);
        if(downloadDialog.isCanceled()) {
            return null;
        }
        DownloadCombi retValue = downloadDialog.getDownloadCombi();
        return retValue;
    }
}
