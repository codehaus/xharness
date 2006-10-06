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

package org.codehaus.xharness.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskAdapter;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.util.JAXPUtils;
import org.xml.sax.AttributeList;
import org.xml.sax.DocumentHandler;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.XMLReaderAdapter;


/**
 * Customized ProjectHelper that allows the import of Ant buildfiles into the
 * current project. Used by {@link IncludeTask}.
 *
 * @author  Gregor Heine
 */
public class IncludeProjectHelper extends ProjectHelper {
    private Target target;
    private TaskContainer parent;


    /**
     * SAX 1 style parser used to parse the given file. This may
     * in fact be a SAX 2 XMLReader wrapped in an XMLReaderAdapter.
     */
    private org.xml.sax.Parser parser;

    /** The project to configure. */
    private Project project;

    /** The configuration file to parse. */
    private File buildFile;

    /**
     * Parent directory of the build file. Used for resolving entities
     * and setting the project's base directory.
     */
    private File buildFileParent;

    /**
     * Locator for the configuration file parser.
     * Used for giving locations of errors etc.
     */
    private Locator locator;

    public IncludeProjectHelper(Target t, TaskContainer p) {
        super();
        this.target = t;
        this.parent = p;
    }

    /**
     * Parses the project file, configuring the project as it goes.
     *
     * @param p project instance to be configured.
     * @param source the source from which the project is read.
     * @exception BuildException if the configuration is invalid or cannot
     *                           be read.
     */
    public void parse(Project p, Object source) throws BuildException {
        if (!(source instanceof File)) {
            throw new BuildException("Only File source supported by default plugin");
        }
        FileInputStream inputStream = null;
        InputSource inputSource = null;

        this.project = p;
        this.buildFile = new File(((File)source).getAbsolutePath());
        buildFileParent = new File(this.buildFile.getParent());

        try {
            try {
                parser = JAXPUtils.getParser();
            } catch (BuildException e) {
                parser = new XMLReaderAdapter(JAXPUtils.getXMLReader());
            }

            String uri = "file:" + buildFile.getAbsolutePath().replace('\\', '/');

            for (int index = uri.indexOf('#'); index != -1; index = uri.indexOf('#')) {
                uri = uri.substring(0, index) + "%23" + uri.substring(index + 1);
            }

            inputStream = new FileInputStream(buildFile);
            inputSource = new InputSource(inputStream);
            inputSource.setSystemId(uri);
            project.log("parsing buildfile " + buildFile + " with URI = " + uri, 
                        Project.MSG_VERBOSE);
            HandlerBase hb = new RootHandler();
//            HandlerBase hb = new TaskHandler(null, parent, null, target);

            parser.setDocumentHandler(hb);
            parser.setEntityResolver(hb);
            parser.setErrorHandler(hb);
            parser.setDTDHandler(hb);
            parser.parse(inputSource);
        } catch (SAXParseException exc) {
            Location location = new Location(exc.getSystemId(), 
                                             exc.getLineNumber(), 
                                             exc.getColumnNumber());

            Throwable t = exc.getException();

            if (t instanceof BuildException) {
                BuildException be = (BuildException)t;

                if (be.getLocation() == Location.UNKNOWN_LOCATION) {
                    be.setLocation(location);
                }
                throw be;
            }

            throw new BuildException(exc.getMessage(), t, location);
        } catch (SAXException exc) {
            Throwable t = exc.getException();

            if (t instanceof BuildException) {
                throw (BuildException)t;
            }
            throw new BuildException(exc.getMessage(), t);
        } catch (FileNotFoundException exc) {
            throw new BuildException(exc);
        } catch (UnsupportedEncodingException exc) {
            throw new BuildException("Encoding of project file is invalid.", exc);
        } catch (IOException exc) {
            throw new BuildException("Error reading project file: " + exc.getMessage(), exc);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioe) {
                    // ignore this
                }
            }
        }
    }

    /**
     * The common superclass for all SAX event handlers used to parse
     * the configuration file. Each method just throws an exception,
     * so subclasses should override what they can handle.
     *
     * Each type of XML element (task, target, etc.) in Ant has
     * a specific subclass.
     *
     * In the constructor, this class takes over the handling of SAX
     * events from the parent handler and returns
     * control back to the parent in the endElement method.
     */
    class AbstractHandler extends HandlerBase {

        /**
         * Previous handler for the document.
         * When the next element is finished, control returns
         * to this handler.
         */
        protected DocumentHandler parentHandler;

        /**
         * Creates a handler and sets the parser to use it
         * for the current element.
         *
         * @param parenthandler The handler which should be restored to the
         *                      parser at the end of the element.
         *                      Must not be <code>null</code>.
         */
        public AbstractHandler(DocumentHandler parenthandler) {
            this.parentHandler = parenthandler;

            // Start handling SAX events
            parser.setDocumentHandler(this);
        }

        /**
         * Handles the start of an element. This base implementation just
         * throws an exception.
         *
         * @param tag The name of the element being started.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         *
         * @exception SAXParseException if this method is not overridden, or in
         *                              case of error in an overridden version
         */
        public void startElement(String tag, AttributeList attrs) throws SAXParseException {
            throw new SAXParseException("Unexpected element \"" + tag + "\"", locator);
        }

        /**
         * Handles text within an element. This base implementation just
         * throws an exception.
         *
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         *
         * @exception SAXParseException if this method is not overridden, or in
         *                              case of error in an overridden version
         */
        public void characters(char[] buf, int start, int count) throws SAXParseException {
            String s = new String(buf, start, count).trim();

            if (s.length() > 0) {
                throw new SAXParseException("Unexpected text \"" + s + "\"", locator);
            }
        }

        /**
         * Called when this element and all elements nested into it have been
         * handled.
         */
        protected void finished() {
        }

        /**
         * Handles the end of an element. Any required clean-up is performed
         * by the finished() method and then the original handler is restored to
         * the parser.
         *
         * @param name The name of the element which is ending.
         *             Will not be <code>null</code>.
         *
         * @exception SAXException in case of error (not thrown in
         *                         this implementation)
         *
         * @see #finished()
         */
        public void endElement(String name) throws SAXException {

            finished();
            // Let parent resume handling SAX events
            parser.setDocumentHandler(parentHandler);
        }
    }


    /**
     * Handler for the root element. Its only child must be the "project" element.
     */
    class RootHandler extends HandlerBase {

        /**
         * Resolves file: URIs relative to the build file.
         *
         * @param publicId The public identifer, or <code>null</code>
         *                 if none is available. Ignored in this
         *                 implementation.
         * @param systemId The system identifier provided in the XML
         *                 document. Will not be <code>null</code>.
         * @return The InputSource for the entity.
         */
        public InputSource resolveEntity(String publicId, String systemId) {

            project.log("resolving systemId: " + systemId, Project.MSG_VERBOSE);

            if (systemId.startsWith("file:")) {
                String path = systemId.substring(5);
                int index = path.indexOf("file:");

                // we only have to handle these for backward compatibility
                // since they are in the FAQ.
                while (index != -1) {
                    path = path.substring(0, index) + path.substring(index + 5);
                    index = path.indexOf("file:");
                }

                String entitySystemId = path;

                index = path.indexOf("%23");
                // convert these to #
                while (index != -1) {
                    path = path.substring(0, index) + "#" + path.substring(index + 3);
                    index = path.indexOf("%23");
                }

                File file = new File(path);

                if (!file.isAbsolute()) {
                    file = new File(buildFileParent, path);
                }

                try {
                    InputSource inputSource = new InputSource(new FileInputStream(file));

                    inputSource.setSystemId("file:" + entitySystemId);
                    return inputSource;
                } catch (FileNotFoundException fne) {
                    project.log(file.getAbsolutePath() + " could not be found", Project.MSG_WARN);
                }
            }
            // use default if not file or file not found
            return null;
        }

        /**
         * Handles the start of a project element. A project handler is created
         * and initialised with the element name and attributes.
         *
         * @param tag The name of the element being started.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         *
         * @exception SAXParseException if the tag given is not
         *                              <code>"project"</code>
         */
        public void startElement(String tag, AttributeList attrs) throws SAXParseException {
            if (!tag.equals("project") && !tag.equals("target")) {
                new TaskHandler(this, parent, null).init(tag, attrs);
            } else {
                throw new SAXParseException("Config file is not of expected XML type", locator);
            }
        }

        /**
         * Sets the locator in the project helper for future reference.
         *
         * @param l The locator used by the parser.
         *          Will not be <code>null</code>.
         */
        public void setDocumentLocator(Locator l) {
            locator = l;
        }
    }


    /**
     * Handler for all task elements.
     */
    class TaskHandler extends AbstractHandler {
        /**
         * Container for the task, if any. If target is
         * non-<code>null</code>, this must be too.
         */
        private TaskContainer container;

        /**
         * Task created by this handler.
         */
        private Task task;

        /**
         * Wrapper for the parent element, if any. The wrapper for this
         * element will be added to this wrapper as a child.
         */
        private RuntimeConfigurable parentWrapper;

        /**
         * Wrapper for this element which takes care of actually configuring
         * the element, if this element is contained within a target.
         * Otherwise the configuration is performed with the configure method.
         * @see ProjectHelper#configure(Object,AttributeList,Project)
         */
        private RuntimeConfigurable wrapper = null;

        /**
         * Constructor.
         *
         * @param parentHandler The handler which should be restored to the
         *                      parser at the end of the element.
         *                      Must not be <code>null</code>.
         *
         * @param cont          Container for the element.
         *                      May be <code>null</code> if the target is
         *                      <code>null</code> as well. If the
         *                      target is <code>null</code>, this parameter
         *                      is effectively ignored.
         *
         * @param parentwrapper Wrapper for the parent element, if any.
         *                      May be <code>null</code>. If the
         *                      target is <code>null</code>, this parameter
         *                      is effectively ignored.
         */
        public TaskHandler(DocumentHandler parentHandler,
                TaskContainer cont, RuntimeConfigurable parentwrapper) {
            super(parentHandler);
            this.container = cont;
            this.parentWrapper = parentwrapper;
        }

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. This configures
         * the element with its attributes and sets it up with
         * its parent container (if any). Nested elements are then
         * added later as the parser encounters them.
         *
         * @param tag Name of the element which caused this handler
         *            to be created. Must not be <code>null</code>.
         *
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         *
         * @exception SAXParseException in case of error (not thrown in
         *                              this implementation)
         */
        public void init(String tag, AttributeList attrs) throws SAXParseException {
//            try {
//                task = project.createTask(tag);
//            } catch (BuildException e) {
//                // swallow here, will be thrown again in
//                // UnknownElement.maybeConfigure if the problem persists.
//            }

            if (task == null) {
                UnknownElement ue = new UnknownElement(tag);
                ue.setProject(project);
                ue.setTaskName(tag);
                ue.setNamespace("");
                ue.setQName(tag);
                task = ue;
            }

            task.setLocation(
                new Location(locator.getSystemId(), locator.getLineNumber(),
                    locator.getColumnNumber()));
            configureId(task, attrs);

            // Top level tasks don't have associated targets
            if (target != null) {
                task.setOwningTarget(target);
                container.addTask(task);
                task.init();
                wrapper = task.getRuntimeConfigurableWrapper();
                wrapper.setAttributes(attrs);
                if (parentWrapper != null) {
                    parentWrapper.addChild(wrapper);
                }
            } else {
                task.init();
                configure(task, attrs, project);
            }
        }

        /**
         * Executes the task if it is a top-level one.
         */
        protected void finished() {
            if (task != null && target == null) {
                task.execute();
            }
        }

        /**
         * Adds text to the task, using the wrapper if one is
         * available (in other words if the task is within a target)
         * or using addText otherwise.
         *
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         *
         * @exception SAXParseException if the element doesn't support text
         *
         * @see ProjectHelper#addText(Project,Object,char[],int,int)
         */
        public void characters(char[] buf, int start, int count) throws SAXParseException {
            if (wrapper == null) {
                try {
                    ProjectHelper.addText(project, task, buf, start, count);
                } catch (BuildException exc) {
                    throw new SAXParseException(exc.getMessage(), locator, exc);
                }
            } else {
                wrapper.addText(buf, start, count);
            }
        }

        /**
         * Handles the start of an element within a target. Task containers
         * will always use another task handler, and all other tasks
         * will always use a nested element handler.
         *
         * @param name The name of the element being started.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         *
         * @exception SAXParseException if an error occurs when initialising
         *                              the appropriate child handler
         */
        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            if (task instanceof TaskContainer) {
                // task can contain other tasks - no other nested elements possible
                new TaskHandler(this, (TaskContainer)task, wrapper).init(name, attrs);
            } else {
                new NestedElementHandler(this, task, wrapper, target).init(name, attrs);
            }
        }
    }


    /**
     * Handler for all nested properties.
     */
    class NestedElementHandler extends AbstractHandler {

        /** Parent object (task/data type/etc). */
        private Object parent;

        /** The nested element itself. */
        private Object child;

        /**
         * Wrapper for the parent element, if any. The wrapper for this
         * element will be added to this wrapper as a child.
         */
        private RuntimeConfigurable parentWrapper;

        /**
         * Wrapper for this element which takes care of actually configuring
         * the element, if a parent wrapper is provided.
         * Otherwise the configuration is performed with the configure method.
         * @see ProjectHelper#configure(Object,AttributeList,Project)
         */
        private RuntimeConfigurable childWrapper = null;

        /** Target this element is part of, if any. */
        private Target target;

        /**
         * Constructor.
         *
         * @param parentHandler The handler which should be restored to the
         *                      parser at the end of the element.
         *                      Must not be <code>null</code>.
         *
         * @param p             Parent of this element (task/data type/etc).
         *                      Must not be <code>null</code>.
         *
         * @param parentwrapper Wrapper for the parent element, if any.
         *                      May be <code>null</code>.
         *
         * @param t             Target this element is part of.
         *                      May be <code>null</code>.
         */
        public NestedElementHandler(
                DocumentHandler parentHandler,
                Object p,
                RuntimeConfigurable parentwrapper,
                Target t) {
            super(parentHandler);

            if (p instanceof TaskAdapter) {
                this.parent = ((TaskAdapter)p).getProxy();
            } else {
                this.parent = p;
            }
            this.parentWrapper = parentwrapper;
            this.target = t;
        }

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. This configures
         * the element with its attributes and sets it up with
         * its parent container (if any). Nested elements are then
         * added later as the parser encounters them.
         *
         * @param propType Name of the element which caused this handler
         *            to be created. Must not be <code>null</code>.
         *
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         *
         * @exception SAXParseException in case of error, such as a
         *            BuildException being thrown during configuration.
         */
        public void init(String propType, AttributeList attrs) throws SAXParseException {
            Class parentClass = parent.getClass();
            IntrospectionHelper ih = IntrospectionHelper.getHelper(parentClass);

            try {
                String elementName = propType.toLowerCase(Locale.US);

                if (parent instanceof UnknownElement) {
                    UnknownElement uc = new UnknownElement(elementName);

                    uc.setProject(project);
                    ((UnknownElement)parent).addChild(uc);
                    uc.setNamespace(((UnknownElement)parent).getNamespace());
                    uc.setQName(elementName);
                    uc.setOwningTarget(((UnknownElement)parent).getOwningTarget());
                    uc.setTaskName(elementName);
                    child = uc;
                } else {
                    child = ih.createElement(project, parent, elementName);
                }

                configureId(child, attrs);

                if (parentWrapper != null) {
                    childWrapper = new RuntimeConfigurable(child, propType);
                    childWrapper.setAttributes(attrs);
                    parentWrapper.addChild(childWrapper);
                } else {
                    configure(child, attrs, project);
                    ih.storeElement(project, parent, child, elementName);
                }
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), locator, exc);
            }
        }

        /**
         * Adds text to the element, using the wrapper if one is
         * available or using addText otherwise.
         *
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         *
         * @exception SAXParseException if the element doesn't support text
         *
         * @see ProjectHelper#addText(Project,Object,char[],int,int)
         */
        public void characters(char[] buf, int start, int count) throws SAXParseException {
            if (parentWrapper == null) {
                try {
                    ProjectHelper.addText(project, child, buf, start, count);
                } catch (BuildException exc) {
                    throw new SAXParseException(exc.getMessage(), locator, exc);
                }
            } else {
                childWrapper.addText(buf, start, count);
            }
        }

        /**
         * Handles the start of an element within this one. Task containers
         * will always use a task handler, and all other elements
         * will always use another nested element handler.
         *
         * @param name The name of the element being started.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         *
         * @exception SAXParseException if an error occurs when initialising
         *                              the appropriate child handler
         */
        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            if (child instanceof TaskContainer) {
                // taskcontainer nested element can contain other tasks - no other
                // nested elements possible
                new TaskHandler(this, (TaskContainer)child, childWrapper).init(name, attrs);
            } else {
                new NestedElementHandler(this, child, childWrapper, target).init(name, attrs);
            }
        }
    }


    /**
     * Handler for all data types directly subordinate to project or target.
     */
    class DataTypeHandler extends AbstractHandler {

        /** Parent target, if any. */
        private Target target;

        /** The element being configured. */
        private Object element;

        /** Wrapper for this element, if it's part of a target. */
        private RuntimeConfigurable wrapper = null;

        /**
         * Constructor with no target specified.
         *
         * @param parentHandler The handler which should be restored to the
         *                      parser at the end of the element.
         *                      Must not be <code>null</code>.
         */
        public DataTypeHandler(DocumentHandler parentHandler) {
            this(parentHandler, null);
        }

        /**
         * Constructor with a target specified.
         *
         * @param parentHandler The handler which should be restored to the
         *                      parser at the end of the element.
         *                      Must not be <code>null</code>.
         *
         * @param t The parent target of this element.
         *          May be <code>null</code>.
         */
        public DataTypeHandler(DocumentHandler parentHandler, Target t) {
            super(parentHandler);
            this.target = t;
        }

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. This configures
         * the element with its attributes and sets it up with
         * its parent container (if any). Nested elements are then
         * added later as the parser encounters them.
         *
         * @param propType Name of the element which caused this handler
         *            to be created. Must not be <code>null</code>.
         *
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         *
         * @exception SAXParseException in case of error, such as a
         *            BuildException being thrown during configuration.
         */
        public void init(String propType, AttributeList attrs) throws SAXParseException {
            try {
                element = project.createDataType(propType);
                if (element == null) {
                    throw new BuildException("Unknown data type " + propType);
                }

                if (target != null) {
                    wrapper = new RuntimeConfigurable(element, propType);
                    wrapper.setAttributes(attrs);
                    target.addDataType(wrapper);
                } else {
                    configure(element, attrs, project);
                    configureId(element, attrs);
                }
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), locator, exc);
            }
        }

        // XXX: (Jon Skeet) Any reason why this doesn't use the wrapper
        // if one is available, whereas NestedElementHandler.characters does?
        /**
         * Adds text to the element.
         *
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         *
         * @exception SAXParseException if the element doesn't support text
         *
         * @see ProjectHelper#addText(Project,Object,char[],int,int)
         */
        public void characters(char[] buf, int start, int count) throws SAXParseException {
            try {
                ProjectHelper.addText(project, element, buf, start, count);
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), locator, exc);
            }
        }

        /**
         * Handles the start of an element within this one.
         * This will always use a nested element handler.
         *
         * @param name The name of the element being started.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         *
         * @exception SAXParseException if an error occurs when initialising
         *                              the child handler
         */
        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            new NestedElementHandler(this, element, wrapper, target).init(name, attrs);
        }
    }

    /**
     * Scans an attribute list for the <code>id</code> attribute and
     * stores a reference to the target object in the project if an
     * id is found.
     * <p>
     * This method was moved out of the configure method to allow
     * it to be executed at parse time.
     *
     * @see #configure(Object,AttributeList,Project)
     * @param t The target object
     * @param attr The attribute list.
     */
    private void configureId(Object t, AttributeList attr) {
        String id = attr.getValue("id");

        if (id != null) {
            project.addReference(id, t);
        }
    }
}
