package org.codehaus.xharness.exceptions;

import org.apache.tools.ant.BuildException;

public class FatalException extends BuildException {
    private static final long serialVersionUID = FatalException.class.getName().hashCode();

    public FatalException(String message) {
        super(message);
    }

}
