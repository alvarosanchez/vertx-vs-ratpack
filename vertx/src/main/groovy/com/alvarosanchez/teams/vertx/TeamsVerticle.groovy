package com.alvarosanchez.teams.vertx

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.groovy.core.http.HttpServer
import io.vertx.groovy.ext.web.Router
import io.vertx.groovy.ext.web.RoutingContext
import io.vertx.groovy.ext.web.handler.BodyHandler
import io.vertx.lang.groovy.GroovyVerticle

@CompileStatic
@Slf4j
class TeamsVerticle extends GroovyVerticle {

    @Override
    void start(Future<Void> startFuture) throws Exception {
        log.debug "Starting Verticle"
        def router = Router.router(vertx)

        router.route().handler(BodyHandler.create())

        router.get("/teams").handler(this.&getTeams)
        router.get("/teams/:teamId").handler(this.&getTeam)
        router.post("/teams").handler(this.&createTeam)
        router.put("/teams").handler(this.&updateTeam)
        router.delete("/teams/:teamId").handler(this.&deleteTeam)

        vertx.createHttpServer().requestHandler(router.&accept).listen(8080) { AsyncResult<HttpServer> result ->
            if (result.succeeded()) {
                log.debug "Verticle deployed"
                startFuture.complete()
            } else {
                startFuture.fail(result.cause())
            }
        }
    }

    void getTeams(RoutingContext routingContext) {
        routingContext.response().end(routingContext.normalisedPath())
    }

    void getTeam(RoutingContext routingContext) {
        routingContext.response().end(routingContext.normalisedPath())
    }

    void createTeam(RoutingContext routingContext) {
        routingContext.response().end(routingContext.normalisedPath())
    }

    void updateTeam(RoutingContext routingContext) {
        routingContext.response().end(routingContext.normalisedPath())
    }

    void deleteTeam(RoutingContext routingContext) {
        routingContext.response().end(routingContext.normalisedPath())
    }

}
