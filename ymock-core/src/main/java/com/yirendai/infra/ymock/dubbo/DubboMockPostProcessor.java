package com.yirendai.infra.ymock.dubbo;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.alibaba.dubbo.config.ReferenceConfig;

public class DubboMockPostProcessor implements BeanFactoryPostProcessor {

  public DubboMockPostProcessor(boolean mock) {
    if (mock) {
      DubboMockManager.setMock(true);
    }
  }

  @SuppressWarnings("deprecation")
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    if (DubboMockManager.isMock()) {
      String[] names = beanFactory.getBeanNamesForType(ReferenceConfig.class);
      for (String name : names) {
        @SuppressWarnings("rawtypes")
        ReferenceConfig config = beanFactory.getBean(name, ReferenceConfig.class);
        config.setInjvm(true);
        config.setCheck(false);
      }
    }
  }
}
