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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.jhbuild.java.wrapper.download.DownloadCombi;
import richtercloud.jhbuild.java.wrapper.download.DownloadFailureCallbackReation;
import richtercloud.jhbuild.java.wrapper.download.Downloader;

/**
 * A wrapper around GNOME's JHBuild build and dependency manager which allows
 * to automatic download of tarballs and checkout of source roots with various
 * SCMs, automatic build, test and installation into a user prefix as well as
 * specification and resolution of dependencies.
 *
 * The wrapper uses some system binaries which are expected to be present and
 * otherwise need to be installed manually by the caller (e.g. {@code sh} and
 * {@code make}. Other non-system binaries, like {@link git} and {@link jhbuild}
 * itself are searched for and automatically downloaded and installed in case of
 * absence. Both system and non-system binaries are search for in the
 * environment variable {@code PATH} and the subdirectory {@code bin} of the
 * specified {@code installationPrefix} so that it's possible to install them as
 * non-root user (system-binaries don't necessarily have to be installed
 * manually into {@code installationPrefix} since any other installation prefix
 * can be added to {@code PATH} before the wrapper is used).
 *
 * {@code stdout} and {@code stderr} of build processes are redirected to
 * {@code stdout} and {@code stderr} of the JVM except {@code silenceStdout} or
 * {@code silenceStderr} are set to {@code true}. In this case the content of
 * both process streams is wrapped in a {@link BuildFailureException} in case a
 * build process returns a code which is unequal {@code 0}.
 *
 * The Wrapper make JHBuild use it's default cache directory for downloads and
 * build results under the user's home directory since there's few incentive to
 * make the location configurable.
 *
 * Only the {@link #cancelInstallModuleset() } is allowed to be called from
 * another thread, calls to other methods from other threads result in
 * unpredictable behaviour.
 *
 * @author richter
 */
public class JHBuildJavaWrapper {
    private final static Logger LOGGER = LoggerFactory.getLogger(JHBuildJavaWrapper.class);
    public final static String GIT_DEFAULT = "git";
    public final static String JHBUILD_DEFAULT = "jhbuild";
    public final static String SH_DEFAULT = "bash";
    public final static String MAKE_DEFAULT = "make";
    public final static String PYTHON_DEFAULT = "python";
    public final static String CC_DEFAULT = "gcc";
    public final static File CONFIG_DIR = new File(SystemUtils.getUserHome(),
            ".jhbuild-java-wrapper");
    public final static File INSTALLATION_PREFIX_DIR_DEFAULT = new File(CONFIG_DIR,
            "installation-prefix");
    public final static File DOWNLOAD_DIR_DEFAULT = new File(CONFIG_DIR,
            "downloads");
    private final static String PATH = "PATH";

    public static int calculateParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * The {@code git} binary to use.
     */
    /*
    internal implementation notes:
    - not final in order to allow overriding after installation
    */
    private String git;
    /**
     * The {@code jhbuild} binary to use.
     */
    /*
    internal implementation notes:
    - not final in order to allow overriding after installation
    */
    private String jhbuild;
    private final String sh;
    private final String make;
    /**
     * Python is necessary in order to avoid
     * {@code ./scripts/debian-python2-postinstall-hook.sh: Unable to find
     * 'python' in the PATH} during {@code make install} of JHBuild.
     */
    private String python;
    /**
     * A C compiler is necessary to build Python in case
     * {@link #actionOnMissingPython} is {@link ActionOnMissingBinary#DOWNLOAD}.
     */
    private final String cc;
    private final ActionOnMissingBinary actionOnMissingGit;
    private final ActionOnMissingBinary actionOnMissingZlib;
    private final ActionOnMissingBinary actionOnMissingLibffi;
    private final ActionOnMissingBinary actionOnMissingJHBuild;
        //Mac OSX download is a .dmg download which can't be extracted locally
    private final ActionOnMissingBinary actionOnMissingPython;
    private final boolean skipMD5SumCheck;
    private final File installationPrefixDir;
    private final File downloadDir;
    private boolean inited;
    /**
     * A flag to indicate that {@code stdout} of build processes ought not to be
     * redirected to {@code stdout} of the JVM.
     */
    private final boolean silenceStdout;
    /**
     * A flag to indicate that {@code stderr} of build processes ought not to be
     * redirected to {@code stderr} of the JVM.
     */
    private final boolean silenceStderr;
    private boolean canceled = false;
    /**
     * A pointer to the currently active process which allows to destroy it in
     * {@link #cancelInstallModuleset() } and thus minimize the time before
     * returning after cancelation has been requested.
     */
    private Process activeProcess = null;
    private final Map<Process, Pair<OutputReaderThread, OutputReaderThread>> processOutputReaderThreadMap = new HashMap<>();
    private final Downloader downloader;
    /**
     * The value passed to the {@code -j} option of all invokations of
     * {@code make}, except {@code make install}.
     */
    private final int parallelism;

