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
import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @author richter
 */
public class JHBuildJavaWrapper {
    private final static Logger LOGGER = LoggerFactory.getLogger(JHBuildJavaWrapper.class);
    private final static String GIT_DEFAULT = "git";
    private final static String JHBUILD_DEFAULT = "jhbuild";
    private final static String SH_DEFAULT = "sh";
    private final static String MAKE_DEFAULT = "make";
    private final static DownloadCombi GIT_DOWNLOAD_COMBI_LINUX_32_DEFAULT = new DownloadCombi("https://www.kernel.org/pub/software/scm/git/git-2.13.3.tar.gz",
            "git-2.13.3.tar.gz",
            ExtractionMode.EXTRACTION_MODE_TAR_GZ,
            "git-2.13.3",
            "e10ede8b80a2c987d04ee376534cb7e1");
    private final static DownloadCombi GIT_DOWNLOAD_COMBI_LINUX_64_DEFAULT = GIT_DOWNLOAD_COMBI_LINUX_32_DEFAULT;
    private final static DownloadCombi GIT_DOWNLOAD_COMBI_WINDOWS_32_DEFAULT = new DownloadCombi("https://github.com/git-for-windows/git/releases/download/v2.13.3.windows.1/PortableGit-2.13.3-32-bit.7z.exe",
            "PortableGit-2.13.3-32-bit.7z.exe",
            ExtractionMode.EXTRACTION_MODE_NONE,
            null,
            "72908af3e98c1ec631113da9d38a6875");
    private final static DownloadCombi GIT_DOWNLOAD_COMBI_WINDOWS_64_DEFAULT = new DownloadCombi("https://github.com/git-for-windows/git/releases/download/v2.13.3.windows.1/PortableGit-2.13.3-64-bit.7z.exe",
            "PortableGit-2.13.3-64-bit.7z.exe",
            ExtractionMode.EXTRACTION_MODE_NONE,
            null,
            "24c9f5482e419174e3b4a53e759ebb96");
    private final static DownloadCombi GIT_DOWNLOAD_COMBI_MAC_OSX_DEFAULT = new DownloadCombi("https://github.com/git-for-windows/git/releases/download/v2.13.3.windows.1/PortableGit-2.13.3-32-bit.7z.exe",
            "PortableGit-2.13.3-32-bit.7z.exe",
            ExtractionMode.EXTRACTION_MODE_NONE,
            null,
            "72908af3e98c1ec631113da9d38a6875");
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
    private final ActionOnMissingBinary actionOnMissingGit;
    private final ActionOnMissingBinary actionOnMissingJHBuild;
        //Mac OSX download is a .dmg download which can't be extracted locally
    /**
     * Mapping between supported OS and the {@link DownloadCombi} for
     * {@code git}.
     */
    /*
    internal implementation notes:
    - @TODO: allow passing custom mapping
    */
    private Map<SupportedOS, DownloadCombi> oSDownloadCombiGitMap = new ImmutableMap.Builder<SupportedOS, DownloadCombi>()
            .put(SupportedOS.LINUX_32, GIT_DOWNLOAD_COMBI_LINUX_32_DEFAULT)
            .put(SupportedOS.LINUX_64, GIT_DOWNLOAD_COMBI_LINUX_64_DEFAULT)
            .put(SupportedOS.WINDOWS_32, GIT_DOWNLOAD_COMBI_WINDOWS_32_DEFAULT)
            .put(SupportedOS.WINDOWS_64, GIT_DOWNLOAD_COMBI_WINDOWS_64_DEFAULT)
            .put(SupportedOS.MAC_OSX_64, GIT_DOWNLOAD_COMBI_MAC_OSX_DEFAULT).build();
    private final Window downloadDialogParent;
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

    public JHBuildJavaWrapper(File installationPrefixDir,
            File downloadDir,
            ActionOnMissingBinary actionOnMissingGit,
            ActionOnMissingBinary actionOnMissingJHBuild,
            Window downloadDialogParent,
            boolean skipMD5SumCheck,
            boolean silenceStdout,
            boolean silenceStderr) {
        this(installationPrefixDir,
                downloadDir,
                GIT_DEFAULT,
                JHBUILD_DEFAULT,
                SH_DEFAULT,
                MAKE_DEFAULT,
                downloadDialogParent,
                skipMD5SumCheck,
                silenceStdout,
                silenceStderr,
                actionOnMissingGit,
                actionOnMissingJHBuild);
    }

