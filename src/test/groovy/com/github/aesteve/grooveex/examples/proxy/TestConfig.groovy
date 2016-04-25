package com.github.aesteve.grooveex.examples.proxy

import io.vertx.groovy.ext.unit.junit.VertxUnitRunner
import io.vertx.groovy.core.Vertx
import io.vertx.groovy.ext.unit.TestContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner.class)
class TestConfig {

  private Vertx vertx

  @Before
  void startVertx() {
    vertx = Vertx.vertx
  }

  @After
  void closeVertx(TestContext ctx) {
    vertx.close ctx.asyncAssertSuccess()
  }

  @Test
  void testGoodConfig(TestContext ctx) {
    Map goodConf = [hostname: 'localhost', port: 8888, proxy: ['remote-host': 'vertx.io']]
    vertx.deployVerticle "groovy:${ProxyVerticle.class.name}", [config: goodConf], ctx.asyncAssertSuccess()
  }


}
