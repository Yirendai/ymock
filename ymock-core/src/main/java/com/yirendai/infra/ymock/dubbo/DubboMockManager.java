package com.yirendai.infra.ymock.dubbo;

import java.util.HashSet;
import java.util.Set;

class DubboMockManager {

  /**
   * 是否开启dubbo,默认关闭.
   */
  private static volatile boolean mock = false;

  /**
   * 需要使用真实数据的接口集.
   */
  private static Set<String> setNeedRealService = new HashSet<String>();

  public static void setMock(boolean mk) {
    mock = mk;
  }

  public static void setMock(String interfaceName, boolean mock) {
    if (interfaceName == null) {
      return;
    }

    setNeedRealService.remove(interfaceName);
    if (!mock) {
      setNeedRealService.add(interfaceName);
    }
  }

  /**
   * true：表明开启dubboMock服务 false：表明关闭dubboMock服务.
   * 
   */
  public static boolean isMock() {
    return mock;
  }

  /**
   * 接口是否使用mock.
   */
  public static boolean isMock(String interfaceName) {
    if (interfaceName == null) {
      return true;
    }

    if (setNeedRealService.contains(interfaceName)) {
      return false;
    }

    return true;
  }
}