    public JHBuildJavaWrapper(File installationPrefixDir,
            File downloadDir,
            String git,
            String jhbuild,
            String sh,
            String make,
            Window downloadDialogParent,
            boolean skipMD5SumCheck,
            boolean silenceStdout,
            boolean silenceStderr,
            ActionOnMissingBinary actionOnMissingGit,
            ActionOnMissingBinary actionOnMissingJHBuild) {
        this.installationPrefixDir = installationPrefixDir;
        this.downloadDir = downloadDir;
        this.git = git;
        this.jhbuild = jhbuild;
        this.sh = sh;
        this.make = make;
        this.downloadDialogParent = downloadDialogParent;
        this.actionOnMissingGit = actionOnMissingGit;
        this.actionOnMissingJHBuild = actionOnMissingJHBuild;
        this.skipMD5SumCheck = skipMD5SumCheck;
        this.silenceStdout = silenceStdout;
        this.silenceStderr = silenceStderr;
    }

    private Process buildProcess(String path,
            String... commands) throws IOException {
        Process retValue = buildProcess(null,
                path,
                commands);
        return retValue;
    }

    private Process buildProcess(File directory,
            String path,
            String... commands) throws IOException {
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
        processBuilder.environment().put("PATH",
                path);
        Process retValue = processBuilder.start();
        return retValue;
    }

