package com.yirendai.infra.ymock.demo.moco;

import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.dreamhead.moco.HttpServer;
import com.github.dreamhead.moco.Runner;

import java.io.IOException;

import static com.github.dreamhead.moco.Moco.and;
import static com.github.dreamhead.moco.Moco.by;
import static com.github.dreamhead.moco.Moco.eq;
import static com.github.dreamhead.moco.Moco.httpServer;
import static com.github.dreamhead.moco.Moco.query;
import static com.github.dreamhead.moco.Moco.uri;
import static com.github.dreamhead.moco.Runner.runner;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * moco使用学习，详情可以参考官方文档.
 * 
 * @author zpc
 * </p>
 */
public class MocoRunnerTest {
  private Runner runner;

  @Before
  public void setup() {
    HttpServer server = httpServer(12306);
    server.get(by(uri("/foo"))).response("rfoo");
    server.request(and(by(uri("/foo")), eq(query("param"), "blah"))).response("bar");
    server.response("other");
    runner = runner(server);
    runner.start();
  }

  @After
  public void tearDown() {
    runner.stop();
  }

  @Test
  public void should_response_as_expected() throws IOException {
    Content content = Request.Get("http://localhost:12306").execute().returnContent();
    assertThat(content.asString(), is("other"));
    Content content2 = Request.Get("http://localhost:12306/foo").execute().returnContent();
    assertThat(content2.asString(), is("rfoo"));
  }
}
