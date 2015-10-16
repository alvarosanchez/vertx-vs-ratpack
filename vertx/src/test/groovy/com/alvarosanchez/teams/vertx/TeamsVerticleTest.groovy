package com.alvarosanchez.teams.vertx

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
}
