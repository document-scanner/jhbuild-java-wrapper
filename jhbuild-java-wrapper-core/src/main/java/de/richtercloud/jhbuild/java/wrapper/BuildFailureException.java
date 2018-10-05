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
 * An exception occuring in a build outside the automated steps of jhbuild.
 *
 * @author richter
 */
public class BuildFailureException extends Exception {
    private static final long serialVersionUID = 1L;

    public BuildFailureException(String moduleName,
            BuildStep buildFailureStep,
            String stdout,
            String stderr) {
        super(String.format("build failure in module '%s' in step %s%s%s",
                moduleName,
                buildFailureStep.name(),
                stdout != null
                        ? String.format(". Stdout was: '%s'",
                                stdout)
                        : "",
                stderr != null
                        ? String.format(". Stderr was: '%s'",
                                stderr)
                        : ""));
    }
}
