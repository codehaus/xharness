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

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.taskdefs.AntlibDefinition;

import org.codehaus.xharness.exceptions.AssertionWarningException;
import org.codehaus.xharness.exceptions.FatalException;

/**
 * Describe class <code>ServiceDef</code> here.
 *
 * @since Ant 1.6
 */
public class ServiceDef extends AntlibDefinition  {
    private static final int UNSTARTED = 0;
    private static final int RUNNING = 1;
    private static final int WARNING = 2;
    private static final int FAILED = 3;
    private static final int STOPPED = 4;

    private ServiceGroupTask  nestedStart;
    private ServiceVerifyTask nestedVerify;
    private ServiceGroupTask  nestedStop;
    private String            serviceName;
    private int               state = UNSTARTED;

    /**
     * Set the name of the service definition.
     * 
     * @param name The name of the service.
     */
    public void setName(String name) {
        this.serviceName = name;
    }

    /**
     * Get the name of the service definition.
     * 
     * @return The name of the service.
     */
    public String getName() {
        return serviceName;
    }
    
    public boolean wasStopped() {
        if (state == STOPPED) {
            state = UNSTARTED;
            return true;
        }
        return false;
    }

    /**
     * This is the sequential nested element of the macrodef.
     *
     * @return a sequential element to be configured.
     */
    public ServiceGroupTask createStart() {
        if (nestedStart != null) {
            throw new FatalException("Only one start allowed");
        }
        nestedStart = new ServiceGroupTask();
        nestedStart.setName("start");
        return nestedStart;
    }

    public ServiceVerifyTask createVerify() {
        if (nestedVerify != null) {
            throw new FatalException("Only one verify allowed");
        }
        nestedVerify = new ServiceVerifyTask();
        nestedVerify.setName("verify");
        return nestedVerify;
    }

    public ServiceGroupTask createStop() {
        if (nestedStop != null) {
            throw new FatalException("Only one stop allowed");
        }
        nestedStop = new ServiceGroupTask();
        nestedStop.setName("stop");
        return nestedStop;
    }

    /**
     * Convert the nested sequential to an unknown element.
     * 
     * @return the nested sequential as an unknown element.
     */
    public ServiceVerifyTask getVerifyTask() {
        return nestedVerify;
    }
    
    public void start() throws BuildException {
        if (state == UNSTARTED) {
            log("Starting service " + getName(), Project.MSG_INFO);
            try {
                nestedStart.perform();
                state = RUNNING;
            } catch (AssertionWarningException ex) {
                state = WARNING;
                throw new AssertionWarningException("Assertion failed during startup of Service.");
            } catch (BuildException ex) {
                state = FAILED;
                throw new BuildException("Startup of Service failed.");
            } finally {
                if (state != RUNNING) {
                    log("Start of service " + getName() + " failed", Project.MSG_INFO);
                }
            }
        }
    }
    
    public void stop() throws BuildException {
        try {
            if (state == RUNNING && nestedStop != null) {
                log("Stopping service " + getName(), Project.MSG_INFO);
                nestedStop.perform();
            }
        } catch (AssertionWarningException ex) {
            log("Stop of service " + getName() + " failed", Project.MSG_INFO);
            throw new AssertionWarningException("Assertion failed during shutdown of Service.");
        } catch (BuildException ex) {
            log("Stop of service " + getName() + " failed", Project.MSG_INFO);
            throw new BuildException("Shutdown of Service failed.");
        } finally {
            int prevState = state;
            state = STOPPED;
            if (prevState == FAILED) {
                throw new BuildException("Startup of Service failed.");
            }
            if (prevState == WARNING) {
                throw new AssertionWarningException("Assertion failed during startup of Service.");
            }
        }
    }

