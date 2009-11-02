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

import java.util.Hashtable;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.codehaus.xharness.exceptions.FatalException;
import org.codehaus.xharness.exceptions.ServiceVerifyException;

/**
 * The class to be placed in the ant type definition.
 * It is given a pointer to the template definition,
 * and makes a copy of the unknown element, substituting
 * the parameter values in attributes and text.
 * @since Ant 1.6
 */
public class ServiceInstance extends Task { 
    private static final int START = 1; 
    private static final int VERIFY = 2; 
    private static final int STOP = 4; 
    private ServiceDef serviceDef;
    private Action action = null;
    private String serviceName = null;

    /**
     * Called from ServiceDef.MyAntTypeDefinition#create().
     *
     * @param def a <code>ServiceDef</code> value.
     */
    public void setServiceDef(ServiceDef def) {
        serviceDef = def;
    }
    
    public ServiceDef getServiceDef() {
        return serviceDef;
    }
    
    public void setReference(String ref) {
        serviceName = ref;
    }
    
    public String getReference() {
        return serviceName;
    }
    
    public void setAction(String val) {
        if (action == null) {
            action = new Action();
        }
        action.addValue(val);
    }

    /**
     * Execute the templates instance.
     * Copies the unknown element, substitutes the attributes,
     * and calls perform on the unknown element.
     *
     */
    public void execute() {
        if (action == null) {
            action = new Action();
            action.addValue(START);
            action.addValue(VERIFY);
        }
        
        if (serviceDef == null) {
            if (serviceName == null) {
                throw new FatalException("No Service Definition found!");
            } else {
                ComponentHelper helper = ComponentHelper.getComponentHelper(getProject());
                AntTypeDefinition def = helper.getDefinition(serviceName);
                Object obj = def.create(getProject());
                if (obj instanceof ServiceInstance) {
                    serviceDef = ((ServiceInstance)obj).getServiceDef();
                } else {
                    throw new FatalException("No Service Definition found!");
                }
            }
        }
        
        ServiceVerifyTask verifyTask = serviceDef.getVerifyTask();

        
        if (action.hasValue(START)) {
            try {
                serviceDef.start();
            } catch (BuildException be) {
                verifyTask.setException(be);
                throw be;
            }
        }
        
        if (action.hasValue(VERIFY)) {
            log("Verifying service " + getTaskName(), Project.MSG_INFO);
            try {
                verifyTask.init();
                verifyTask.perform();
            } catch (Exception ex) {
                log("Verify of service " + getTaskName() + " failed", Project.MSG_INFO);
                throw new ServiceVerifyException(ex);
            }
        }
        
        if (action.hasValue(STOP)) {
            serviceDef.stop();
        }
    }

    public static class Action {
        private static Hashtable actionTable = new Hashtable();
        
        static {
            actionTable.put("start",  new Integer(ServiceInstance.START));
            actionTable.put("verify",  new Integer(ServiceInstance.VERIFY));
            actionTable.put("stop",   new Integer(ServiceInstance.STOP));
        }

        private int value;
        
        public void addValue(String valName) {
            String key = valName.toLowerCase();
            Integer i = (Integer)actionTable.get(key);
            if (i == null) {
                throw new BuildException(valName + " is not a legal value for this attribute");
            }
            value |= i.intValue();
        }
        
        public void addValue(int val) {
            value |= val;
        }
        
        public boolean hasValue(int val) {
            return (value & val) != 0;
        }
    }
}
