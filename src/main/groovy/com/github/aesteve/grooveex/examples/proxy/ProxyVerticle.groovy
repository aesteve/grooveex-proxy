package com.github.aesteve.grooveex.examples.proxy

import com.github.aesteve.grooveex.examples.proxy.handlers.ProxyHandler
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import io.vertx.core.Future
import io.vertx.groovy.core.http.HttpServer
import io.vertx.groovy.ext.web.Router
import io.vertx.lang.groovy.GroovyVerticle
import org.codehaus.groovy.control.ConfigurationException

@CompileStatic
class ProxyVerticle extends GroovyVerticle {

  private HttpServer server
  private Map clientConf
  private Map conf
  private ProxyHandler proxy
  private String forwardPath

  @Override
  public void start(Future<Void> future) throws ConfigurationException {
    checkConfig()
    forwardPath = context.config['forward-path'] ?: '/'
    server = vertx.createHttpServer context.config
    proxy = new ProxyHandler(vertx, clientConf, forwardPath)
    Router router = vertx.router
    router.route(forwardPath) >> proxy.&forward
    server.requestHandler router.&accept
    server.listen {
      future.complete()
    }
  }

  void checkConfig() throws ConfigurationException {
    conf = context.config
    clientConf = conf['proxy'] as Map
    if (!clientConf) throw new ConfigurationException("Config should contain a 'proxy' node")
  }
}
