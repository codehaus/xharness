package test.junit;

import junit.framework.TestCase;

public class MyTest extends TestCase {
    public void testPassing() {
        System.out.println("test.junit.MyTest:testPassing() started");
        System.err.println("Nothing to report on System.err");
        assertTrue("This test is meant to pass", true);
        System.out.println("test.junit.MyTest:testPassing() passed");
    }
    
    public void testGood() {
        System.out.println("test.junit.MyTest:testGood() started");
        System.err.println("Still nothing to report on System.err");
        assertTrue("This test is meant to pass", true);
        System.out.println("test.junit.MyTest:testGood() passed");
    }
    
//  public void testFailing() {
//      fail("This test is meant to fail");
//  }
//
//  public void testFailing2() {
//      assertTrue("This test is meant to fail", false);
//  }
}
