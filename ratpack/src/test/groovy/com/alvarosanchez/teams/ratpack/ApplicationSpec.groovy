package com.alvarosanchez.teams.ratpack

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.sql.Sql
import org.h2.jdbcx.JdbcConnectionPool
import ratpack.groovy.Groovy
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.h2.H2Module
import ratpack.server.RatpackServer
import ratpack.test.ServerBackedApplicationUnderTest
import ratpack.test.embed.EmbeddedApp
import ratpack.test.http.TestHttpClient
import spock.lang.Shared
import spock.lang.Specification

import javax.sql.DataSource

class ApplicationSpec extends Specification {

    @Shared
    ServerBackedApplicationUnderTest appUnderTest = new GroovyRatpackMainApplicationUnderTest()

    @Delegate
    TestHttpClient testClient = appUnderTest.httpClient

    @Shared
    String token

    @Shared
    DataSource dataSource

    void setupSpec() {
        dataSource = new H2Module().dataSource()

    }

    void setup() {
        token = postText('login')
        requestSpec { it.headers.add('Authorization', "Bearer ${token}") }
    }

    void cleanup() {
        Sql sql = new Sql(dataSource)
        sql.executeInsert("DROP ALL OBJECTS DELETE FILES")
        sql.executeInsert('CREATE TABLE teams(id int auto_increment, name varchar(255))')
    }

    void 'API is protected'() {
        given:
        requestSpec { it.headers.clear() }

        when:
        get('teams')

        then:
        response.statusCode == 401
    }

    void 'it can create teams'() {
        given:
        requestSpec { spec ->
            spec.headers.add('Authorization', "Bearer ${token}")
            spec.body { b ->
                b.type('application/json').text(JsonOutput.toJson(name: 'Real Madrid CF'))
            }
        }

        when:
        post('teams')

        then:
        new JsonSlurper().parseText(response.body.text).success

        when:
        get('teams/1')

        then:
        new JsonSlurper().parseText(response.body.text).name == 'Real Madrid CF'
    }

    void 'it can list teams'() {
        given:
        create(new Team(name: 'Real Madrid CF'))
        create(new Team(name: 'FC Barcelona'))

        when:
        get('teams')

        then:
        new JsonSlurper().parseText(response.body.text).size() == 2
    }

    void 'it can find team by id'() {
        given:
        create(new Team(name: 'Real Madrid CF'))

        when:
        get('teams/1')

        then:
        new JsonSlurper().parseText(response.body.text).name == 'Real Madrid CF'
    }

    void 'it can modify teams'() {
        given:
        create(new Team(name: 'Real Madrid CF'))
        requestSpec { spec ->
            spec.headers.add('Authorization', "Bearer ${token}")
            spec.body { b ->
                b.type('application/json').text(JsonOutput.toJson(name: 'Real Madrid Club de Fútbol'))
            }
        }

        when:
        put('teams/1')

        then:
        new JsonSlurper().parseText(response.body.text).success

        when:
        get('teams/1')

        then:
        new JsonSlurper().parseText(response.body.text).name == 'Real Madrid Club de Fútbol'
    }

    void 'it can delete teams'() {
        given:
        create(new Team(name: 'Real Madrid CF'))

        when:
        delete('teams/1')

        then:
        new JsonSlurper().parseText(response.body.text).success
    }

    private void create(Team team) {
        Sql sql = new Sql(dataSource)
        sql.execute("INSERT INTO teams (name) VALUES (:name)", [name: team.name])
        sql.close()
    }

}