    private void init(String installationPrefixPath) throws OSNotRecognizedException,
            ArchitectureNotRecognizedException,
            IOException,
            ExtractionException,
            InterruptedException,
            MissingSystemBinary,
            BuildFailureException {
        if(inited) {
            LOGGER.debug("already inited");
            return;
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
                    SupportedOS currentOS = DownloadTools.getCurrentOS();
                    DownloadCombi gitDownloadCombi = oSDownloadCombiGitMap.get(currentOS);
                    boolean notCanceled = DownloadTools.downloadFile(gitDownloadCombi,
                            downloadDialogParent,
                            "Downloading git", //downloadDialogTitle
                            "Downloading git", //downloadDialogLabelText
                            "Downloading git", //downloadDialogProgressBarText
                            skipMD5SumCheck,
                        ex1 -> {
                            return DownloadFailureCallbackReation.RETRY;
                        },
                        (String md5SumExpected, String md5SumActual) -> {
                            return MD5SumCheckUnequalsCallbackReaction.RETRY;
                        });
                    if(!notCanceled) {
                        return;
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
                    File extractionLocationDir = new File(gitDownloadCombi.getExtractionLocation());
                    assert extractionLocationDir.exists();
                    Process gitConfigureProcess = buildProcess(extractionLocationDir,
                            sh, "configure",
                            String.format("--prefix=%s", installationPrefixDir.getAbsolutePath()));
                    gitConfigureProcess.waitFor();
                    if(gitConfigureProcess.exitValue() != 0) {
                        handleBuilderFailure("git", BuildStep.CONFIGURE, gitConfigureProcess);
                    }
                    Process gitMakeProcess = buildProcess(extractionLocationDir,
                            installationPrefixPath,
                            make);
                    gitMakeProcess.waitFor();
                    if(gitMakeProcess.exitValue() != 0) {
                        handleBuilderFailure("git", BuildStep.MAKE, gitMakeProcess);
                    }
                    Process gitMakeInstallProcess = buildProcess(extractionLocationDir,
                            installationPrefixPath,
                            make,
                            "install");
                    gitMakeInstallProcess.waitFor();
                    if(gitMakeInstallProcess.exitValue() != 0) {
                        handleBuilderFailure("git", BuildStep.MAKE_INSTALL, gitMakeInstallProcess);
                    }
                    git = "git";
                        //is found in modified path of every process built with
                        //buildProcess
                    LOGGER.debug(String.format("using git command '%s'",
                            git));
            }
        }
        try {
            BinaryTools.validateBinary("git",
                    "git",
                    installationPrefixPath);
        } catch (BinaryValidationException ex) {
            assert false: "git exisistence check or installation failed";
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
                        Process jhbuildSourceRootCheckProcess = buildProcess(jhbuildCloneDir,
                                installationPrefixPath,
                                git,
                                "status");
                        LOGGER.debug("waiting for jhbuild source root check");
                        jhbuildSourceRootCheckProcess.waitFor();
                        if(jhbuildSourceRootCheckProcess.exitValue() != 0) {
                            throw new IllegalStateException(String.format("The "
                                    + "jhbuild clone directory '%s' already "
                                    + "exist, is not empty and is not a valid "
                                    + "git source root. This might be the "
                                    + "result of a failing previous checkout. "
                                    + "You need to check and eventually delete "
                                    + "the existing directory or specify "
                                    + "another download directory for JHBuild "
                                    + "Java wrapper.",
                                    jhbuildCloneDir.getAbsolutePath()));
                        }
                        needClone = false;
                    }
                    if(needClone) {
                        Process jhbuildCloneProcess = buildProcess(installationPrefixPath,
                                git,
                                "clone",
                                "git://git.gnome.org/jhbuild",
                                jhbuildCloneDir.getAbsolutePath());
                            //directory doesn't matter because target path is
                            //absolute
                        LOGGER.debug("waiting for jhbuild download");
                        jhbuildCloneProcess.waitFor();
                        if(jhbuildCloneProcess.exitValue() != 0) {
                            handleBuilderFailure("jhbuild", BuildStep.CLONE, jhbuildCloneProcess);
                        }
                        LOGGER.debug("jhbuild download finished");
                    }
                    Process jhbuildAutogenProcess = buildProcess(jhbuildCloneDir,
                            installationPrefixPath,
                            sh, "autogen.sh",
                            String.format("--prefix=%s", installationPrefixDir.getAbsolutePath()));
                        //autogen.sh runs configure
                    LOGGER.debug("waiting for jhbuild build bootstrap process");
                    jhbuildAutogenProcess.waitFor();
                    if(jhbuildAutogenProcess.exitValue() != 0) {
                        handleBuilderFailure("jhbuild", BuildStep.BOOTSTRAP, jhbuildAutogenProcess);
                    }
                    LOGGER.debug("jhbuild build bootstrap process finished");
                    Process jhbuildMakeProcess = buildProcess(jhbuildCloneDir,
                            installationPrefixPath,
                            make);
                    LOGGER.debug("waiting for jhbuild build process");
                    jhbuildMakeProcess.waitFor();
                    if(jhbuildMakeProcess.exitValue() != 0) {
                        handleBuilderFailure("jhbuild", BuildStep.MAKE, jhbuildMakeProcess);
                    }
                    LOGGER.debug("jhbuild build process finished");
                    Process jhbuildMakeInstallProcess = buildProcess(jhbuildCloneDir,
                            installationPrefixPath,
                            make, "install");
                    LOGGER.debug("waiting for jhbuild installation process");
                    jhbuildMakeInstallProcess.waitFor();
                    if(jhbuildMakeInstallProcess.exitValue() != 0) {
                        handleBuilderFailure("jhbuild", BuildStep.MAKE_INSTALL, jhbuildMakeInstallProcess);
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
    }

    private void handleBuilderFailure(String moduleName,
            BuildStep buildFailureStep,
            Process failedBuildProcess) throws BuildFailureException, IOException {
        String stdout = null;
        String stderr = null;
        if(!silenceStdout) {
            stdout = IOUtils.toString(failedBuildProcess.getInputStream(),
                    Charsets.UTF_8);
        }
        if(!silenceStderr) {
            stderr = IOUtils.toString(failedBuildProcess.getErrorStream(),
                    Charsets.UTF_8);
        }
        throw new BuildFailureException(moduleName,
                buildFailureStep,
                stdout,
                stderr);
    }

    public void installModuleset(InputStream modulesetInputStream,
            String moduleName) throws OSNotRecognizedException,
            ArchitectureNotRecognizedException,
            IOException,
            ExtractionException,
            InterruptedException,
            MissingSystemBinary,
            BuildFailureException {
        String installationPrefixPath = String.join(File.pathSeparator, System.getenv("PATH"),
                String.join(File.separator, installationPrefixDir.getAbsolutePath(), "bin"));
        init(installationPrefixPath);
        LOGGER.debug(String.format("building module %s with jhbuild command %s",
                moduleName,
                jhbuild));
        Process jhbuildBootstrapProcess = buildProcess(installationPrefixPath,
                jhbuild, "bootstrap");
            //directory doesn't matter
        LOGGER.debug("waiting for jhbuild bootstrap process");
        jhbuildBootstrapProcess.waitFor();
        if(jhbuildBootstrapProcess.exitValue() != 0) {
            throw new RuntimeException(); //@TODO:
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
        Process jhbuildProcess = buildProcess(installationPrefixPath,
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
            throw new RuntimeException(); //@TODO:
        }
        LOGGER.debug("jhbuild build process finished");
    }
}
