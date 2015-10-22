package com.alvarosanchez.teams.vertx

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.sql.Sql
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.unit.Async
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.h2.jdbcx.JdbcConnectionPool
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import javax.sql.DataSource

@RunWith(VertxUnitRunner)
class TeamsVerticleTest {

    Vertx vertx

    DataSource dataSource
    
    Integer port

    @Before
    public void setUp(TestContext context) {
        Properties config = new Properties()
        config.load(this.getClass().classLoader.getResourceAsStream('teams.properties'))
        port = config.getProperty('server.port') as Integer
        def options = [ "config" : config as Map ] as DeploymentOptions
        vertx = Vertx.vertx()
        vertx.deployVerticle('groovy:'+TeamsVerticle.class.name, options, context.asyncAssertSuccess())

        dataSource = JdbcConnectionPool.create(config.getProperty('jdbc.url'), config.getProperty('jdbc.user'), config.getProperty('jdbc.password'))
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess())
    }

    @Test
    public void authenticatedApi(TestContext context) {
        final Async async = context.async()

        vertx.createHttpClient().getNow(port, "localhost", "/teams") { response ->
            context.assertTrue(response.statusCode() == 401)
            async.complete()
        }
    }

    @Test
    public void listTeams(TestContext context) {
        final Async async = context.async()

        create(new Team(name: 'Real Madrid CF'))
        create(new Team(name: 'FC Barcelona'))

        vertx.createHttpClient().get(port, "localhost", "/teams") { response ->
            response.handler { body ->
                context.assertTrue(new JsonSlurper().parseText(body.toString()).size() == 2)
                async.complete()
            }
        }.putHeader('Authorization', "Bearer ${token}").end()
    }

    @Test
    public void createTeam(TestContext context) {
        final Async async = context.async()
        String token = getToken()

        Team team = new Team(name: 'Real Madrid CF')
        String requestBody = JsonOutput.toJson(name: team.name)

        vertx.createHttpClient().post(port, "localhost", "/teams") { response ->
            response.handler { body ->
                assertSuccessfulResponse(context, body)

                vertx.createHttpClient().get(port, "localhost", "/teams/1") { listResponse ->
                    listResponse.handler { listBody ->
                        context.assertTrue(listBody.toString().equals(JsonOutput.toJson(new Team(id: 1, name: team.name))))
                        async.complete()
                    }
                }.putHeader('Authorization', "Bearer ${token}").end()


            }
        }.putHeader('Authorization', "Bearer ${token}").putHeader('Content-Type', 'application/json').end(requestBody)
    }

    @Test
    public void showTeam(TestContext context) {
        final Async async = context.async()

        Team team = new Team(name: 'Real Madrid CF')
        create(team)

        vertx.createHttpClient().get(port, "localhost", "/teams/1") { listResponse ->
            listResponse.handler { listBody ->
                context.assertTrue(listBody.toString().equals(JsonOutput.toJson(new Team(id: 1, name: team.name))))
                async.complete()
            }
        }.putHeader('Authorization', "Bearer ${token}").end()
    }

    @Test
    public void updateTeam(TestContext context) {
        final Async async = context.async()
        String token = getToken()

        Team team = new Team(name: 'Real Madrid CF')
        create(team)

        team.name += ', best club of the 20th century'

        vertx.createHttpClient().put(port, "localhost", "/teams/1") { response ->
            response.handler { body ->
                assertSuccessfulResponse(context, body)

                vertx.createHttpClient().get(port, "localhost", "/teams/1") { listResponse ->
                    listResponse.handler { listBody ->
                        context.assertTrue(listBody.toString().equals(JsonOutput.toJson(new Team(id: 1, name: team.name))))
                        async.complete()
                    }
                }.putHeader('Authorization', "Bearer ${token}").end()
            }
        }.putHeader('Authorization', "Bearer ${token}").putHeader('Content-Type', 'application/json').end(JsonOutput.toJson(name: team.name))
    }

    @Test
    public void deleteTeam(TestContext context) {
        final Async async = context.async()

        Team team = new Team(name: 'Real Madrid CF')
        create(team)

        vertx.createHttpClient().delete(port, "localhost", "/teams/1") { response ->
            response.handler { body ->
                assertSuccessfulResponse(context, body)
                async.complete()
            }
        }.putHeader('Authorization', "Bearer ${token}").end()
    }

    private void create(Team team) {
        Sql sql = new Sql(dataSource)
        sql.execute("INSERT INTO teams (name) VALUES (:name)", [name: team.name])
        sql.close()
    }

    private void assertSuccessfulResponse(TestContext context, Buffer body) {
        context.assertTrue(new JsonSlurper().parseText(body.toString()).success)
    }

    private String getToken() {
        new URL("http://localhost:${port}/login").text
    }
}
