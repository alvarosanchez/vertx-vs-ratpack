package com.alvarosanchez.teams.vertx

import com.alvarosanchez.teams.core.Team
import com.alvarosanchez.teams.core.TeamRepository
import com.alvarosanchez.teams.core.TeamRepositoryJdbcImpl
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import io.vertx.core.Future
import io.vertx.groovy.core.eventbus.EventBus
import io.vertx.groovy.core.eventbus.Message
import io.vertx.lang.groovy.GroovyVerticle
import org.h2.jdbcx.JdbcConnectionPool

@Slf4j
class TeamsRepositoryVerticle extends GroovyVerticle {

    TeamRepository repository

    @Override
    void start(Future<Void> startFuture) throws Exception {
        log.debug "Initialising DB"
        repository = new TeamRepositoryJdbcImpl(JdbcConnectionPool.create("jdbc:h2:mem:teams", "sa", ""))
        repository.init()

        EventBus eb = vertx.eventBus()
        log.debug "Registering event bus handlers"

        eb.consumer("teams.list").handler { Message message ->
            log.debug "Received list request"
            List<Team> teams = repository.list()
            message.reply(JsonOutput.toJson(teams))
        }

        startFuture.complete()
    }
}
