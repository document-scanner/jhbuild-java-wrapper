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

/**
 *
 * @author richter
 */
public enum BuildStep {
    /**
     * Refers to any local duplication of a remote source root, however it's
     * called in the used SCM.
     */
    /*
    internal implementation notes:
    - use git's name for duplicating a remote source root because it's the best
    SCM
    */
    CLONE,
    /**
     * Indicates a build failure in the bootstrapping script (formerly called
     * {@code autogen.sh} which is deprecated). Note that some bootstrapping
     * scripts call configure with the arguments passed to them.
     */
    BOOTSTRAP,
    CONFIGURE,
    MAKE,
    MAKE_CHECK,
    MAKE_INSTALL,
    SETUPTOOLS_BUILD,
    SETUPTOOLS_TEST,
    SETUPTOOLS_INSTALL
}
