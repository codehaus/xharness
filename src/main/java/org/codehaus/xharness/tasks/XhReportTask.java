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
import java.util.Enumeration;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.junit.AggregateTransformer;
import org.apache.tools.ant.taskdefs.optional.junit.DOMUtil;
import org.apache.tools.ant.taskdefs.optional.junit.XMLResultAggregator;
import org.apache.tools.ant.util.StringUtils;

import org.codehaus.xharness.log.XMLConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.SAXException;

/**
 * Aggregates all &lt;XHarness&gt; XML formatter testsuite data under
 * a specific directory and transforms the results via XSLT.
 * It is not particulary clean but
 * should be helpful while I am thinking about another technique.
 *
 * <p> The main problem is due to the fact that a JVM can be forked for a testcase
 * thus making it impossible to aggregate all testcases since the listener is
 * (obviously) in the forked JVM. A solution could be to write a
 * TestListener that will receive events from the TestRunner via sockets. This
 * is IMHO the simplest way to do it to avoid this file hacking thing.
 * 
 * Modified version of {@link org.apache.tools.ant.taskdefs.optional.junit.XMLResultAggregator}.
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 * @author Gregor Heine
 */
public class XhReportTask extends XMLResultAggregator implements XMLConstants {
    private boolean failOnError = false;
    private Vector failedTests = new Vector();


    /**
     * Generate a report based on the document created by the merge.
     * 
     * @return The transformer.
     */
    public AggregateTransformer createReport() {
        AggregateTransformer transformer = new XhAggregateTransformer(this);
        transformers.addElement(transformer);
        return transformer;
    }

    /**
     * If false, note errors to the output but keep going.
     * @param failonerror true or false
     */
    public void setFailOnError(boolean failonerror) {
        this.failOnError = failonerror;
    }

    /**
     * Aggregate all testsuites into a single document and write it to the
     * specified directory and file.
     * @throws  BuildException  thrown if there is a serious error while writing
     *          the document.
     */
    public void execute() throws BuildException {
        super.execute();
        if (failedTests.size() > 0) {
            log("The following tests failed:", Project.MSG_WARN);
            Enumeration e = failedTests.elements();
            while (e.hasMoreElements()) {
                log((String)e.nextElement(), Project.MSG_WARN);
            }
            if (failOnError) {
                throw new BuildException("Failed tests detected!");
            }
        }
    }
    
    public File getDestinationFile() {
        return super.getDestinationFile();
    }

    protected File[] getFiles() {
        return super.getFiles();
    }

    /**
     * <p> Create a DOM tree.
     * Has 'testsuites' as firstchild and aggregates all
     * testsuite results that exists in the base directory.
     * @return  the root element of DOM tree that aggregates all testsuites.
     */
    protected Element createDocument() {
        // create the dom tree
        DocumentBuilder builder = getDocumentBuilder();
        Document doc = builder.newDocument();
        Element rootElement = doc.createElement(RESULTS);
        doc.appendChild(rootElement);

        // get all files and add them to the document
        File[] files = getFiles();
        for (int i = 0; i < files.length; i++) {
            try {
                log("Parsing file: '" + files[i] + "'", Project.MSG_VERBOSE);
                //REVISIT there seems to be a bug in xerces 1.3.0 that doesn't like file object
                // will investigate later. It does not use the given directory but
                // the vm dir instead ? Works fine with crimson.
                Document testsuiteDoc = builder.parse("file:///" + files[i].getAbsolutePath());
                Element elem = testsuiteDoc.getDocumentElement();
                DOMUtil.importNode(rootElement, elem);
                String result = elem.getAttribute(ATTR_RESULT).toLowerCase();
                if ("warning".equals(result)
                        || "failed".equals(result)
                        || "invalid".equals(result)) {
                    failedTests.add(elem.getAttribute(ATTR_PARENT)
                            + "/" + elem.getAttribute(ATTR_TASK_NAME));
                }
            } catch (SAXException e) {
                // a testcase might have failed and write a zero-length document,
                // It has already failed, but hey.... mm. just put a warning
                log("The file " + files[i]
                    + " is not a valid XML document. It is possibly corrupted.",
                    Project.MSG_WARN);
                log(StringUtils.getStackTrace(e), Project.MSG_DEBUG);
            } catch (IOException e) {
                log("Error while accessing file " + files[i] + ": " + e.getMessage(), 
                    Project.MSG_ERR);
            }
        }
        return rootElement;
    }

    /**
     * Create a new document builder. Will issue an <tt>ExceptionInitializerError</tt>
     * if something is going wrong. It is fatal anyway.
     * @todo factorize this somewhere else. It is duplicated code.
     * @return a new document builder to create a DOM
     */
    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }
}
