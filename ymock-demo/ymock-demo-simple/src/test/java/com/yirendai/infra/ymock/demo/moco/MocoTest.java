package com.yirendai.infra.ymock.demo.moco;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import com.github.dreamhead.moco.HttpServer;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import com.github.dreamhead.moco.Runnable;

import static com.github.dreamhead.moco.Moco.httpServer;
import static com.github.dreamhead.moco.Runner.running;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * 测试过程如果依赖第三方Http服务，可以采用moco框架.
 * 一下只是使用简单举例。
 * <p>
 * @author zpc
 * </p>
 */
public class MocoTest {

  @BeforeClass
  public static void setUp() {
    HttpServer server = httpServer(12306);
    server.response("foo");
  }

  @Test
  public void test() throws Exception {
    HttpServer server = httpServer(12306);
    server.response("foo");

    running(server, new Runnable() {
      public void run() throws IOException {
        Content content = Request.Get("http://localhost:12306").execute().returnContent();
        assertThat(content.asString(), is("foo"));
      }
    });
  }

  @Test
  @Ignore
  public void test2() throws Exception {
    Content content = Request.Get("http://localhost:12306").execute().returnContent();
    assertThat(content.asString(), is("foo"));
  }
}