    /**
     * Create a new ant type based on the embedded tasks and types.
     *
     */
    public void execute() {
        if (serviceName == null) {
            throw new BuildException("Name not specified");
        } else  if (nestedStart == null) {
            throw new BuildException("Missing start element");
        } else if (nestedVerify == null) {
            nestedVerify = new ServiceVerifyTask();
            nestedVerify.setName(ServiceVerifyTask.DUMMY);
            nestedVerify.setLocation(getLocation());
            nestedVerify.setProject(getProject());
            nestedVerify.setTaskName(ServiceVerifyTask.DUMMY);
        }
        
        serviceName = ProjectHelper.genComponentName(getURI(), serviceName);
        log("Service " + serviceName + " defined", Project.MSG_INFO);

        MyAntTypeDefinition def = new MyAntTypeDefinition(this);
        def.setName(serviceName);
        def.setClass(ServiceInstance.class);

        ComponentHelper helper = ComponentHelper.getComponentHelper(
            getProject());

        helper.addDataTypeDefinition(def);
    }

    /**
     * similar equality method for macrodef, ignores project and
     * runtime info.
     *
     * @param obj an <code>Object</code> value
     * @return a <code>boolean</code> value
     */
    public boolean similar(Object obj) {
        if (obj == this) {
            return true;
        }
        return false;

//        if (obj == null) {
//            return false;
//        }
//        if (!obj.getClass().equals(getClass())) {
//            return false;
//        }
//        ServiceDef other = (ServiceDef) obj;
//        if (serviceName == null) {
//            return other.serviceName == null;
//        }
//        if (!serviceName.equals(other.serviceName)) {
//            return false;
//        }
//        if (getURI() == null || getURI().equals("")
//            || getURI().equals(ProjectHelper.ANT_CORE_URI)) {
//            if (!(other.getURI() == null || other.getURI().equals("")
//                  || other.getURI().equals(ProjectHelper.ANT_CORE_URI))) {
//                return false;
//            }
//        } else {
//            if (!getURI().equals(other.getURI())) {
//                return false;
//            }
//        }
//
//        if (!nestedStart.similar(other.nestedStart)) {
//            return false;
//        }
//        
//        if (nestedVerify == null || other.nestedVerify == null) {
//            if (nestedVerify != other.nestedVerify) {
//                return false;
//            }
//        } else {
//            if (!nestedVerify.similar(other.nestedVerify)) {
//                return false;
//            }
//        }
//        
//        if (nestedStop == null || other.nestedStop == null) {
//            if (nestedStop != other.nestedStop) {
//                return false;
//            }
//        } else {
//            if (!nestedStop.similar(other.nestedStop)) {
//                return false;
//            }
//        }
//        return true;
    }

    /**
     * extends AntTypeDefinition, on create of the object, the template service 
     * definition is given.
     */
    private static class MyAntTypeDefinition extends AntTypeDefinition {
        private ServiceDef    serviceDef;

        /**
         * Creates a new <code>MyAntTypeDefinition</code> instance.
         *
         * @param def a <code>ServiceDef</code> value.
         */
        public MyAntTypeDefinition(ServiceDef def) {
            this.serviceDef = def;
        }

        /**
         * create an instance of the definition.
         * The instance may be wrapped in a proxy class.
         * @param project the current project
         * @return the created object
         */
        public Object create(Project project) {
            Object o = super.create(project);
            if (o == null) {
                return null;
            }
            ((ServiceInstance)o).setServiceDef(serviceDef);
            return o;
        }

        /**
         * Equality method for this definition.
         *
         * @param other another definition
         * @param project the current project
         * @return true if the definitions are the same
         */
        public boolean sameDefinition(AntTypeDefinition other, Project project) {
            if (!super.sameDefinition(other, project)) {
                return false;
            }
            MyAntTypeDefinition otherDef = (MyAntTypeDefinition)other;
            return serviceDef.similar(otherDef.serviceDef);
        }

        /**
         * Similar method for this definition.
         *
         * @param other another definition
         * @param project the current project
         * @return true if the definitions are the same
         */
        public boolean similarDefinition(
            AntTypeDefinition other, Project project) {
            if (!super.similarDefinition(other, project)) {
                return false;
            }
            MyAntTypeDefinition otherDef = (MyAntTypeDefinition)other;
            return serviceDef.similar(otherDef.serviceDef);
        }
    }
}
