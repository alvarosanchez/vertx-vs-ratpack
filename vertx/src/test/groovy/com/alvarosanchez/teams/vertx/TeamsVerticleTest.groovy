package com.alvarosanchez.teams.vertx

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.sql.Sql
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

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx()
        vertx.deployVerticle('groovy:com.alvarosanchez.teams.vertx.TeamsVerticle', context.asyncAssertSuccess())

        dataSource = JdbcConnectionPool.create("jdbc:h2:mem:teams", "sa", "")
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess())
    }

    @Test
    public void listTeams(TestContext context) {
        final Async async = context.async()

        create(new Team(name: 'Real Madrid CF'))
        create(new Team(name: 'FC Barcelona'))

        vertx.createHttpClient().getNow(8080, "localhost", "/teams") { response ->
            response.handler { body ->
                context.assertTrue(new JsonSlurper().parseText(body.toString()).size() == 2)
                async.complete()
            }
        }
    }

    @Test
    public void createTeam(TestContext context) {
        final Async async = context.async()

        Team team = new Team(name: 'Real Madrid CF')
        String requestBody = JsonOutput.toJson(name: team.name)

        vertx.createHttpClient().post(8080, "localhost", "/teams") { response ->
            response.handler { body ->
                assertSuccessfulResponse(context, body)

                vertx.createHttpClient().getNow(8080, "localhost", "/teams/1") { listResponse ->
                    listResponse.handler { listBody ->
                        context.assertTrue(listBody.toString().equals(JsonOutput.toJson(new Team(id: 1, name: team.name))))
                        async.complete()
                    }
                }


            }
        }.putHeader('Content-Type', 'application/json').end(requestBody)
    }

    @Test
    public void showTeam(TestContext context) {
        final Async async = context.async()

        Team team = new Team(name: 'Real Madrid CF')
        create(team)

        vertx.createHttpClient().getNow(8080, "localhost", "/teams/1") { listResponse ->
            listResponse.handler { listBody ->
                context.assertTrue(listBody.toString().equals(JsonOutput.toJson(new Team(id: 1, name: team.name))))
                async.complete()
            }
        }
    }

    @Test
    public void updateTeam(TestContext context) {
        final Async async = context.async()

        Team team = new Team(name: 'Real Madrid CF')
        create(team)

        team.name += ', best club of the 20th century'

        vertx.createHttpClient().put(8080, "localhost", "/teams/1") { response ->
            response.handler { body ->
                assertSuccessfulResponse(context, body)

                vertx.createHttpClient().getNow(8080, "localhost", "/teams/1") { listResponse ->
                    listResponse.handler { listBody ->
                        context.assertTrue(listBody.toString().equals(JsonOutput.toJson(new Team(id: 1, name: team.name))))
                        async.complete()
                    }
                }
            }
        }.putHeader('Content-Type', 'application/json').end(JsonOutput.toJson(name: team.name))
    }

    @Test
    public void deleteTeam(TestContext context) {
        final Async async = context.async()

        Team team = new Team(name: 'Real Madrid CF')
        create(team)

        vertx.createHttpClient().delete(8080, "localhost", "/teams/1") { response ->
            response.handler { body ->
                assertSuccessfulResponse(context, body)
                async.complete()
            }
        }.end()
    }

    private void create(Team team) {
        Sql sql = new Sql(dataSource)
        sql.execute("INSERT INTO teams (name) VALUES (:name)", [name: team.name])
        sql.close()
    }

    private void assertSuccessfulResponse(TestContext context, Buffer body) {
        context.assertTrue(new JsonSlurper().parseText(body.toString()).success)
    }
}
