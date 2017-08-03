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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.message.handler.ExceptionMessage;
import richtercloud.message.handler.IssueHandler;

/**
 *
 * @author richter
 */
public class OutputReaderThread extends Thread {
    private final static Logger LOGGER = LoggerFactory.getLogger(OutputReaderThread.class);
    private final StringBuilder outputBuilder = new StringBuilder();
    /**
     * The process stream to read ({@code stdout} or {@code stderr}).
     */
    private final InputStream processStream;
    private final IssueHandler issueHandler;

    /**
     * Creates a new {@code OutputReaderThread}.
     *
     * @param processStream the process output stream to read from
     * @param issueHandler the issue handler for exceptions in this thread
     */
    public OutputReaderThread(InputStream processStream,
            IssueHandler issueHandler) {
        this.processStream = processStream;
        this.issueHandler = issueHandler;
    }

    /**
     * Creates a new {@code OutputReaderThread}.
     *
     * @param processStream the process output stream to read from
     * @param issueHandler the issue handler for exceptions in this thread
     * @param name the name of the thread (see
     * {@link Thread#Thread(java.lang.String) } for details)
     */
    public OutputReaderThread(InputStream processStream,
            IssueHandler issueHandler,
            String name) {
        super(name);
        this.processStream = processStream;
        this.issueHandler = issueHandler;
    }

    public StringBuilder getOutputBuilder() {
        return outputBuilder;
    }

    @Override
    public void run() {
        try {
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(processStream));
            //testing for outputReader.ready causes thread to terminate
            //before the end of the output is reached
            String line;
            while((line = outputReader.readLine()) != null) {
                LOGGER.trace(String.format("[output reader] %s",
                        line));
                outputBuilder.append(line);
            }
            LOGGER.trace("output reader thread terminated");
        } catch (IOException ex) {
            issueHandler.handleUnexpectedException(new ExceptionMessage(ex));
        }
    }
}
