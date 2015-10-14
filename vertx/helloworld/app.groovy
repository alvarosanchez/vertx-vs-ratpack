@Grab('io.vertx:vertx-lang-groovy:3.1.0')
@GrabExclude('org.codehaus.groovy:groovy-all')
import io.vertx.groovy.core.Vertx

Vertx.vertx().createHttpServer().requestHandler { request ->
  request.response().end("Hello world")
}.listen(8080)
