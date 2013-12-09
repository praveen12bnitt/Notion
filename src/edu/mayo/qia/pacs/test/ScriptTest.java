package edu.mayo.qia.pacs.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class ScriptTest extends PACSTest {

  @Test
  public void simplePrint() throws Exception {
    ScriptEngineManager manager = new ScriptEngineManager();
    ScriptEngine engine = manager.getEngineByName("JavaScript");
    Bindings bindings = engine.createBindings();
    bindings.put("num", "20");
    Object result = null;
    // result =
    // engine.eval("print ( fib(num) ); function fib(n) {  if(n <= 1) return n;  return fib(n-1) + fib(n-2); };",
    // bindings);
    result = engine.eval("function foo () { return 'hi' }; foo();", bindings);
    if (result instanceof String) {
      logger.info("\n======\n" + result + "\n=======");
      assertEquals("Result from javascript", "hi", result.toString());
    } else {
      fail("Did not get a string back");
    }

    // Last statement ends up being the result value from an eval call
    result = engine.eval("function foo () { return 'hi' }; 'hi';", bindings);
    if (result instanceof String) {
      logger.info("\n======\n" + result + "\n=======");
      assertEquals("Result from javascript", "hi", result.toString());
    } else {
      fail("Did not get a string back");
    }

  }
}
