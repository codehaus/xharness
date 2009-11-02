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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.types.DataType;

/**
 * Implementation of the <code>envset</code>Type. An EnvSet can contain multiple
 * environment variables specified as nested <code>env</code>elements and can be
 * referenced via the Ant reference infrastructure (id/refid attributes).
 * 
 * @author Gregor Heine
 */
public class EnvSet extends DataType {
    private HashMap loadedEnv = new HashMap();
    private List setEnv = new LinkedList();
    private EnvironmentVariable[] variables;
    private boolean noDefault = false;

    /**
     * Load all variables of the system's environment into this EnvSet.
     * 
     * @param load
     *            iff true, the system environment is added to this EnvSet.
     */
    public void setLoadenvironment(boolean load) {
        if (load) {
            log("Loading Environment...", Project.MSG_VERBOSE);
            Vector osEnv = Execute.getProcEnvironment();
            Iterator iter = osEnv.iterator();

            while (iter.hasNext()) {
                String entry = (String)iter.next();
                int pos = entry.indexOf('=');

                if (pos == -1) {
                    log("Ignoring: " + entry, Project.MSG_WARN);
                } else {
                    String key = entry.substring(0, pos);
                    String value = entry.substring(pos + 1);

                    loadedEnv.put(key, new EnvironmentVariable(key, value));
                }
            }
        }
    }

    /**
     * Ignore all loaded environment variables that are not also specified as
     * nested elements and would otherwise be added to the EnvSet by default.
     * Only effective of loadenvironment="true".
     * 
     * @param b If true, all default variables are ignored.
     */
    public void setNodefault(boolean b) {
        noDefault = b;
    }

    /**
     * Add a reference to another EnvSet to this EnvSet, adding the nested
     * EnvSet's environment variables to the parent environment.
     * 
     * @param envSet The nested EnvSet
     */
    public void addConfiguredEnvset(EnvSet envSet) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        EnvironmentVariable[] vars = envSet.getVariables(getProject());

        for (int i = 0; i < vars.length; i++) {
            addEnv(vars[i]);
        }
    }

    /**
     * Add a nested env element - an environment variable.
     * 
     * @param var
     *            the enviromnent variable.
     */
    public void addEnv(EnvironmentVariable var) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        setEnv.add(var);
    }

    /**
     * Get all enviromnent variables defined in this EnvSet.
     * 
     * @param p
     *            reference to the Project
     * @return array of this EnvSet's enviromnent variables.
     */
    public EnvironmentVariable[] getVariables(Project p) {
        if (isReference()) {
            return getRef(p).getVariables(p);
        } else {
            if (variables == null) {
                variables = createEnvironment();
            }
            return variables;
        }
    }

    /**
     * Performs the check for circular references and returns the referenced
     * EnvSet.
     * 
     * @param p The current Project.
     * @return The referenced EnvSet.
     */
    private EnvSet getRef(Project p) {
        if (!isChecked()) {
            Stack stk = new Stack();

            stk.push(this);
            dieOnCircularReference(stk, p);
        }

        Object o = getRefid().getReferencedObject(p);

        if (!(o instanceof EnvSet)) {
            throw new BuildException(getRefid().getRefId() + " doesn\'t denote an envset");
        } else {
            return (EnvSet)o;
        }
    }

    /**
     * Create this EnvSet's set of enviromnent variables, depending on nested
     * elements and attribute values.
     * 
     * @return An array of environment variables, representing the set environment.
     */
    private EnvironmentVariable[] createEnvironment() {
        HashMap intermediateEnv;

        if (noDefault) {
            intermediateEnv = new HashMap();
        } else {
            intermediateEnv = loadedEnv;
        }

        Iterator iter = setEnv.iterator();

        while (iter.hasNext()) {
            EnvironmentVariable setVar = (EnvironmentVariable)iter.next();
            String key = setVar.getKey();
            EnvironmentVariable lodedVar = (EnvironmentVariable)loadedEnv
                    .get(key);

            setVar.combineWith(lodedVar);
            intermediateEnv.put(key, setVar);
        }

        EnvironmentVariable[] empty = new EnvironmentVariable[0];

        return (EnvironmentVariable[])intermediateEnv.values().toArray(empty);
    }
}

