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
    HttpClientRequest proxyReq = proxy req
    proxyReq >> { HttpClientResponse proxyResp ->
      resp.headers = proxyResp.headers - CONTENT_LENGTH // since response is chunked, do not send body-length
      proxyResp >>> resp.&end
      Pump fromRemote = proxyResp | resp
      fromRemote++ // server -> proxy -> client
    }
    req >>> proxyReq.&end
    Pump toRemote = req | proxyReq // client -> proxy -> server
    toRemote++
    proxyReq++
  }

  private HttpClientRequest proxy(HttpServerRequest req) {
    HttpClientRequest request = client.request(req.method, normalize(req))
    request.chunked = true
    request.headers = req.headers - CONTENT_LENGTH - HOST // since request is pumped, do not send body-length
    request.headers['X-Forwarded-For'] = req.remoteAddress
    request.headers['X-Forwarded-Host'] = req.headers[HOST]
    request
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