    public JHBuildJavaWrapper(ActionOnMissingBinary actionOnMissingGit,
            ActionOnMissingBinary actionOnMissingZlib,
            ActionOnMissingBinary actionOnMissingLibffi,
            ActionOnMissingBinary actionOnMissingJHBuild,
            ActionOnMissingBinary actionOnMissingPython,
            Downloader downloader,
            boolean skipMD5SumCheck,
            boolean silenceStdout,
            boolean silenceStderr) throws IOException {
        this(INSTALLATION_PREFIX_DIR_DEFAULT,
                DOWNLOAD_DIR_DEFAULT,
                actionOnMissingGit,
                actionOnMissingZlib,
                actionOnMissingLibffi,
                actionOnMissingJHBuild,
                actionOnMissingPython,
                downloader,
                skipMD5SumCheck,
                silenceStdout,
                silenceStderr);
    }

    public JHBuildJavaWrapper(File installationPrefixDir,
            File downloadDir,
            ActionOnMissingBinary actionOnMissingGit,
            ActionOnMissingBinary actionOnMissingZlib,
            ActionOnMissingBinary actionOnMissingLibffi,
            ActionOnMissingBinary actionOnMissingJHBuild,
            ActionOnMissingBinary actionOnMissingPython,
            Downloader downloader,
            boolean skipMD5SumCheck,
            boolean silenceStdout,
            boolean silenceStderr) throws IOException {
        this(installationPrefixDir,
                downloadDir,
                GIT_DEFAULT,
                JHBUILD_DEFAULT,
                SH_DEFAULT,
                MAKE_DEFAULT,
                PYTHON_DEFAULT,
                CC_DEFAULT,
                downloader,
                skipMD5SumCheck,
                silenceStdout,
                silenceStderr,
                actionOnMissingGit,
                actionOnMissingZlib,
                actionOnMissingLibffi,
                actionOnMissingJHBuild,
                actionOnMissingPython,
                calculateParallelism());
    }

    public JHBuildJavaWrapper(File installationPrefixDir,
            File downloadDir,
            String git,
            String jhbuild,
            String sh,
            String make,
            String python,
            String cc,
            Downloader downloader,
            boolean skipMD5SumCheck,
            boolean silenceStdout,
            boolean silenceStderr,
            ActionOnMissingBinary actionOnMissingGit,
            ActionOnMissingBinary actionOnMissingZlib,
            ActionOnMissingBinary actionOnMissingLibffi,
            ActionOnMissingBinary actionOnMissingJHBuild,
            ActionOnMissingBinary actionOnMissingPython,
            int parallelism) throws IOException {
        if(installationPrefixDir.exists() && !installationPrefixDir.isDirectory()) {
            throw new IllegalArgumentException("installationPrefixDir points "
                    + "to an existing location and is not a directory");
        }
        this.installationPrefixDir = installationPrefixDir;
        if(!installationPrefixDir.exists()) {
            FileUtils.forceMkdir(installationPrefixDir);
        }
        this.downloadDir = downloadDir;
        if(!downloadDir.exists()) {
            FileUtils.forceMkdir(downloadDir);
        }
        this.git = git;
        this.jhbuild = jhbuild;
        this.sh = sh;
        this.make = make;
        this.python = python;
        this.cc = cc;
        if(downloader == null) {
            throw new IllegalArgumentException("downloader mustn't be null");
        }
        this.downloader = downloader;
        this.actionOnMissingGit = actionOnMissingGit;
        this.actionOnMissingZlib = actionOnMissingZlib;
        this.actionOnMissingLibffi = actionOnMissingLibffi;
        this.actionOnMissingJHBuild = actionOnMissingJHBuild;
        this.actionOnMissingPython = actionOnMissingPython;
        this.skipMD5SumCheck = skipMD5SumCheck;
        this.silenceStdout = silenceStdout;
        this.silenceStderr = silenceStderr;
        if(parallelism < 1) {
            throw new IllegalArgumentException(String.format("parallelism value of less than 1 doesn't make sense (was %d)",
                    parallelism));
        }
        this.parallelism = parallelism;
    }

