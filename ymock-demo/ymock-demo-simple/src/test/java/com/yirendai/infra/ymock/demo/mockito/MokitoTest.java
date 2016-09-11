package com.yirendai.infra.ymock.demo.mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

/**
 * mockito是stackoverflow评选出来的最好的mock框架.
 * 以下只是使用举例，详情可以参考官方文档。
 * 另外jmockit也是一款不错的mock框架，功能最全面，mockit不能支持的情况，可以考虑jmockit. 
 *
 * @author zpc
 * </p>
 */
public class MokitoTest {

  @Test
  @Ignore
  public void testMokito() throws Exception {
    // you can mock concrete classes, not only interfaces
    @SuppressWarnings("rawtypes")
    List mockedList = mock(LinkedList.class);

    // stubbing appears before the actual execution
    when(mockedList.get(0)).thenReturn("first");

    Class<?> realClz = mockedList.getClass().getSuperclass();
    String interfaceName = realClz.getCanonicalName();

    System.err.println("interface:" + interfaceName);

    Method[] methods = realClz.getDeclaredMethods();

    Method meth = null;
    for (Method method : methods) {
      String methodName = method.getName();
      Class<?>[] paraTypes = method.getParameterTypes();

      System.err
          .println(methodName + "," + method.isAccessible() + "," + method.isBridge() + "," + method.isSynthetic());

      if ("get".equals(methodName)) {
        System.err.println("method:" + method.getName());

        meth = method;

        for (Class<?> clz : paraTypes) {
          System.err.println("paraType:" + clz.getName());
        }
      }
    }

    System.err.println("goodTry:" + meth.invoke(mockedList, 0));

    // the following prints "first"

    // the following prints "null" because get(999) was not stubbed
    System.out.println(mockedList.get(999));

    verify(mockedList).get(0);
    verify(mockedList).get(999);
  }

  @Test
  public void testMockito() {
    // you can mock concrete classes, not only interfaces
    @SuppressWarnings("rawtypes")
    LinkedList mockedList = mock(LinkedList.class);

    // stubbing appears before the actual execution
    when(mockedList.get(0)).thenReturn("first");

    // the following prints "first"
    System.out.println(mockedList.get(0));

    // the following prints "null" because get(999) was not stubbed
    System.out.println(mockedList.get(999));
  }

  @Test
  public void testMockito2() {
    @SuppressWarnings("unchecked")
    List<String> mockedList = mock(List.class);

    // using mock object - it does not throw any "unexpected interaction"
    // exception
    mockedList.add("one");
    mockedList.clear();

    // selective, explicit, highly readable verification
    verify(mockedList).add("one");
    verify(mockedList).clear();
  }
}
