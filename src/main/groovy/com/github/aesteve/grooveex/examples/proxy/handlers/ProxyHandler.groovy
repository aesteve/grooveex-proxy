package com.github.aesteve.grooveex.examples.proxy.handlers

import groovy.transform.CompileStatic
import io.vertx.groovy.core.Vertx
import io.vertx.groovy.core.http.*
import io.vertx.groovy.core.streams.Pump
import io.vertx.groovy.ext.web.RoutingContext
import org.codehaus.groovy.control.ConfigurationException

import static io.vertx.core.http.HttpHeaders.CONTENT_LENGTH
import static io.vertx.core.http.HttpHeaders.HOST

@CompileStatic
class ProxyHandler {

  private Map conf
  private Vertx vertx
  private String remoteHost
  private int remotePort = 80
  private final String mountPoint

  public ProxyHandler(Vertx vertx, Map conf, String mountPoint) throws ConfigurationException {
    this.vertx = vertx
    this.conf = conf
    checkConfig()
    this.mountPoint = mountPoint
  }

  public void forward(RoutingContext context) {
    HttpServerRequest req = context.request
    HttpServerResponse resp = context.response
    resp.chunked = true
    HttpClientRequest proxyReq = client.request req.method, normalize(req)
    proxyReq.chunked = true
    proxyReq.headers = req.headers - CONTENT_LENGTH - HOST // since request is pumped, do not send body-length
    proxyReq.headers['X-Forwarded-For'] = req.remoteAddress()
    proxyReq.headers['X-Forwarded-Host'] = req.headers[HOST]
    proxyReq >> { HttpClientResponse remoteResp ->
      resp.headers = remoteResp.headers - CONTENT_LENGTH // since response is chunked, do not send body-length
      Pump p = remoteResp | resp
      p++ // server -> proxy -> client
      remoteResp >>> resp.&end
    }
    Pump toRemote = req | proxyReq // client -> proxy -> server
    toRemote.start()
    req >>> proxyReq.&end
  }

  private String normalize(HttpServerRequest req) {
    if (mountPoint == '/') return req.path
    return req - mountPoint
  }

  private HttpClient getClient() {
    Map<String, Object> options = [host:remoteHost, port:remotePort] as Map<String, Object>
    vertx.createHttpClient options
  }

  void checkConfig() throws ConfigurationException {
    remoteHost = conf['remote-host']
    if (!remoteHost) throw new ConfigurationException('You should specify a remote hostname in json config : proxy:{"remote-host":"foo.com"}')
    remotePort = conf['remote-port'] as Integer ?: remotePort
  }

}
