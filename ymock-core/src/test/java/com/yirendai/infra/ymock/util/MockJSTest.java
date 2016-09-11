package com.yirendai.infra.ymock.util;

import com.yirendai.infra.ymock.dubbo.util.MockJS;

public class MockJSTest {

  public static void main(String[] args) {
    System.err.println(MockJS.mock("{\"string|1-10\":\"*\"}"));

    String exp = " {\"string|2-9\":\"*\"}";
    System.err.println(MockJS.mock(exp));
  }
}