    private Process createProcess(String path,
            String... commands) throws IOException {
        Process retValue = createProcess(null,
                path,
                commands);
        return retValue;
    }

    private Process createProcess(File directory,
            String path,
            String... commands) throws IOException {
        Process retValue = createProcess(directory,
                ImmutableMap.<String, String>builder()
                        .put("PATH", path)
                        .build(),
                commands);
        return retValue;
    }

    /**
     * Allows sharing code between different process creation routines.
     *
     * @param directory
     * @param path
     * @param commands
     * @return
     * @throws IOException
     */
    /*
    internal implementation notes:
    - checking for canceled doesn't make sense here because null would have to
    be returned or an exception thrown in the case of canceled state which
    creates the need to evaluate the return value by callers which is equally
    complex as checking the condition before calls to createProcess
    */
    private Process createProcess(File directory,
            Map<String, String> env,
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
        if(silenceStdout || silenceStderr) {
            OutputReaderThread stdoutReaderThread = null, stderrReaderThread = null;
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
            processOutputReaderThreadMap.put(retValue,
                    new ImmutablePair<>(stdoutReaderThread, stderrReaderThread));
        }
        synchronized(this) {
            this.activeProcess = retValue;
        }
        return retValue;
    }

    /**
     * Initialization routines.
     *
     * @param installationPrefixPath
     * @return {@code false} if the download or any build step has been
     * canceled, {@code true} otherwise
     * @throws OSNotRecognizedException
     * @throws ArchitectureNotRecognizedException
     * @throws IOException
     * @throws ExtractionException
     * @throws InterruptedException
     * @throws MissingSystemBinary
     * @throws BuildFailureException
     * @throws InitCanceledException
     */
    private boolean init(String installationPrefixPath) throws OSNotRecognizedException,
            ArchitectureNotRecognizedException,
            IOException,
            ExtractionException,
            InterruptedException,
            MissingSystemBinary,
            BuildFailureException {
        if(inited) {
            LOGGER.debug("already inited");
            return true;
        }
        assert downloadDir.exists() && downloadDir.isDirectory();
        LOGGER.trace(String.format("silenceStdout: %s",
                silenceStdout));
        LOGGER.trace(String.format("silenceStderr: %s",
                silenceStderr));
        try {
            BinaryTools.validateBinary(cc,
                    "cc",
                    installationPrefixPath);
        }catch(BinaryValidationException gccBinaryValidationException) {
            //there's no sense in providing options/MissingBiaryAction since
            //building GCC from source without a working C compiler is between
            //troublesome and impossible
            throw new IllegalStateException(String.format("cc (C compiler) binary '%s' doesn't exist and can't be found in PATH",
                            cc));
        }
        //zlib is a prerequisite of git and python build
        boolean zlibPresent = checkLibPresence(installationPrefixDir,
                "zlib.pc");
        if(zlibPresent) {
            LOGGER.debug("using existing version of zlib in installation prefix");
        }else {
            switch(actionOnMissingZlib) {
                case FAIL:
                    throw new IllegalStateException("library zlib doesn't exist in installation prefix");
                case DOWNLOAD:
                    DownloadCombi zlibDownloadCombi = new DownloadCombi("https://www.zlib.net/zlib-1.2.11.tar.gz", //downloadURL
                            new File(downloadDir,
                                    "zlib-1.2.11.tar.gz").getAbsolutePath(), //downloadTarget
                            ExtractionMode.EXTRACTION_MODE_TAR_GZ,
                            new File(downloadDir,
                                    "zlib-1.2.11").getAbsolutePath(), //extractionLocation
                            "1c9f62f0778697a09d36121ead88e08e" //md5sum
                    );
                    String zlib = installPrerequisiteAutotools(installationPrefixPath,
                            "", //binary (library doesn't provide binary, see
                                //installPrerequisiteAutotools for details)
                            "zlib",
                            zlibDownloadCombi);
                    if(zlib == null) {
                        //interactive download has been canceled
                        return false;
                    }
                    assert "".equals(zlib);
            }
        }
        //libffi is prerequisite in order to avoid failure to build `_ctypes`
        //module of Python
        boolean libffiPresent = checkLibPresence(installationPrefixDir,
                "libffi.pc");
        if(libffiPresent) {
            LOGGER.debug("using existing version of libffi in installation prefix");
        }else {
            switch(actionOnMissingLibffi) {
                case FAIL:
                    throw new IllegalStateException("library libffi doesn't exist in installation prefix");
                case DOWNLOAD:
                    DownloadCombi libffiDownloadCombi = new DownloadCombi("http://sourceware.org/pub/libffi/libffi-3.2.tar.gz", //downloadURL
                            new File(downloadDir,
                                    "libffi-3.2.tar.gz").getAbsolutePath(), //downloadTarget
                            ExtractionMode.EXTRACTION_MODE_TAR_GZ,
                            new File(downloadDir,
                                    "libffi-3.2").getAbsolutePath(), //extractionLocation
                            "41e0216cc2be4029fad3128988295f0f" //md5sum
                    );
                    String libffi = installPrerequisiteAutotools(installationPrefixPath,
                            "", //binary (library doesn't provide binary, see
                                //installPrerequisiteAutotools for details)
                            "libffi",
                            libffiDownloadCombi);
                    if(libffi == null) {
                        //interactive download has been canceled
                        return false;
                    }
                    assert "".equals(libffi);
            }
        }
        //unclear why git version of Python has been used before (only increases
        //download time and might include instabilities from master)
        try {
            BinaryTools.validateBinary(python,
                    "python",
                    installationPrefixPath);
        }catch(BinaryValidationException ex1) {
            switch(actionOnMissingPython) {
                case FAIL:
                    throw new IllegalStateException(String.format("python binary '%s' doesn't exist and can't be found in PATH",
                            python));
                case DOWNLOAD:
                    DownloadCombi pythonDownloadCombi = new DownloadCombi("https://www.python.org/ftp/python/3.6.4/Python-3.6.4.tgz",
                            new File(downloadDir,
                                    "Python-3.6.4.tgz").getAbsolutePath(),
                            ExtractionMode.EXTRACTION_MODE_TAR_GZ,
                            new File(downloadDir,
                                    "Python-3.6.4").getAbsolutePath(),
                            "9de6494314ea199e3633211696735f65");
                    python = installPrerequisiteAutotools(installationPrefixPath,
                            "python",
                            "python",
                            pythonDownloadCombi);
                    if(python == null) {
                        //interactive download has been canceled
                        return false;
                    }
                    try {
                        BinaryTools.validateBinary(python,
                                "python",
                                installationPrefixPath);
                    } catch (BinaryValidationException ex2) {
                        assert false: "python exisistence check or installation failed";
                    }
            }
        }
        try {
            BinaryTools.validateBinary(git,
                    "git",
                    installationPrefixPath);
        }catch(BinaryValidationException ex) {
            switch(actionOnMissingGit) {
                case FAIL:
                    throw new IllegalStateException(String.format("git binary '%s' doesn't exist and can't be found in PATH",
                            git));
                case DOWNLOAD:
                    DownloadCombi gitDownloadCombi = new DownloadCombi("https://www.kernel.org/pub/software/scm/git/git-2.13.3.tar.gz",
                            new File(downloadDir,
                                    "git-2.13.3.tar.gz").getAbsolutePath(),
                            ExtractionMode.EXTRACTION_MODE_TAR_GZ,
                            new File(downloadDir,
                                    "git-2.13.3").getAbsolutePath(),
                            "d2dc550f6693ba7e5b16212b2714f59f");
                    git = installPrerequisiteAutotools(installationPrefixPath,
                            "git",
                            "git",
                            gitDownloadCombi);
                    if(git == null) {
                        //interactive download has been canceled
                        return false;
                    }
                    try {
                        BinaryTools.validateBinary(git,
                                "git",
                                installationPrefixPath);
                    } catch (BinaryValidationException ex1) {
                        assert false: "git exisistence check or installation failed";
                    }
            }
        }
        try {
            BinaryTools.validateBinary(jhbuild,
                    "jhbuild",
                    installationPrefixPath);
        }catch(BinaryValidationException ex) {
            switch(actionOnMissingJHBuild) {
                case FAIL:
                    throw new IllegalStateException(String.format("jhbuild binary '%s' doesn't exist and can't be found in PATH",
                            jhbuild));
                case DOWNLOAD:
                    File jhbuildCloneDir = new File(downloadDir, "jhbuild");
                    boolean needClone = true;
                    if(jhbuildCloneDir.exists()
                            && jhbuildCloneDir.list().length > 0) {
                        //check whether the existing non-empty directory is a
                        //valid source root
                        synchronized(this) {
                            if(canceled) {
                                return false;
                            }
                        }
                        Process jhbuildSourceRootCheckProcess = createProcess(jhbuildCloneDir,
                                installationPrefixPath,
                                git,
                                "status");
                        LOGGER.debug("waiting for jhbuild source root check");
                        jhbuildSourceRootCheckProcess.waitFor();
                        if(jhbuildSourceRootCheckProcess.exitValue() != 0) {
                            OutputReaderThread stdoutReaderThread = processOutputReaderThreadMap.get(jhbuildSourceRootCheckProcess).getKey();
                            OutputReaderThread stderrReaderThread = processOutputReaderThreadMap.get(jhbuildSourceRootCheckProcess).getValue();
                            stdoutReaderThread.join();
                            stderrReaderThread.join();
                            throw new IllegalStateException(String.format("The "
                                    + "jhbuild clone directory '%s' already "
                                    + "exist, is not empty and is not a valid "
                                    + "git source root. This might be the "
                                    + "result of a failing previous checkout. "
                                    + "You need to check and eventually delete "
                                    + "the existing directory or specify "
                                    + "another download directory for JHBuild "
                                    + "Java wrapper (git status process had "
                                    + "stdout '%s' and stderr '%s').",
                                    jhbuildCloneDir.getAbsolutePath(),
                                    stdoutReaderThread.getOutputBuilder().toString(),
                                    stderrReaderThread.getOutputBuilder().toString()));
                        }
                        needClone = false;
                    }
                    if(needClone) {
                        synchronized(this) {
                            if(canceled) {
                                return false;
                            }
                        }
                        Process jhbuildCloneProcess = createProcess(installationPrefixPath,
                                git,
                                "clone",
                                "git://git.gnome.org/jhbuild",
                                jhbuildCloneDir.getAbsolutePath());
                            //directory doesn't matter because target path is
                            //absolute
                        LOGGER.debug("waiting for jhbuild download");
                        jhbuildCloneProcess.waitFor();
                        if(jhbuildCloneProcess.exitValue() != 0) {
                            handleBuilderFailure("jhbuild",
                                    BuildStep.CLONE,
                                    jhbuildCloneProcess);
                        }
                        LOGGER.debug("jhbuild download finished");
                    }
                    synchronized(this) {
                        if(canceled) {
                            return false;
                        }
                    }
                    Process jhbuildAutogenProcess = createProcess(jhbuildCloneDir,
                            installationPrefixPath,
                            sh, "autogen.sh",
                            String.format("--prefix=%s", installationPrefixDir.getAbsolutePath()));
                        //autogen.sh runs configure
                    LOGGER.debug("waiting for jhbuild build bootstrap process");
                    jhbuildAutogenProcess.waitFor();
                    if(jhbuildAutogenProcess.exitValue() != 0) {
                        handleBuilderFailure("jhbuild",
                                BuildStep.BOOTSTRAP,
                                jhbuildAutogenProcess);
                    }
                    LOGGER.debug("jhbuild build bootstrap process finished");
                    synchronized(this) {
                        if(canceled) {
                            return false;
                        }
                    }
                    Process jhbuildMakeProcess = createProcess(jhbuildCloneDir,
                            installationPrefixPath,
                            make, String.format("-j%d", parallelism));
                    LOGGER.debug("waiting for jhbuild build process");
                    jhbuildMakeProcess.waitFor();
                    if(jhbuildMakeProcess.exitValue() != 0) {
                        handleBuilderFailure("jhbuild",
                                BuildStep.MAKE,
                                jhbuildMakeProcess);
                    }
                    LOGGER.debug("jhbuild build process finished");
                    synchronized(this) {
                        if(canceled) {
                            return false;
                        }
                    }
                    Process jhbuildMakeInstallProcess = createProcess(jhbuildCloneDir,
                            installationPrefixPath,
                            make, "install");
                    LOGGER.debug("waiting for jhbuild installation process");
                    jhbuildMakeInstallProcess.waitFor();
                    if(jhbuildMakeInstallProcess.exitValue() != 0) {
                        handleBuilderFailure("jhbuild",
                                BuildStep.MAKE_INSTALL,
                                jhbuildMakeInstallProcess);
                    }
                    LOGGER.debug("jhbuild installation process finished");
                    jhbuild = "jhbuild";
                        //is found in modified path of every process built with
                        //buildProcess
                    LOGGER.debug(String.format("using jhbuild command '%s'",
                            jhbuild));
            }
        }
        this.inited = true;
        return true;
    }

