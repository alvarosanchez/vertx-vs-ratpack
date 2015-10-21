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
        List<Team> teams = []
        executeQuery "SELECT * FROM teams", null, { row ->
            teams << new Team(id: row[0], name: row[1])
        }, {
            sendResponseBack(routingContext, teams)
        }
    }

    void getTeam(RoutingContext routingContext) {
        Long teamId = routingContext.request().getParam('teamId') as Long
        executeQuery("SELECT * FROM teams WHERE id = ?", [teamId]) { row ->
            sendResponseBack(routingContext, new Team(id: row[0], name: row[1]))
        }
    }

    void createTeam(RoutingContext routingContext) {
        Map<String, Object> body = routingContext.bodyAsJson
        executeUpdate(routingContext, "INSERT INTO teams (name) VALUES (?)", [body.name])
    }

    void updateTeam(RoutingContext routingContext) {
        Long teamId = routingContext.request().getParam('teamId') as Long
        String name = routingContext.bodyAsJson.name
        executeUpdate(routingContext, "UPDATE teams SET name = ? WHERE id = ?", [name, teamId])
    }

    void deleteTeam(RoutingContext routingContext) {
        Long teamId = routingContext.request().getParam('teamId') as Long
        executeUpdate(routingContext, "DELETE FROM teams WHERE id = ?", [teamId])
    }

    private void executeQuery(String sql, List parameters, Closure rowCallback, Closure endCallback = null) {
        client.getConnection() { connectionResult ->
            SQLConnection connection = connectionResult.result()
            Closure doWithRs = { rs ->
                rs.result().results.each { row ->
                    rowCallback.call(row)
                }
                endCallback?.call()
            }

            if (parameters) {
                connection.queryWithParams(sql, parameters, doWithRs)
            } else {
                connection.query(sql, doWithRs)
            }
            connection.close()
        }
    }


    private void executeUpdate(RoutingContext routingContext, String sql, List parameters) {
        client.getConnection() { connectionResult ->
            SQLConnection connection = connectionResult.result()
            connection.updateWithParams(sql, parameters) { result ->
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
