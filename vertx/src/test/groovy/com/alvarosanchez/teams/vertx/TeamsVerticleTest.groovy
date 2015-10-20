package com.alvarosanchez.teams.vertx

import com.alvarosanchez.teams.core.Team
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import io.vertx.core.Vertx
import io.vertx.ext.unit.Async
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(VertxUnitRunner)
@Slf4j
class TeamsVerticleTest {

    Vertx vertx

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx()
        vertx.deployVerticle('groovy:com.alvarosanchez.teams.vertx.TeamsVerticle', context.asyncAssertSuccess())
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess())
    }

    @Test
    public void listTeams(TestContext context) {
        final Async async = context.async()

        vertx.createHttpClient().getNow(8080, "localhost", "/teams") { response ->
            response.handler { body ->
                log.debug "body -> ${body.toString()}"
                context.assertTrue(body.toString().equals("[]"))
                async.complete()
            }
        }
    }

    @Test
    public void createTeam(TestContext context) {
        final Async async = context.async()

        Team team = new Team(name: 'Real Madrid CF')

        create(team, context)

        vertx.createHttpClient().getNow(8080, "localhost", "/teams") { listResponse ->
            listResponse.handler { listBody ->
                log.debug "listBody -> ${listBody.toString()}"
                log.debug JsonOutput.toJson([new Team(id: 1, name: "Real Madrid CF")])
                context.assertTrue(listBody.toString().equals(JsonOutput.toJson([new Team(id: 1, name: "Real Madrid CF")])))
                async.complete()
            }
        }
    }

    private void create(Team team, TestContext context) {
        String requestBody = JsonOutput.toJson(team)

        vertx.createHttpClient().post(8080, "localhost", "/teams") { response ->
            response.handler { body ->
                log.debug "body -> ${body.toString()}"
                context.assertTrue(new JsonSlurper().parseText(body.toString()).success)
            }
        }.putHeader('Content-Type', 'application/json').end(requestBody)

    }
}
