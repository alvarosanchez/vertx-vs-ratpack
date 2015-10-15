package com.alvarosanchez.teams.core

import groovy.sql.Sql

import javax.sql.DataSource

class TeamRepositoryJdbcImpl implements TeamRepository {

    DataSource dataSource

    TeamRepositoryJdbcImpl(DataSource dataSource) {
        this.dataSource = dataSource
    }

    @Override
    void init() {
        Sql sql = new Sql(dataSource)
        sql.execute('CREATE TABLE teams(id int auto_increment, name varchar(255))')
        sql.close()
    }

    @Override
    void save(Team team) {
        Sql sql = new Sql(dataSource)
        sql.execute("INSERT INTO teams (name) VALUES (:name)", [name: team.name])
        sql.close()
    }

    @Override
    Team findbyName(String name) {
        Sql sql = new Sql(dataSource)
        def result = sql.firstRow("SELECT * FROM teams WHERE name = :name", [name: name])
        Team team
        if (result) {
            team = new Team(id: result.id, name: result.name)
        }
        sql.close()
        return team
    }

    @Override
    List<Team> list() {
        Sql sql = new Sql(dataSource)
        List<Team> teams = []

        sql.eachRow("SELECT * FROM teams") {
            teams << new Team(id: it.id, name: it.name)
        }

        sql.close()
        return teams
    }

    @Override
    void update(Team team) {
        Sql sql = new Sql(dataSource)
        sql.executeUpdate("UPDATE teams SET name = :name WHERE id = :id", [name: team.name, id: team.id])
        sql.close()
    }

    @Override
    void delete(Team team) {
        Sql sql = new Sql(dataSource)
        sql.executeUpdate("DELETE FROM teams WHERE id = :id", [id: team.id])
        sql.close()
    }
}
