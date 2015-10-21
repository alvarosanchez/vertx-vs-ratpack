package com.alvarosanchez.teams.vertx

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import io.vertx.core.Future
import io.vertx.groovy.ext.jdbc.JDBCClient
import io.vertx.groovy.ext.sql.SQLConnection
import io.vertx.groovy.ext.web.Router
import io.vertx.groovy.ext.web.RoutingContext
import io.vertx.groovy.ext.web.handler.BodyHandler
import io.vertx.lang.groovy.GroovyVerticle

/**
 * REST API implementation
 */
@Slf4j
class TeamsVerticle extends GroovyVerticle {

    JDBCClient client

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

        //TODO this should be read from configuration file, but for the sake of the demo, it's enough to be here
        client = JDBCClient.createShared(vertx, [
            url: "jdbc:h2:mem:teams",
            user: "sa",
            password: "",
            driver_class: "org.h2.Driver",
            max_pool_size: 30
        ])

        vertx.createHttpServer().requestHandler(router.&accept).listen(8080) { listeningResult ->
            if (listeningResult.succeeded()) {
                client.getConnection() { connectionResult ->
                    connectionResult.result().execute('CREATE TABLE teams(id int auto_increment, name varchar(255))') { createResult ->
                        log.debug "Deployment completed"
                        startFuture.complete()
                    }
                }
            } else {
                startFuture.fail(listeningResult.cause())
            }
        }
    }

    void getTeams(RoutingContext routingContext) {
        client.getConnection() { connectionResult ->
            SQLConnection connection = connectionResult.result()
            connection.query("SELECT * FROM teams") { rs ->
                List<Team> teams = []

                rs.result().results.each { row ->
                    teams << new Team(id: row[0], name: row[1])
                }

                sendResponseBack(routingContext, teams)
            }
            connection.close()
        }
    }

    void getTeam(RoutingContext routingContext) {
        Long teamId = routingContext.request().getParam('teamId') as Long
        client.getConnection() { connectionResult ->
            SQLConnection connection = connectionResult.result()
            connection.queryWithParams("SELECT * FROM teams WHERE id = ?", [teamId]) { rs ->
                rs.result().results.each { row ->
                    sendResponseBack(routingContext, new Team(id: row[0], name: row[1]))
                }
            }
            connection.close()
        }
    }

    void createTeam(RoutingContext routingContext) {
        Map<String, Object> body = routingContext.bodyAsJson
        client.getConnection() { connectionResult ->
            SQLConnection connection = connectionResult.result()
            connection.updateWithParams("INSERT INTO teams (name) VALUES (?)", [body.name]) { result ->
                sendResponseBack(routingContext, [success: result.succeeded()])
            }
            connection.close()
        }
    }

    void updateTeam(RoutingContext routingContext) {
        Long teamId = routingContext.request().getParam('teamId') as Long
        String name = routingContext.bodyAsJson.name
        client.getConnection() { connectionResult ->
            SQLConnection connection = connectionResult.result()
            connection.updateWithParams("UPDATE teams SET name = ? WHERE id = ?", [name, teamId]) { result ->
                sendResponseBack(routingContext, [success: result.succeeded()])
            }
            connection.close()
        }
    }

    void deleteTeam(RoutingContext routingContext) {
        Long teamId = routingContext.request().getParam('teamId') as Long
        client.getConnection() { connectionResult ->
            SQLConnection connection = connectionResult.result()
            connection.updateWithParams("DELETE FROM teams WHERE id = ?", [teamId]) { result ->
                sendResponseBack(routingContext, [success: result.succeeded()])
            }
            connection.close()
        }
    }

    @Override
    void stop() throws Exception {
        client.getConnection() { connectionResult ->
            connectionResult.result().execute("DROP ALL OBJECTS DELETE FILES") {
                assert it.succeeded()
            }
        }
    }

    private void sendResponseBack(RoutingContext routingContext, Object response) {
        routingContext.response().end(JsonOutput.toJson(response))
    }


}
