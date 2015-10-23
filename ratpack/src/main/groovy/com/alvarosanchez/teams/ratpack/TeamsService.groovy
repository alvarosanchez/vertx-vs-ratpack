package com.alvarosanchez.teams.ratpack

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import ratpack.rx.RxRatpack
import ratpack.server.Service
import ratpack.server.StartEvent
import ratpack.server.StopEvent

import javax.inject.Inject

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
        sql.executeInsert('CREATE TABLE teams(id int auto_increment, name varchar(255))')
        log.info "Database initialised"
    }

    @Override
    void onStop(StopEvent event) throws Exception {
        sql.executeInsert("DROP ALL OBJECTS DELETE FILES")
    }
}
