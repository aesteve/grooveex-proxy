package com.github.aesteve.grooveex.examples.proxy

import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.groovy.core.http.HttpServer
import io.vertx.groovy.ext.unit.TestContext
import io.vertx.groovy.ext.web.RoutingContext
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner.class)
abstract class TestBase {

  private static final Integer REMOTE_PORT = '8890'
  private static final Integer PROXY_PORT = '8889'

  private HttpServer remoteServer


  abstract remoteServerContextHandler(RoutingContext ctx)


  @Before
  void setup(TestContext ctx) {
    setupRemoteServer()
  }

}
