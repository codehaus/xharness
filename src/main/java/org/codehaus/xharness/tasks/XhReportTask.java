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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.optional.junit.DOMUtil;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.JAXPUtils;
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
public class XhReportTask extends Task implements XMLConstants {
    /** The default directory: <tt>&#046;</tt>. It is resolved from the project directory */
    public static final String DEFAULT_DIR = ".";

    /** the default file name: <tt>TESTS-TestSuites.xml</tt>. */
    public static final String DEFAULT_FILENAME = "TESTS-TestSuites.xml";

    /** the list of all filesets, that should contains the xml to aggregate. */
    protected Vector filesets = new Vector();

    /** the name of the result file. */
    protected String toFile;

    /** the directory to write the file to. */
    protected File toDir;

    protected Vector transformers = new Vector();

    private boolean failOnError = false;
    private Vector failedTests = new Vector();


    /**
     * Generate a report based on the document created by the merge.
     * 
     * @return The transformer.
     */
    public AggregateTransformer createReport() {
        AggregateTransformer transformer = new AggregateTransformer(this);
        transformers.addElement(transformer);
        return transformer;
    }

    /**
     * Set the name of the aggregegated results file. It must be relative
     * from the <tt>todir</tt> attribute. If not set it will use {@link #DEFAULT_FILENAME}
     * @param  value   the name of the file.
     * @see #setTodir(File)
     */
    public void setTofile(String value) {
        toFile = value;
    }

    /**
     * Set the destination directory where the results should be written. If not
     * set if will use {@link #DEFAULT_DIR}. When given a relative directory
     * it will resolve it from the project directory.
     * @param value    the directory where to write the results, absolute or
     * relative.
     */
    public void setTodir(File value) {
        toDir = value;
    }

    /**
     * If false, note errors to the output but keep going.
     * @param failonerror true or false
     */
    public void setFailOnError(boolean failonerror) {
        this.failOnError = failonerror;
    }

    /**
     * Add a new fileset containing the XML results to aggregate.
     * 
     * @param fs The new fileset of xml results.
     */
    public void addFileSet(FileSet fs) {
        filesets.addElement(fs);
    }

    /**
     * Aggregate all testsuites into a single document and write it to the
     * specified directory and file.
     * @throws  BuildException  thrown if there is a serious error while writing
     *          the document.
     */
    public void execute() throws BuildException {
        Element rootElement = createDocument();
        File destFile = getDestinationFile();
        // write the document
        try {
            writeDOMTree(rootElement.getOwnerDocument(), destFile);
        } catch (IOException e) {
            throw new BuildException("Unable to write test aggregate to '" + destFile + "'", e);
        }
        // apply transformation
        Enumeration enu = transformers.elements();
        while (enu.hasMoreElements()) {
            AggregateTransformer transformer =
                (AggregateTransformer)enu.nextElement();
            transformer.setXmlDocument(rootElement.getOwnerDocument());
            transformer.transform();
        }
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

    /**
     * Get the full destination file where to write the result. It is made of
     * the <tt>todir</tt> and <tt>tofile</tt> attributes.
     * @return the destination file where should be written the result file.
     */
    protected File getDestinationFile() {
        if (toFile == null) {
            toFile = DEFAULT_FILENAME;
        }
        if (toDir == null) {
            toDir = getProject().resolveFile(DEFAULT_DIR);
        }
        return new File(toDir, toFile);
    }

    /**
     * Get all <code>.xml</code> files in the fileset.
     *
     * @return all files in the fileset that end with a '.xml'.
     */
    protected File[] getFiles() {
        Vector v = new Vector();
        final int size = filesets.size();
        for (int i = 0; i < size; i++) {
            FileSet fs = (FileSet)filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            ds.scan();
            String[] f = ds.getIncludedFiles();
            for (int j = 0; j < f.length; j++) {
                String pathname = f[j];
                if (pathname.endsWith(".xml")) {
                    File file = new File(ds.getBasedir(), pathname);
                    file = getProject().resolveFile(file.getPath());
                    v.addElement(file);
                }
            }
        }

        File[] files = new File[v.size()];
        v.copyInto(files);
        return files;
    }

    //----- from now, the methods are all related to DOM tree manipulation

    /**
     * Write the DOM tree to a file.
     * @param doc the XML document to dump to disk.
     * @param file the filename to write the document to. Should obviouslly be a .xml file.
     * @throws IOException thrown if there is an error while writing the content.
     */
    protected void writeDOMTree(Document doc, File file) throws IOException {
        OutputStream out = null;
        PrintWriter wri = null;
        try {
            out = new FileOutputStream(file);
            wri = new PrintWriter(new OutputStreamWriter(out, "UTF8"));
            wri.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
            (new DOMElementWriter()).write(doc.getDocumentElement(), wri, 0, "  ");
            wri.flush();
            // writers do not throw exceptions, so check for them.
            if (wri.checkError()) {
                throw new IOException("Error while writing DOM content");
            }
        } finally {
            if (wri != null) {
                wri.close();
                out = null;
            }
            if (out != null) {
                out.close();
            }
        }
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
                            + "/" + elem.getAttribute(ATTR_NAME));
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

    /**
     * Subclass of {@link org.apache.tools.ant.taskdefs.optional.junit.AggregateTransformer}
     * that loads the XHarness XSLT stylesheets instead of the junit ones.
     *
     * @author  Gregor Heine
     */
    class AggregateTransformer 
        extends org.apache.tools.ant.taskdefs.optional.junit.AggregateTransformer {
        
        public AggregateTransformer(Task task) {
            super(task);
        }

        protected String getStylesheetSystemId() throws IOException {
            String xslname = "frames.xsl";

            if (styleDir == null) {
                URL url = getClass().getResource("/org/codehaus/xharness/xsl/" + xslname);

                if (url == null) {
                    throw new FileNotFoundException(
                            "Could not find jar resource /org/codehaus/xharness/xsl/" + xslname);
                }
                return url.toExternalForm();
            }
            File file = new File(styleDir, xslname);

            if (!file.exists()) {
                throw new FileNotFoundException("Could not find file '" + file + "'");
            }
            return JAXPUtils.getSystemId(file);
        }
    }
}
