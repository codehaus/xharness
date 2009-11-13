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
import java.io.IOException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.util.FileUtils;

import org.codehaus.xharness.exceptions.FatalException;
import org.codehaus.xharness.util.IncludeProjectHelper;


/**
 * Task to import another build file into the current project.
 * Derived from and inspired by Ant 1.6 <code>import</code> Task.
 *
 * @author  Gregor Heine
 */
public class IncludeTask extends Task implements TaskContainer {
    private static final FileUtils FILE_UTILS = FileUtils.newFileUtils();
    private Task nestedTask;
    private String file;

    /**
     * the name of the file to import. How relative paths are resolved is still
     * in flux: use absolute paths for safety.
     * @param f the name of the file
     */
    public void setFile(String f) {
        file = f;
    }

    /**
     * Add a nested task to this TaskContainer.
     *
     * @param task Nested task to execute sequentially.
     */
    public void addTask(Task task) {
        if (nestedTask != null) {
            throw new FatalException("Invalid XML");
        }
        nestedTask = task;
    }
    
    public Task getNestedTask() {
        return nestedTask;
    }

   /**
     *  This relies on the task order model.
     */
    public void execute() {
        if (file == null) {
            throw new FatalException("import requires file attribute");
        }

        File buildFileParent = getProject().getBaseDir();

        buildFileParent = new File(buildFileParent.getAbsolutePath());

        log("Importing file " + file + " from " + buildFileParent.getAbsolutePath(), 
            Project.MSG_VERBOSE);

        // Paths are relative to the build file they're imported from,
        // *not* the current directory (same as entity includes).

        File importedFile = FILE_UTILS.resolveFile(buildFileParent, file);

        if (!importedFile.exists()) {
            String message = "Cannot find " + file 
                           + " imported from " + buildFileParent.getAbsolutePath();

            throw new FatalException(message);
        }

        importedFile = new File(getPath(importedFile));

        IncludeProjectHelper importHelper = new IncludeProjectHelper(getOwningTarget(), this);

        String savedAntFile = getProject().getProperty("ant.file");

        getProject().setUserProperty("ant.file", importedFile.getAbsolutePath());
        log("old ant.file=" + savedAntFile, Project.MSG_VERBOSE);
        log("new ant.file=" + getProject().getProperty("ant.file"), Project.MSG_VERBOSE);

        String savedBaseDir = getProject().getProperty("basedir");

        getProject().setBasedir(importedFile.getParent());
        log("old basedir=" + savedBaseDir, Project.MSG_VERBOSE);
        log("new basedir=" + getProject().getProperty("basedir"), Project.MSG_VERBOSE);

        importHelper.parse(getProject(), importedFile);
        try {
            nestedTask.perform();
        } finally {
            getProject().setUserProperty("ant.file", savedAntFile);
            log("reset ant.file=" + getProject().getProperty("ant.file"), Project.MSG_VERBOSE);
            getProject().setBasedir(savedBaseDir);
            log("reset basedir=" + getProject().getProperty("basedir"), Project.MSG_VERBOSE);
        }
    }

    public String toString() {
        return "include " + file;
    }

    private static String getPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }
}
