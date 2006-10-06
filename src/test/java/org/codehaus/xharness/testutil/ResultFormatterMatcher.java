package org.codehaus.xharness.testutil;

import org.easymock.AbstractMatcher;

public class ResultFormatterMatcher extends AbstractMatcher {
    public boolean matches(Object[] expected, Object[] actual) {
        if (expected != null
            && actual != null
            && expected.length == 4
            && actual.length == 4) {
            Object[] ex1 = new Object[3];
            Object[] ac1 = new Object[3];
            System.arraycopy(expected, 0, ex1, 0, 3);
            System.arraycopy(actual, 0, ac1, 0, 3);
            return super.matches(ex1, ac1);
        }
        return super.matches(expected, actual);
    }
}
