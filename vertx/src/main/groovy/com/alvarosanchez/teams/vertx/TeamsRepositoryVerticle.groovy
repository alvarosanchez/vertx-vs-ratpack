package com.alvarosanchez.teams.vertx

import com.alvarosanchez.teams.core.Team
import com.alvarosanchez.teams.core.TeamRepository
import com.alvarosanchez.teams.core.TeamRepositoryJdbcImpl
import groovy.json.JsonOutput
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import io.vertx.core.Future
import io.vertx.groovy.core.eventbus.EventBus
import io.vertx.groovy.core.eventbus.Message
import io.vertx.lang.groovy.GroovyVerticle
import org.h2.jdbcx.JdbcConnectionPool

import javax.sql.DataSource

@Slf4j
class TeamsRepositoryVerticle extends GroovyVerticle {

    TeamRepository repository

    DataSource dataSource = JdbcConnectionPool.create("jdbc:h2:mem:teams", "sa", "")

    @Override
    void start(Future<Void> startFuture) throws Exception {
        log.debug "Initialising DB"
        repository = new TeamRepositoryJdbcImpl(dataSource)
        repository.init()

        EventBus eb = vertx.eventBus()
        log.debug "Registering event bus handlers"

        eb.consumer("teams").handler { Message message ->
            Map<String, Object> body = message.body() as Map<String, Object>
            log.debug "Received message on the event bus: ${body}"
            String response = ""

            switch (body.action) {
                case 'list':
                    List<Team> teams = repository.list()
                    response = JsonOutput.toJson(teams)
                    break

                case 'save':
                    Team team = new Team(body.team as Map)
                    repository.save(team)
                    response = JsonOutput.toJson(success: true)
                    break

                case 'show':
                    Team team = repository.findById(body.teamId as Long)
                    response = JsonOutput.toJson(team)
                    break

                case 'update':
                    Team given = new Team(body.team as Map)
                    Team existing = repository.findById(given.id)
                    existing.name = given.name
                    repository.update(existing)
                    response = JsonOutput.toJson(success: true)
                    break

                case 'delete':
                    Team team = repository.findById(body.teamId as Long)
                    repository.delete(team)
                    response = JsonOutput.toJson(success: true)
                    break
            }

            message.reply(response)
        }

        startFuture.complete()
    }

    @Override
    void stop() throws Exception {
        new Sql(dataSource).execute "DROP ALL OBJECTS DELETE FILES"
        log.debug "Database dropped"
    }
}
