package com.alvarosanchez.teams.vertx

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.groovy.core.eventbus.Message
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

        vertx.createHttpServer().requestHandler(router.&accept).listen(8080) { AsyncResult<HttpServer> listening ->
            if (listening.succeeded()) {
                log.debug "Verticle deployed. Deploying another verticle"

                vertx.deployVerticle('groovy:com.alvarosanchez.teams.vertx.TeamsRepositoryVerticle') { AsyncResult<String> deployment ->
                    if(deployment.succeeded()) {
                        log.debug "Deployment completed"
                        startFuture.complete()
                    } else {
                        startFuture.fail(deployment.cause())
                    }
                }
            } else {
                startFuture.fail(listening.cause())
            }
        }
    }

    void getTeams(RoutingContext routingContext) {
        vertx.eventBus().send("teams.list", [:]) { AsyncResult<Message> response ->
            routingContext.response().end(response.result().body().toString())
        }
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
