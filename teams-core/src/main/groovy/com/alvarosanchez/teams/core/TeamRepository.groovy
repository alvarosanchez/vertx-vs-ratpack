package com.alvarosanchez.teams.core

interface TeamRepository {

    void init()

    void save(Team team)

    Team findbyName(String name)
    List<Team> list()

    void update(Team team)

    void delete(Team team)

}