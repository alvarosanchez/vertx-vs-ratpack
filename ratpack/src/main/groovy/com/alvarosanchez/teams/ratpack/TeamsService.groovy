package com.alvarosanchez.teams.ratpack

import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import ratpack.exec.Blocking
import ratpack.rx.RxRatpack
import ratpack.server.Service
import ratpack.server.StartEvent
import rx.Observable

import javax.inject.Inject

import static ratpack.rx.RxRatpack.observe
import static ratpack.rx.RxRatpack.observeEach

@Slf4j
class TeamsService implements Service {

    Sql sql

    @Inject
    TeamsService(Sql sql) {
        this.sql = sql
    }

    @Override
    void onStart(StartEvent event) throws Exception {
        RxRatpack.initialize()
        observe(Blocking.get {
            sql.executeInsert('CREATE TABLE IF NOT EXISTS teams(id int auto_increment, name varchar(255))')
        }).subscribe {
            log.debug "Database initialised"
        }
    }

    Observable<Team> list() {
        observeEach(Blocking.get {
            sql.rows('SELECT * FROM teams')
        }).map team
    }

    Observable<Team> findById(Long id) {
        observe(Blocking.get {
            sql.firstRow('SELECT * FROM teams WHERE id = :teamId', [teamId: id])
        }).map team
    }

    Observable<Integer> update(Team team) {
        observe(Blocking.get {
            sql.executeUpdate('UPDATE teams SET name = :name WHERE id = :id', [name: team.name, id: team.id])
        })

    }

    private team = { GroovyRowResult row ->
        return new Team(id: row.id, name: row.name)
    }


}