    private void handleBuilderFailure(String moduleName,
            BuildStep buildFailureStep,
            Process failedBuildProcess) throws BuildFailureException, IOException, InterruptedException {
        String stdout = null;
        String stderr = null;
        if(silenceStdout) {
            OutputReaderThread stdoutReaderThread = processOutputReaderThreadMap.get(failedBuildProcess).getKey();
            stdoutReaderThread.join();
            stdout = stdoutReaderThread.getOutputBuilder().toString();
        }
        if(silenceStderr) {
            OutputReaderThread stderrReaderThread = processOutputReaderThreadMap.get(failedBuildProcess).getValue();
            stderrReaderThread.join();
            stderr = stderrReaderThread.getOutputBuilder().toString();
        }
        throw new BuildFailureException(moduleName,
                buildFailureStep,
                stdout,
                stderr);
    }

    /**
     * Allows cancelation (with minimal delay) from another thread.
     */
    public void cancelInstallModuleset() {
        this.canceled = true;
        synchronized(this) {
            if(activeProcess != null) {
                activeProcess.destroy();
            }
        }
    }

    /**
     * Check canceled state.
     *
     * @return {@code true} if {@link #cancelInstallModuleset() } has been
     * invoked and no other build process started so far, {@code false}
     * otherwise
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Tries to find {@code moduleName} in the default moduleset on the
     * classpath shipped with archive.
     *
     * The module installation can be canceled from another thread with
     * {@link #cancelInstallModuleset() }.
     *
     * @param moduleName the module to build
     * @throws OSNotRecognizedException
     * @throws ArchitectureNotRecognizedException
     * @throws IOException
     * @throws ExtractionException
     * @throws InterruptedException
     * @throws MissingSystemBinary
     * @throws BuildFailureException
     */
    public boolean installModuleset(String moduleName) throws OSNotRecognizedException,
            ArchitectureNotRecognizedException,
            IOException,
            ExtractionException,
            InterruptedException,
            MissingSystemBinary,
            BuildFailureException,
            ModuleBuildFailureException {
        InputStream modulesetInputStream = JHBuildJavaWrapper.class.getResourceAsStream("/moduleset-default.xml");
        assert modulesetInputStream != null;
        boolean retValue = installModuleset(modulesetInputStream,
                moduleName);
        return retValue;
    }

