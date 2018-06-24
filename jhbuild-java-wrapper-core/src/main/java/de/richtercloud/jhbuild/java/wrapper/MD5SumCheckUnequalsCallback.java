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
package de.richtercloud.jhbuild.java.wrapper;

/**
 *
 * @author richter
 */
@FunctionalInterface
public interface MD5SumCheckUnequalsCallback {
    public final static MD5SumCheckUnequalsCallback RETRY_5_TIMES = (String md5SumExpected,
            String md5SumActual,
            int numberOfRetries) -> {
        return numberOfRetries < 5
                ? MD5SumCheckUnequalsCallbackReaction.RETRY
                : MD5SumCheckUnequalsCallbackReaction.CANCEL;
    };

    /**
     * Allows to make a decision whether the retrieval/download ought to be
     * canceled or retried.
     *
     * @param expectedMD5Sum the expected MD5 checksum
     * @param actualMD5Sum the actual MD5 checksum
     * @param numberOfRetries the number of retries (starting with 0)
     * @return the decision
     */
    public MD5SumCheckUnequalsCallbackReaction run(String expectedMD5Sum,
            String actualMD5Sum,
            int numberOfRetries);
}
