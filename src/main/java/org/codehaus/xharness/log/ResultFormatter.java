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

package org.codehaus.xharness.log;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.DOMElementWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * Writes output and result of a Task to an XML file. 
 * Derived from and inspired by junit's
 * {@link org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter}
 *
 * @author Gregor Heine
 */

public class ResultFormatter implements XMLConstants {

    /**
     * The XML document.
     */
    private Document doc;
    
    /**
     *  The directory into which Result XML files are written.
     */
    private File basedir;
    
    /**
     * The wrapper for the whole testsuite.
     */
    private Element rootElement;

    /**
     * Constructs a new ResultFormatter.
     * 
     * @param dir The output directory into which the XML files are written.
     */
    public ResultFormatter(File dir) {
        this.basedir = dir;
    }

    /**
     * Write the result of a Task to a file in the output directory.
     * 
     * @param logger The Task's logger.
     * @param result The result of the Task.
     * @param description A description of the Result.
     * @param time The duration of the Task.
     * @throws BuildException If an error occurs while writing the file.
     */
    public synchronized void writeResults(TaskLogger logger, 
                                          int result, 
                                          String description, 
                                          long time) throws BuildException {
        doc = getDocumentBuilder().newDocument();
        int taskType = logger.getTaskType();
        switch (taskType) {
            case Result.PROCESS_TASK:
            case Result.OTHER_TASK: 
                rootElement = doc.createElement(TASK);
                break;
                
            case Result.SERVICE:
                rootElement = doc.createElement(SERVICE);
                break;
                
            case Result.START:
                rootElement = doc.createElement(START);
                break;
                
            case Result.VERIFY:
                rootElement = doc.createElement(VERIFY);
                break;
                
            case Result.STOP:
                rootElement = doc.createElement(STOP);
                break;
                
            case Result.TESTGROUP:
                rootElement = doc.createElement(GROUP);
                break;
                
            case Result.TESTCASE:
                rootElement = doc.createElement(TEST);
                break;
                
            case Result.XHARNESS:
                rootElement = doc.createElement(XHARNESS);
                break;
                
            case Result.LINK:
                rootElement = doc.createElement(LINK);
                break;
                
            default:
                throw new BuildException("Invalid Task Type: " + taskType);
        }

        addAttribute(ATTR_ORDERID, Integer.toString(logger.getId()));
        addAttribute(ATTR_RESULT, printResult(result));
        
        if (taskType != Result.XHARNESS) {
            addAttribute(ATTR_PARENT, logger.getParentName());
        }
        
        addAttribute(ATTR_TASK_NAME, logger.getName());
        addAttribute(ATTR_FULL_NAME, logger.getFullName());
        addAttribute(ATTR_TIME, Float.toString((float)time / (float)1000.0));

        if (taskType == Result.TESTCASE) {
            addAttribute(ATTR_OWNER, logger.getOwner());
        }
        
        addAttribute(ATTR_REFERENCE, logger.getReference());
        
        Element descrElement = doc.createElement(DESCRIPTION);
        addText(descrElement, description);
        rootElement.appendChild(descrElement);

        if (taskType == Result.PROCESS_TASK) {
            addAttribute(ATTR_RETVAL, Integer.toString(logger.getRetVal()));
            Element execElement = doc.createElement(COMMAND);
            String command = logger.getCommand();
            addText(execElement, command);
            rootElement.appendChild(execElement);
        }

        LineBuffer lines = logger.getLineBuffer();

        Iterator iter = lines.iterator();
        if (iter.hasNext()) {
            LogLine previous = (LogLine)iter.next();
            StringBuffer text = new StringBuffer(previous.getText());
            while (iter.hasNext()) {
                LogLine line = (LogLine)iter.next();
                if (line.getPriority() != previous.getPriority()) {
                    Element outElement = doc.createElement(OUTPUT);
                    outElement.setAttribute(ATTR_LOGLEVEL, 
                                            Integer.toString(previous.getPriority()));
                    addText(outElement, text.toString());
                    rootElement.appendChild(outElement);
                    text = new StringBuffer(line.getText());
                } else {
                    text.append("\n");
                    text.append(line.getText());
                }
                previous = line;
            }
            Element outElement = doc.createElement(OUTPUT);
            outElement.setAttribute(ATTR_LOGLEVEL, Integer.toString(previous.getPriority()));
            addText(outElement, text.toString());
            rootElement.appendChild(outElement);
        }

        String filename = genFileName(taskType, logger.getFullName());
        File outfile = new File(basedir, filename + ".xml");
        int count = 1;
        while (outfile.exists()) {
            outfile = new File(basedir, filename + "_" + (count++) + ".xml");
        }
        OutputStream out = null;

        try {
            out = new FileOutputStream(outfile);
        } catch (IOException e) {
            throw new BuildException("Unable to write results file "
                                   + outfile.getName() + ": " + outfile);
        }

        Writer wri = null;

        try {
            wri = new OutputStreamWriter(out, "UTF8");
            wri.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
            (new DOMElementWriter()).write(rootElement, wri, 0, "  ");
            wri.flush();
        } catch (Exception exc) {
            throw new BuildException("Unable to write log file", exc);
        } finally {
            if (wri != null) {
                try {
                    wri.close();
                } catch (IOException e) {
                    //ignore
                }
            }
            try {
                out.close();
            } catch (IOException e) {
                //ignore
            }
        }
    }

    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }

    private void addText(Element elem, String text) {
        Text textNode = doc.createTextNode(text);

        elem.appendChild(textNode);
        // elem.appendChild(m_doc.createCDATASection(text));
    }

    private String printResult(int res) {
        switch (res) {
            case Result.SKIPPED:
                return "Skipped";
    
            case Result.PASSED:
                return "Passed";
    
            case Result.WARNING:
                return "Warning";
    
            case Result.FAILED:
                return "Failed";
    
            default:
                return "Invalid";
        }
    }

    private String genFileName(int type, String name) {
        StringBuffer ret = new StringBuffer();

        switch (type) {
            case Result.PROCESS_TASK:
            case Result.OTHER_TASK:
                ret.append("TASK");
                break;
            case Result.SERVICE:
            case Result.START:
            case Result.VERIFY:
            case Result.STOP:
                ret.append("SVCS");
                break;
            case Result.TESTCASE:
                ret.append("TEST");
                break;
            case Result.TESTGROUP:
                ret.append("GROUP");
                break;
            case Result.XHARNESS:
                ret.append("XHARNESS");
                break;
            case Result.LINK:
                ret.append("LNK");
                break;
            default:
                ret.append("UNKN");
        }
        ret.append("_");

        if (name != null && !"".equals(name)) {
            name = name.replace('/', '_').replace('\\', '_');
            if (name.startsWith("_")) {
                name = name.substring(1);
            }
            ret.append(name);
        }

        return ret.toString();
    }
    
    private void addAttribute(String name, String value) {
        rootElement.setAttribute(name, value == null ? "" : value);
    }
}
