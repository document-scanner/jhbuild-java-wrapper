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

import java.io.InputStream;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richter
 */
public class OutputReaderThread extends Thread {
    private final static Logger LOGGER = LoggerFactory.getLogger(OutputReaderThread.class);
    private final static int OUTPUT_BUILDER_CAPACITY_DEFAULT = 2048;
    private static int counter = 0;
    @SuppressWarnings("PMD.AvoidStringBufferField")
        //currently no better way found
    private final StringBuilder outputBuilder = new StringBuilder(OUTPUT_BUILDER_CAPACITY_DEFAULT);
    /**
     * The process stream to read ({@code stdout} or {@code stderr}).
     */
    private final InputStream processStream;

    public OutputReaderThread(InputStream processStream) {
        this(processStream,
                String.format("output-reader-thread-%d",
                        counter));
        counter += 1;
    }

    /**
     * Creates a new {@code OutputReaderThread}.
     *
     * @param processStream the process output stream to read from
     * @param name the name of the thread (see
     * {@link Thread#Thread(java.lang.String) } for details)
     */
    public OutputReaderThread(InputStream processStream,
            String name) {
        super(name);
        this.processStream = processStream;
    }

    public StringBuilder getOutputBuilder() {
        return outputBuilder;
    }

    @Override
    public void run() {
        //Using a BufferedReader buffering an InputStreamReader or the
        //InputStreamReader directly causes random deadlocks because of
        //blocks at BufferedReader.readLine or InputStreamReader.read after
        //Process.destory. Those simply don't ever happen with Scanner, so
        //Scanner is used. The tip comes from
        //https://stackoverflow.com/questions/30846870/java-problems-with-reading-from-process-output.
        //Using Scanner avoids the need to catch IOException and thus passing an
        //IssueHandler reference.
        Scanner scanner = new Scanner(processStream);
        //Possible condition to loop over (different have been tried because a
        //deadlock of InputStream.read occured after killing the postgres
        //process with Process.destroy; Scanner is the best because it avoids
        //catching IOException):
        //- testing for BufferedReader.ready causes thread to terminate
        //before the end of the output is reached
        //- testing for process.isAlive doesn't make sense because it discards
        //output after the process terminated
        //- using code from com.Ostermiller.util.ExecHelper causes output to be
        //not available in short runs like `[program] --version`
        //- Scanner.hasNext()
        while(scanner.hasNext()) {
            String line = scanner.nextLine();
            LOGGER.trace(String.format("[output reader] %s",
                    line));
            outputBuilder.append(line)
                    .append('\n');
        }
        LOGGER.trace("output reader thread terminated");
    }
}