    /**
     * Installs module {@code moduleName} from JHBuild moduleset provided by
     * {@code modulesetInputStream}.
     *
     * The module installation can be canceled from another thread with
     * {@link #cancelInstallModuleset() }.
     *
     * @param modulesetInputStream
     * @param moduleName
     * @throws OSNotRecognizedException
     * @throws ArchitectureNotRecognizedException
     * @throws IOException
     * @throws ExtractionException
     * @throws InterruptedException
     * @throws MissingSystemBinary
     * @throws BuildFailureException
     * @throws IllegalArgumentException if {@code modulesetInputStream} is
     * {@code null}
     */
    /*
    internal implementation notes:
    - throwing IllegalArgumentException if modulesetInputStream is null improves
    handling of InputStream which have been acquired through
    Class.getResourceAsStream since those might be null
    */
    public boolean installModuleset(InputStream modulesetInputStream,
            String moduleName) throws OSNotRecognizedException,
            ArchitectureNotRecognizedException,
            IOException,
            ExtractionException,
            InterruptedException,
            MissingSystemBinary,
            BuildFailureException,
            ModuleBuildFailureException {
        canceled = false;
        if(modulesetInputStream == null) {
            throw new IllegalArgumentException("modulesetInputStream mustn't be null");
        }
        if(moduleName == null || moduleName.isEmpty()) {
            throw new IllegalArgumentException("moduleName mustn't be null or empty");
        }
        String installationPrefixPath = String.join(File.pathSeparator, System.getenv("PATH"),
                String.join(File.separator, installationPrefixDir.getAbsolutePath(), "bin"));
        boolean notCanceled = init(installationPrefixPath);
        if(!notCanceled) {
            return false;
        }
        LOGGER.debug(String.format("building module %s with jhbuild command %s",
                moduleName,
                jhbuild));
        Process jhbuildBootstrapProcess = createProcess(installationPrefixPath,
                jhbuild, "bootstrap");
            //directory doesn't matter
        LOGGER.debug("waiting for jhbuild bootstrap process");
        jhbuildBootstrapProcess.waitFor();
        if(jhbuildBootstrapProcess.exitValue() != 0) {
            OutputReaderThread stdoutReaderThread = processOutputReaderThreadMap.get(jhbuildBootstrapProcess).getKey();
            OutputReaderThread stderrReaderThread = processOutputReaderThreadMap.get(jhbuildBootstrapProcess).getValue();
            stdoutReaderThread.join();
            stderrReaderThread.join();
            throw new ModuleBuildFailureException(String.format("jhbuild "
                    + "bootstrap process returned with code %d (stdout was "
                    + "'%s' and stderr was '%s')",
                    jhbuildBootstrapProcess.exitValue(),
                    stdoutReaderThread.getOutputBuilder().toString(),
                    stderrReaderThread.getOutputBuilder().toString()));
        }
        LOGGER.debug("jhbuild bootstrap process finished");
        File modulesetFile = Files.createTempFile(JHBuildJavaWrapper.class.getSimpleName(), //prefix
                "moduleset" //suffix
        ).toFile();
        IOUtils.copy(modulesetInputStream, new FileOutputStream(modulesetFile));
        String jHBuildrcTemplate = String.format("prefix=\"%s\"",
                installationPrefixDir.getAbsolutePath());
        File jHBuildrcFile = Files.createTempFile(JHBuildJavaWrapper.class.getSimpleName(), //prefix
                "jhbuildrc" //suffix
        ).toFile();
        IOUtils.write(jHBuildrcTemplate,
                new FileOutputStream(jHBuildrcFile),
                Charsets.UTF_8);
        Process jhbuildProcess = createProcess(installationPrefixPath,
                jhbuild,
                String.format("--file=%s",
                        jHBuildrcFile.getAbsolutePath()),
                    //the .jhbuildrc file
                String.format("--moduleset=%s",
                        modulesetFile.getAbsolutePath()),
                "--no-interact",
                "build",
                "--nodeps",
                moduleName);
            //directory doesn't matter
        LOGGER.debug("waiting for jhbuild build process");
        jhbuildProcess.waitFor();
        if(jhbuildProcess.exitValue() != 0) {
            OutputReaderThread stdoutReaderThread = processOutputReaderThreadMap.get(jhbuildProcess).getKey();
            OutputReaderThread stderrReaderThread = processOutputReaderThreadMap.get(jhbuildProcess).getValue();
            stdoutReaderThread.join();
            stderrReaderThread.join();
            throw new ModuleBuildFailureException(String.format("jhbuild "
                    + "returned with code %d during building of module (stdout "
                    + "was '%s' and stderr was '%s')",
                    jhbuildProcess.exitValue(),
                    stdoutReaderThread.getOutputBuilder().toString(),
                    stderrReaderThread.getOutputBuilder().toString()));
        }
        LOGGER.debug("jhbuild build process finished");
        return true;
    }

