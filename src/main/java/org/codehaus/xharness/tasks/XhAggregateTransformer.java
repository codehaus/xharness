package org.codehaus.xharness.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.optional.junit.AggregateTransformer;
import org.apache.tools.ant.types.Resource;
import org.codehaus.xharness.util.URLResource;

/**
 * Subclass of {@link org.apache.tools.ant.taskdefs.optional.junit.AggregateTransformer}
 * that loads the XHarness XSLT stylesheets instead of the junit ones.
 * 
 * This AggregateTransformer is compatible with both the Ant 1.6 and 1.7 base classes.
 * For each of the two Ant versions a method is overridded that returns the xharness styesheet 
 * instead of the junit one.
 *
 * @author  Gregor Heine
 */
class XhAggregateTransformer extends AggregateTransformer {
    public XhAggregateTransformer(Task task) {
        super(task);
    }

    public void setStyledir(File styledir) {
        task.log("This task doesn't support the styledir attribute. It is ignored.", 
                 Project.MSG_WARN);
    }

    /**
     * Ant 1.7.x override that returns the Xharness stylesheet location in an 
     * {@link org.codehaus.xharness.util.URLResource}.
     * 
     * @return  URLResource of the xharness xslt
     */
    protected Resource getStylesheet() {
        URLResource stylesheet = new URLResource();
        try {
            stylesheet.setURL(getStylesheetURL());
        } catch (IOException ioe) {
            stylesheet.setURL(null);
        }
        return stylesheet;
    }

    /**
     * Ant 1.6.x override that returns the Xharness stylesheet location as an URL String.
     * 
     * @throws  IOException if the stylesheet can't be found
     * @return  URL String of the xharness xslt
     */
    protected String getStylesheetSystemId() throws IOException {
        return getStylesheetURL().toExternalForm();
    }
    
    private URL getStylesheetURL() throws IOException {
        URL url = getClass().getResource("/org/codehaus/xharness/xsl/frames.xsl");

        if (url == null) {
            throw new FileNotFoundException(
                    "Could not find jar resource /org/codehaus/xharness/xsl/frames.xsl");
        }
        return url;
    }
}
