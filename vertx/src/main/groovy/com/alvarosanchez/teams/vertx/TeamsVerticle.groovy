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
        router.put("/teams/:teamId").handler(this.&updateTeam)
        router.delete("/teams/:teamId").handler(this.&deleteTeam)

        vertx.createHttpServer().requestHandler(router.&accept).listen(8080) { AsyncResult<HttpServer> listening ->
            if (listening.succeeded()) {
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
        log.debug "Received GET /teams request. Sending a message to the EB"
        vertx.eventBus().send("teams", [action: 'list']) { AsyncResult<Message> response ->
            sendResponseBack(routingContext, response)
        }
    }

    void getTeam(RoutingContext routingContext) {
        Long teamId = routingContext.request().getParam('teamId') as Long
        log.debug "Received GET /teams/${teamId} request. Sending a message to the EB"
        vertx.eventBus().send("teams", [action: 'show', teamId: teamId]) { AsyncResult<Message> response ->
            sendResponseBack(routingContext, response)
        }
    }

    void createTeam(RoutingContext routingContext) {
        log.debug "Received POST /teams request. Sending a message to the EB"

        Map<String, Object> body = routingContext.bodyAsJson
        log.debug "Request body: ${body}"

        vertx.eventBus().send("teams", [action: 'save', team: body]) { AsyncResult<Message> response ->
            sendResponseBack(routingContext, response)
        }
    }

    void updateTeam(RoutingContext routingContext) {
        Long teamId = routingContext.request().getParam('teamId') as Long
        String name = routingContext.bodyAsJson.name
        log.debug "Received PUT /teams/${teamId} request. Sending a message to the EB"
        vertx.eventBus().send("teams", [action: 'update', team: [id: teamId, name: name]]) { AsyncResult<Message> response ->
            sendResponseBack(routingContext, response)
        }
    }

    void deleteTeam(RoutingContext routingContext) {
        Long teamId = routingContext.request().getParam('teamId') as Long
        log.debug "Received DELETE /teams/${teamId} request. Sending a message to the EB"
        vertx.eventBus().send("teams", [action: 'delete', teamId: teamId]) { AsyncResult<Message> response ->
            sendResponseBack(routingContext, response)
        }
    }

    private void sendResponseBack(RoutingContext routingContext, AsyncResult<Message> response) {
        routingContext.response().end(response.result().body().toString())
    }


}
