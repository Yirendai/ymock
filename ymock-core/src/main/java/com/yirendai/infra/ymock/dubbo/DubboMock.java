package com.yirendai.infra.ymock.dubbo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mockit.MockUp;

public class DubboMock {

  private static Map<String, DubboMockEntity> mockServiceMap = new ConcurrentHashMap<String, DubboMockEntity>();

  /**
   * true：表明开启dubboMock服务 false：表明关闭dubboMock服务.
   * 
   */
  public static boolean isMock() {
    return DubboMockManager.isMock();
  }

  /**
   * 接口是否使用mock.
   */
  public static boolean isMock(String interfaceName) {
    return DubboMockManager.isMock(interfaceName);
  }

  public static void setMock(boolean mk) {
    DubboMockManager.setMock(mk);
  }

  public static void setMock(String interfaceName, boolean mock) {
    DubboMockManager.setMock(interfaceName, mock);
  }

  public static void clear() {
    mockServiceMap.clear();
  }

  public static <T> void set(MockUp<T> mockup) throws Exception {
    @SuppressWarnings("rawtypes")
    Class mockUpClz = mockup.getClass();

    Type mockType = mockUpClz.getGenericSuperclass();
    ParameterizedType mockPT = (ParameterizedType) mockType;
    Class<?> actualType = (Class<?>) mockPT.getActualTypeArguments()[0];
    String interfaceName = actualType.getCanonicalName();

    DubboMockEntity mockEntity = new DubboMockEntity();
    mockEntity.setInterfaceName(interfaceName);

    T service = mockup.getMockInstance();
    mockEntity.setServiceObj(service);

    Method[] methods = mockUpClz.getDeclaredMethods();
    for (Method method : methods) {
      Class<?>[] paraTypes = method.getParameterTypes();
      String key = generateKey(method.getName(), paraTypes);

      String methodName = method.getName();
      Method realMethod = service.getClass().getDeclaredMethod(methodName, paraTypes);
      mockEntity.put(key, realMethod);
    }

    mockServiceMap.put(interfaceName, mockEntity);
  }
  
  public static <T> void set(T mockObject) throws Exception {
    if (mockObject == null) {
      return;
    }

    DubboMockEntity mockEntity = new DubboMockEntity();
    mockEntity.setServiceObj(mockObject);

    Class<?> realClz = mockObject.getClass();
    String interfaceName = realClz.getCanonicalName();
    int index = interfaceName.indexOf('$');
    if (index != -1) {
      interfaceName = interfaceName.substring(0, index);
    }

    mockEntity.setInterfaceName(interfaceName);

    Method[] methods = realClz.getDeclaredMethods();
    for (Method method : methods) {
      String methodName = method.getName();
      if (methodName.indexOf('$') > -1) {
        continue;
      }

      Class<?>[] paraTypes = method.getParameterTypes();
      String key = generateKey(method.getName(), paraTypes);

      mockEntity.put(key, method);
    }

    mockServiceMap.put(interfaceName, mockEntity);
  }


  public static Object invoke(String interfaceName, String methodName, Class<?>[] paraTypes, Object[] paraObjects)
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    DubboMockEntity mockEntity = mockServiceMap.get(interfaceName);
    if (mockEntity == null) {
      throw new NoSuchMethodError("no define " + interfaceName + "," + methodName + " in DubboMock!");
    }

    return mockEntity.invoke(methodName, paraTypes, paraObjects);
  }

  private static String generateKey(String methodName, Class<?>[] paraTypes) {
    StringBuilder sb = new StringBuilder();
    for (Class<?> pclz : paraTypes) {
      sb.append("_").append(pclz.getCanonicalName());
    }

    return new StringBuilder().append(methodName).append(sb).toString();
  }

  static class DubboMockEntity {

    private String interfaceName;

    private Object serviceObj;

    private Map<String, Method> methodMap = new ConcurrentHashMap<String, Method>();

    public String getInterfaceName() {
      return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
      this.interfaceName = interfaceName;
    }

    public Object getServiceObj() {
      return serviceObj;
    }

    public void setServiceObj(Object serviceObj) {
      this.serviceObj = serviceObj;
    }

    public Map<String, Method> getMethodMap() {
      return methodMap;
    }

    public void setMethodMap(Map<String, Method> methodMap) {
      this.methodMap = methodMap;
    }

    public void put(String methodName, Method method) {
      this.methodMap.put(methodName, method);
    }

    public Method getMethod(String methodName) {
      return this.methodMap.get(methodName);
    }

    public Object invoke(String methodName, Class<?>[] paraTypes, Object[] paraObjects)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      String key = generateKey(methodName, paraTypes);

      Method method = this.methodMap.get(key);
      if (method != null) {
        return method.invoke(serviceObj, paraObjects);
      } else {
        throw new NoSuchMethodError("no define " + interfaceName + "," + methodName + " in DubboMock!");
      }
    }
  }

}