    /**
     * Installs an autotools-based prerequisiste.
     *
     * @param installationPrefixPath the {@code PATH} of the installation prefix
     * to use
     * @param binary the binary which is supposed to be installed and returned
     * after successful, non-canceled installation (see return value
     * description)
     * @param binaryDescription a description of the binary to be installed used
     * in logging messages (can be the name of the binary or a short description
     * like "C compiler")
     * @return the path of the installed binary, the empty string in case no
     * binary has been installed, but the installation hasn't been canceled or
     * {@code null} if the installation or any part of it has been aborted
     * @throws IOException
     * @throws OSNotRecognizedException
     * @throws ArchitectureNotRecognizedException
     * @throws ExtractionException
     * @throws MissingSystemBinary
     * @throws InterruptedException
     * @throws BuildFailureException
     */
    private String installPrerequisiteAutotools(String installationPrefixPath,
            String binary,
            String binaryDescription,
            DownloadCombi downloadCombi) throws IOException, OSNotRecognizedException, ArchitectureNotRecognizedException, ExtractionException, MissingSystemBinary, InterruptedException, BuildFailureException {
        boolean notCanceled = downloader.downloadFile(downloadCombi,
                skipMD5SumCheck,
            ex1 -> {
                return DownloadFailureCallbackReation.RETRY;
            },
            (String md5SumExpected,
                    String md5SumActual,
                    int numberOfRetries) -> {
                return numberOfRetries < 5
                        ? MD5SumCheckUnequalsCallbackReaction.RETRY
                        : MD5SumCheckUnequalsCallbackReaction.CANCEL;
            });
        if(!notCanceled) {
            LOGGER.debug(String.format("install prerequisiste download for %s canceled",
                    binaryDescription));
            return null;
        }
        //need make for building and it's overly hard to bootstrap
        //without it, so force installation out of JHBuild wrapper
        try {
            BinaryTools.validateBinary(make,
                    "make",
                    installationPrefixPath);
        }catch(BinaryValidationException ex1) {
            throw new MissingSystemBinary("make",
                    ex1);
        }
        //build
        File extractionLocationDir = new File(downloadCombi.getExtractionLocation());
        assert extractionLocationDir.exists();
        synchronized(this) {
            if(canceled) {
                LOGGER.debug(String.format("canceling prerequisiste installation of %s because the build wrapper has been canceled",
                        binaryDescription));
                return null;
            }
        }
        Process configureProcess = createProcess(extractionLocationDir,
                ImmutableMap.<String, String>builder()
                        .put(PATH, installationPrefixPath)
                        .put("CFLAGS", String.format("-I%s -L%s",
                                new File(installationPrefixDir, "include").getAbsolutePath(),
                                new File(installationPrefixDir, "lib").getAbsolutePath()))
//                        .put("LDFLAGS", String.format("-L%s", new File(installationPrefixDir, "lib").getAbsolutePath()))
                        .build(),
                sh, "configure",
                String.format("--prefix=%s", installationPrefixDir.getAbsolutePath()));
        configureProcess.waitFor();
        if(configureProcess.exitValue() != 0) {
            handleBuilderFailure(binaryDescription,
                    BuildStep.CONFIGURE,
                    configureProcess);
        }
        synchronized(this) {
            if(canceled) {
                LOGGER.debug(String.format("canceling prerequisiste installation of %s because the build wrapper has been canceled",
                        binaryDescription));
                return null;
            }
        }
        Process makeProcess = createProcess(extractionLocationDir,
                ImmutableMap.<String, String>builder()
                        .put(PATH, installationPrefixPath)
                        .put("CFLAGS", String.format("-I%s -L%s", new File(installationPrefixDir, "include").getAbsolutePath(),
                                new File(installationPrefixDir, "lib").getAbsolutePath()))
//                        .put("LDFLAGS", String.format("-L%s", new File(installationPrefixDir, "lib").getAbsolutePath()))
                        .build(),
                make, String.format("-j%d", parallelism));
        makeProcess.waitFor();
        if(makeProcess.exitValue() != 0) {
            handleBuilderFailure(binaryDescription,
                    BuildStep.MAKE,
                    makeProcess);
        }
        synchronized(this) {
            if(canceled) {
                LOGGER.debug(String.format("canceling prerequisiste installation of %s because the build wrapper has been canceled",
                        binaryDescription));
                return null;
            }
        }
        Process makeInstallProcess = createProcess(extractionLocationDir,
                installationPrefixPath,
                make,
                "install");
        makeInstallProcess.waitFor();
        if(makeInstallProcess.exitValue() != 0) {
            handleBuilderFailure(binaryDescription,
                    BuildStep.MAKE_INSTALL,
                    makeInstallProcess);
        }
        return binary;
            //is found in modified path of every process built with
            //buildProcess
    }

    /**
     * Checks presence of a {@code .pc} file in the specified installation
     * prefix which allows to conclude that the library is installed in a
     * rudimentary way.
     *
     * @param pcFileName the name of the {@code .pc} file to search for
     * @throws IOException if {@link Files.walk} throws such an exception
     * @return {@code true} if the file has been found, {@code false} otherwise
     */
    private boolean checkLibPresence(File installationPrefixDir,
            String pcFileName) throws IOException {
        Optional<Path> hit = Files.walk(installationPrefixDir.toPath())
                .filter(file -> file.getFileName().toFile().getName().equals(pcFileName))
                .findAny();
            //recursive search, from
            //https://stackoverflow.com/questions/10780747/recursively-search-for-a-directory-in-java
        return hit.isPresent();
    }
}
