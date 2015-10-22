package com.alvarosanchez.teams.vertx

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.sql.ResultSet
import io.vertx.groovy.core.http.HttpServer
import io.vertx.groovy.ext.auth.jwt.JWTAuth
import io.vertx.groovy.ext.web.Router
import io.vertx.groovy.ext.web.RoutingContext
import io.vertx.groovy.ext.web.handler.BodyHandler
import io.vertx.groovy.ext.web.handler.JWTAuthHandler
import io.vertx.lang.groovy.GroovyVerticle
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.ext.jdbc.JDBCClient
import rx.Subscription
import rx.observers.Observers


/**
 * REST API implementation
 */
@Slf4j
class TeamsVerticle extends GroovyVerticle {

    Map<String, Object> config

    JDBCClient jdbc

    Vertx rxVertx

    JWTAuth authProvider

    @Override
    void start(Future<Void> startFuture) throws Exception {
        log.debug "Starting Verticle"
        rxVertx = Vertx.newInstance(vertx.delegate)

        config = vertx.getOrCreateContext().config()

        setupJdbcClient(rxVertx)
        setupAuthProvider()

        Router router = setupRoutes()
        setupHttpServer(router, startFuture, jdbc)
    }

    private HttpServer setupHttpServer(Router router, startFuture, jdbc) {
        return vertx.createHttpServer().requestHandler(router.&accept).listen(config.get('server.port') as Integer) { listeningResult ->
            if (listeningResult.succeeded()) {
                jdbc.connectionObservable.subscribe { connection ->
                    connection.executeObservable('CREATE TABLE teams(id int auto_increment, name varchar(255))').subscribe {
                        startFuture.complete()
                    }
                }
            } else {
                startFuture.fail(listeningResult.cause())
            }
        }
    }

    private void setupAuthProvider() {
        authProvider = JWTAuth.create(vertx, [
            keyStore: [
                path    : config.get('keyStore.path'),
                type    : config.get('keyStore.type'),
                password: config.get('keyStore.password')
            ]
        ])
    }

    private Router setupRoutes() {
        Router router = Router.router(vertx)

        router.route().handler(BodyHandler.create())

        router.route("/teams*").handler(JWTAuthHandler.create(authProvider))
        router.route("/login").handler { RoutingContext ctx ->
            ctx.response().end(authProvider.generateToken([sub: 'alvaro.sanchez'], [expiresInSeconds: 60]));
        }

        router.get("/teams").handler(this.getTeams())
        router.get("/teams/:teamId").handler(this.getTeam())
        router.post("/teams").handler(this.createTeam())
        router.put("/teams/:teamId").handler(this.updateTeam())
        router.delete("/teams/:teamId").handler(this.deleteTeam())
        return router
    }

    private JDBCClient setupJdbcClient(Vertx rxVertx) {
        jdbc = JDBCClient.createShared(rxVertx, [
                url          : config.get('jdbc.url'),
                user         : config.get('jdbc.user'),
                password     : config.get('jdbc.password'),
                driver_class : config.get('jdbc.driver_class')
        ] as JsonObject)
        return jdbc
    }

    Handler getTeams() {
        Observers.create { RoutingContext routingContext ->
            List<Team> teams = []
            executeQuery "SELECT * FROM teams", null, { row ->
                teams << new Team(id: row[0], name: row[1])
            }, {
                sendResponseBack(routingContext, teams)
            }
        }.toHandler()
    }

    Handler getTeam() {
        Observers.create { RoutingContext routingContext ->
            Long teamId = routingContext.request().getParam('teamId') as Long
            executeQuery("SELECT * FROM teams WHERE id = ?", [teamId]) { row ->
                sendResponseBack(routingContext, new Team(id: row[0], name: row[1]))
            }
        }.toHandler()
    }

    Handler createTeam() {
        Observers.create { RoutingContext routingContext ->
            Map<String, Object> body = routingContext.bodyAsJson
            executeUpdate(routingContext, "INSERT INTO teams (name) VALUES (?)", [body.name])
        }.toHandler()
    }

    Handler updateTeam() {
        Observers.create { RoutingContext routingContext ->
            Long teamId = routingContext.request().getParam('teamId') as Long
            String name = routingContext.bodyAsJson.name
            executeUpdate(routingContext, "UPDATE teams SET name = ? WHERE id = ?", [name, teamId])
        }.toHandler()
    }

    Handler deleteTeam() {
        Observers.create { RoutingContext routingContext ->
            Long teamId = routingContext.request().getParam('teamId') as Long
            executeUpdate(routingContext, "DELETE FROM teams WHERE id = ?", [teamId])
        }.toHandler()
    }

    private Subscription executeQuery(String sql, List parameters, Closure rowCallback, Closure endCallback = null) {
        jdbc.connectionObservable.subscribe { connection ->
            Closure doWithResultSet = { ResultSet rs ->
                rs.results.each { row ->
                    rowCallback.call(row)
                }
                endCallback?.call()
            }

            if (parameters) {
                connection.queryWithParamsObservable(sql, new JsonArray(parameters)).subscribe(doWithResultSet)
            } else {
                connection.queryObservable(sql).subscribe(doWithResultSet)
            }
            connection.close()
        }
    }


    private Subscription executeUpdate(RoutingContext routingContext, String sql, List parameters) {
        jdbc.connectionObservable.subscribe { connection ->
            connection.updateWithParamsObservable(sql, new JsonArray(parameters)).subscribe { result ->
                sendResponseBack(routingContext, [success: result.updated as boolean])
            }
            connection.close()
        }
    }

    @Override
    void stop() throws Exception {
        jdbc.connectionObservable.subscribe { connection ->
            connection.executeObservable("DROP ALL OBJECTS DELETE FILES")
        }
    }

    private void sendResponseBack(RoutingContext routingContext, Object response) {
        routingContext.response().end(JsonOutput.toJson(response))
    }


}
