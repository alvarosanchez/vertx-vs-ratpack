package com.alvarosanchez.teams.ratpack

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
        }).map { row ->
            return new Team(id: row.id, name: row.name)
        }
    }

}
