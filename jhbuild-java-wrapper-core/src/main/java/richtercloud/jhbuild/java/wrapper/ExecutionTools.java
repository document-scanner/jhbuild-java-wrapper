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

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richter
 */
public class ExecutionTools {
    private final static Logger LOGGER = LoggerFactory.getLogger(ExecutionTools.class);
    public final static String SH_DEFAULT = "sh";

    public static Triple<Process, OutputReaderThread, OutputReaderThread> createProcess(String path,
            boolean silenceStdout,
            boolean silenceStderr,
            String... commands) throws IOException {
        Triple<Process, OutputReaderThread, OutputReaderThread> retValue = createProcess(null,
                path,
                silenceStdout,
                silenceStderr,
                commands);
        return retValue;
    }

    public static Triple<Process, OutputReaderThread, OutputReaderThread> createProcess(File directory,
            String path,
            boolean silenceStdout,
            boolean silenceStderr,
            String... commands) throws IOException {
        Triple<Process, OutputReaderThread, OutputReaderThread> retValue = createProcess(directory,
                ImmutableMap.<String, String>builder()
                        .put("PATH", path)
                        .build(),
                SH_DEFAULT,
                silenceStdout,
                silenceStderr,
                commands);
        return retValue;
    }

    /**
     * Allows sharing code between different process creation routines.
     *
     * @param directory the directory in which to execute the process
     * @param path the path to add to the current value of the {@code PATH}
     * environment variable
     * @param sh the shell in which to wrap the command execution
     * @param commands the commands to create a {@link Process} for
     * @return the created process
     * @throws IOException
     */
    /*
    internal implementation notes:
    - checking for canceled doesn't make sense here because null would have to
    be returned or an exception thrown in the case of canceled state which
    creates the need to evaluate the return value by callers which is equally
    complex as checking the condition before calls to createProcess
    */
    public static Triple<Process, OutputReaderThread, OutputReaderThread> createProcess(File directory,
            Map<String, String> env,
            String sh,
            boolean silenceStdout,
            boolean silenceStderr,
            String... commands) throws IOException {
        LOGGER.trace(String.format("building process with commands '%s' with environment '%s' running in %s",
                Arrays.asList(commands),
                env,
                directory != null ? String.format("directory '%s'",
                        directory.getAbsolutePath())
                        : "current directory"));
        ProcessBuilder processBuilder = new ProcessBuilder(sh, "-c", String.join(" ", commands))
                .redirectOutput(silenceStdout ? Redirect.PIPE : Redirect.INHERIT)
                .redirectError(silenceStderr ? Redirect.PIPE : Redirect.INHERIT);
            //need to wrap commands in a shell in order allow modified path for
            //binary discovery (the unmodifiable PATH of the JVM is used to find
            //the command to execute which doesn't allow any modification after
            //installations by wrapper)
        if(directory != null) {
            processBuilder.directory(directory);
        }
        processBuilder.environment().putAll(env);
        Process retValue = processBuilder.start();
        OutputReaderThread stdoutReaderThread = null, stderrReaderThread = null;
        if(silenceStdout || silenceStderr) {
            if(silenceStdout) {
                stdoutReaderThread = new OutputReaderThread(retValue.getInputStream(),
                        retValue);
                stdoutReaderThread.start();
            }
            if(silenceStderr) {
                stderrReaderThread = new OutputReaderThread(retValue.getErrorStream(),
                        retValue);
                stderrReaderThread.start();
            }
        }
        return new ImmutableTriple<>(retValue,
                stdoutReaderThread,
                stderrReaderThread);
    }

    private ExecutionTools() {
    }
}
