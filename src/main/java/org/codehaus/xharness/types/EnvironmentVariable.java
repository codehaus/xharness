/*
 * Copyright 2006 IONA Technologies
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.codehaus.xharness.types;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.codehaus.xharness.exceptions.FatalException;

/**
 * Extension of the Ant Environment.Variable class. Adds a set of new
 * constructors that initialize the Varaible with default valuesand adds the
 * functionality to merge two Variable values.
 * 
 * @author Gregor Heine
 */
public class EnvironmentVariable extends Environment.Variable {
    private static final int REPLACE = 0;
    private static final int PREPEND = 1;
    private static final int APPEND = 2;
    private int replaceMode = REPLACE;
    private boolean isPath = false;

    /**
     * Default constructor. Creates an empty Environment Varaible (with no key
     * and no value)
     */
    public EnvironmentVariable() {
        super();
    }

    /**
     * Constructor that takes a default key, value pair.
     * 
     * @param key
     *            the key of the variable
     * @param value
     *            the value of the variable
     */
    public EnvironmentVariable(String key, String value) {
        super();
        setKey(key);
        setValue(value);
    }

    /**
     * Constructor that takes a default key, value (File type) pair.
     * 
     * @param key
     *            the key of the variable
     * @param file
     *            the value of the variable in File form
     */
    public EnvironmentVariable(String key, File file) {
        super();
        setKey(key);
        setFile(file);
    }

    /**
     * Constructor that takes a default key, value (Path type) pair.
     * 
     * @param key
     *            the key of the variable
     * @param path
     *            the value of the variable in Path form
     */
    public EnvironmentVariable(String key, Path path) {
        super();
        setKey(key);
        setPath(path);
    }

    /**
     * Set the value of the variable as a Path type.
     * 
     * @param path
     *            the value of the variable in Path form
     */
    public void setPath(Path path) {
        super.setPath(path);
        isPath = true;
    }

    /**
     * Set the merge policy to "prepend". If the variable is merged with a
     * second variable (that has the same key), the value of the variable is
     * prepended to the value of the second variable.
     * 
     * @param prepend If true, prepend the value of this variable if merged.
     * @throws BuildException If append has been set as well.
     */
    public void setPrepend(boolean prepend) throws BuildException {
        if (prepend) {
            setMode(PREPEND);
        } else {
            replaceMode = REPLACE;
        }
    }

    /**
     * Set the merge policy to "append". If the variable is merged with a second
     * variable (that has the same key), the value of the variable is appended
     * to the value of the second variable.
     * 
     * @param append If true, append the value of this variable if merged.
     * @throws BuildException If prepend has been set as well.
     */
    public void setAppend(boolean append) throws BuildException {
        if (append) {
            setMode(APPEND);
        } else {
            replaceMode = REPLACE;
        }
    }

    /**
     * Combine this variable with a second one. Depending on the set merge
     * policy, the value of the second variable is either discarded, appended or
     * prepended to the value of this variable.
     * 
     * @param other
     *            the second variable
     * @return the varaible containing the merged value
     */
    public Environment.Variable combineWith(Environment.Variable other) {
        if (other != null && other.getKey().equals(getKey())) {
            String separator = isPath ? File.pathSeparator : "";

            if (replaceMode == APPEND) {
                setValue(other.getValue() + separator + getValue());
            } else if (replaceMode == PREPEND) {
                setValue(getValue() + separator + other.getValue());
            }
        }
        return this;
    }

    /**
     * Set the merge policy to either REPLACE, APPEND or PREPEND.
     * 
     * @param mode The merge mode for pre-existing environment variables. 
     *             One of replace, append or prepend.
     * @throws BuildException If both prepend and append have been set.
     */
    private void setMode(int mode) throws BuildException {
        if (replaceMode != REPLACE) {
            throw new FatalException(
                    "Only \"prepend\" OR \"append\" equals \"true\" allowed!");
        }
        replaceMode = mode;
    }
}

