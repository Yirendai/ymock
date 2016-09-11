package com.yirendai.infra.ymock.dubbo.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.alibaba.fastjson.JSON;

public class MockJS {

  private final ScriptEngineManager factory;
  private final ScriptEngine engine;

  private static class MockJSHolder {
    private static MockJS instance = new MockJS();
  }

  private MockJS() {
    factory = new ScriptEngineManager();
    engine = factory.getEngineByName("nashorn");

    try {
      if (engine == null) {
        throw new RuntimeException("need jdk1.8+");
      }

      @SuppressWarnings("static-access")
      Reader reader =
          new BufferedReader(new InputStreamReader(MockJS.class.getClassLoader().getSystemResourceAsStream("mock.js")));
      engine.eval(reader);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public static MockJS init() {
    return MockJSHolder.instance;
  }

  public Object eval(String expression) {
    Object obj = null;
    try {
      obj = engine.eval(expression);
    } catch (ScriptException ex) {
      ex.printStackTrace();
    }
    return obj;
  }

  public String evalMock(String expression) {
    String mocked = JSON.toJSONString(eval("Mock.mock(" + expression + ")"));
    return mocked;
  }

  public String evalMock(@SuppressWarnings("rawtypes") Map map) {
    String json = JSON.toJSONString(map);
    String mocked = (String) eval("JSON.stringify(Mock.mock(" + json + "))");
    return mocked;
  }

  public static String mock(String expression) {
    return init().evalMock(expression);
  }

  public static <K, V> String mock(Map<K, V> map) {
    String result = init().evalMock(map);

    return result;
  }
}
