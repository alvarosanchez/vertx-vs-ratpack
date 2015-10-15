package com.alvarosanchez.teams.core

import groovy.sql.Sql
import org.h2.jdbcx.JdbcConnectionPool
import org.h2.jdbcx.JdbcDataSource
import spock.lang.Shared
import spock.lang.Specification

import javax.sql.DataSource

class TeamRepositorySpec extends Specification {

    @Shared
    TeamRepository teamRepository

    void setupSpec() {
        teamRepository = new TeamRepositoryJdbcImpl(JdbcConnectionPool.create("jdbc:h2:mem:teams", "sa", ""))
        teamRepository.init()
    }

    void 'it can save teams and read'() {
        given:
        Team team = new Team(name: 'Real Madrid C.F.')

        when:
        teamRepository.save(team)
        Team found = teamRepository.findbyName('Real Madrid C.F.')

        then:
        found

        cleanup:
        teamRepository.delete(found)
    }

    void 'it can delete teams'() {
        given:
        teamRepository.save(new Team(name: 'F.C. Barcelona'))
        Team team = teamRepository.findbyName('F.C. Barcelona')

        when:
        teamRepository.delete(team)

        then:
        teamRepository.findbyName('F.C. Barcelona') == null
    }

}
