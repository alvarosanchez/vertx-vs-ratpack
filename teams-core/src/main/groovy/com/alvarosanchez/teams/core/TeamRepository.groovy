package com.alvarosanchez.teams.core

interface TeamRepository {

    void init()

    void save(Team team)

    Team findById(Long id)
    Team findbyName(String name)
    List<Team> list()

    void update(Team team)

    void delete(Team team)

}