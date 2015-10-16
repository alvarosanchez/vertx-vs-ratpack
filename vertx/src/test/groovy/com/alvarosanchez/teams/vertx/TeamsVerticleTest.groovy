package com.alvarosanchez.teams.vertx

import io.vertx.core.Vertx
import io.vertx.ext.unit.Async
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(VertxUnitRunner)
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
    public void testMyApplication(TestContext context) {
        final Async async = context.async()

        vertx.createHttpClient().getNow(8080, "localhost", "/teams") { response ->
            response.handler { body ->
                context.assertTrue(body.toString().contains("/teams"))
                async.complete()
            }
        }
    }
}
