package com.alvarosanchez.teams.ratpack

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import ratpack.exec.Blocking
import ratpack.rx.RxRatpack
import ratpack.server.Service
import ratpack.server.StartEvent
import ratpack.server.StopEvent

import javax.inject.Inject

import static ratpack.rx.RxRatpack.observe

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
            sql.executeInsert('CREATE TABLE teams(id int auto_increment, name varchar(255))')
        }).subscribe {
            log.debug "Database initialised"
        }
    }

    @Override
    void onStop(StopEvent event) throws Exception {
        observe(Blocking.get {
            sql.executeInsert("DROP ALL OBJECTS DELETE FILES")
        }).subscribe {
            log.debug "Database stopped"
        }
    }

}
