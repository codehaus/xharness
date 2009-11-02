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

package org.codehaus.xharness.tasks;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.PathTokenizer;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.codehaus.xharness.exceptions.FatalException;



/**
 * Implementation of the {org.codehaus.xharness.tasks.WhichTask}.
 *
 * @author  Robert Delaney
 */
public class WhichTask extends Task {
    private String executable;
    private String path;
    private String property;
    private boolean failonerror = false;

    /**
     * Set the name of the executable, the environment path and our return fullfilename.
     * The Which Task looks up an executable in a PATH-like String and sets a property
     * with the fully qualified path name of the executable.
     * 
     * @param exec The executable to be resolved.
     */
    public void setExecutable(String exec) {
        executable = exec;
    }

    public void setPath(String envpath) {
        path = envpath;
    }

    public void setProperty(String p) {
        property = p;
    }

    public void setFailOnError(boolean fail) {
        failonerror = fail;
    }

    /**
     * Do the execution of the task.
     * 
     * @throws BuildException If a reuqired attribute isn't set.
     */
    public void execute() throws BuildException {
        if (executable == null) {
            throw new FatalException("The executable attribute is required.");
        } else if (path == null) {
            throw new FatalException("The path attribute is required.");
        } else if (property == null) {
            throw new FatalException("The property attribute is required.");
        }

        boolean found = false;
        String executableExtn; // needed for file extension
        String[] extn;

        if (System.getProperty("os.name", "").toLowerCase().startsWith("win")
                && executable.indexOf('.') == -1) {
            // different possible file extensions on Windows
            extn = new String[] {".com", ".exe", ".bat", ".sh"};
        } else {
            extn = new String[] {""};
        }

        // PathTokenizer
        PathTokenizer strTok = new PathTokenizer(path);

        while (strTok.hasMoreTokens()) {
            String mytoken = strTok.nextToken();

            mytoken = mytoken.replace('/', File.separatorChar);
            mytoken = mytoken.replace('\\', File.separatorChar);

            // Append Executable
            for (int i = 0; i < extn.length; i++) {
                executableExtn = executable.concat(extn[i]);

                log("Searching for file:  " + executableExtn, Project.MSG_VERBOSE);
                log("in directory: " + mytoken, Project.MSG_VERBOSE);

                String mytoken2 = mytoken + File.separatorChar + executableExtn;

                // Check for existence of file
                boolean exists = new File(mytoken2).exists();

                if (exists) {
                    log("Found it at :   " + mytoken, Project.MSG_VERBOSE);

                    getProject().setNewProperty(property, mytoken2);
                    found = true;
                    break;
                }
            }
            if (found) {
                // we've found our file so no need for more iterations
                break;
            }
        }

        if (!found) {
            log("File not found in path:  " + path, Project.MSG_VERBOSE);
            if (failonerror) {
                throw new BuildException("File " + executable + " not found in path " + path);
            }
        }
    }

}
