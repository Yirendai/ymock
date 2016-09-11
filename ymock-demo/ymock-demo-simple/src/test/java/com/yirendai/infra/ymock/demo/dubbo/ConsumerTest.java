package com.yirendai.infra.ymock.demo.dubbo;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yirendai.infra.ymock.dubbo.DubboMock;

import mockit.Mock;
import mockit.MockUp;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * 重点演示测试过程中，如果依赖第三方dubbo服务时，如何使用ymock负责测试.
 * 
 * @author zpc
 * </p>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring-test.xml"})
public class ConsumerTest {

  @Autowired
  private DemoService demoService;

  @Autowired

  @Test
  @Ignore
  public void testJmockit() throws Exception {
    // 基于jmockit设置期望
    MockUp<DemoService> mockService = new MockUp<DemoService>() {
      @Mock
      String sayHello(String name) {
        return "haha " + name;
      }
    };

    // 将期望注册到ymock中
    DubboMock.set(mockService);

    // 执行
    String result = demoService.sayHello("zpc");

    // 验收
    assertThat(result, equalTo("haha zpc"));
  }

  @Test
  public void testMockito() throws Exception {
    // 基于mokito设置期望
    DemoService mockFacade = mock(DemoService.class);
    when(mockFacade.sayHello("zpc")).thenReturn("goodman");

    // 将期望注册到ymock中
    DubboMock.set(mockFacade);

    // 执行
    String result = demoService.sayHello("zpc");

    // 验收
    assertThat(result, equalTo("goodman"));
  }
}
