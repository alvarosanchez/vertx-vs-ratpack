import com.alvarosanchez.teams.ratpack.JwtAuthentication
import com.alvarosanchez.teams.ratpack.Team
import com.alvarosanchez.teams.ratpack.TeamsService
import groovy.json.JsonOutput
import ratpack.groovy.sql.SqlModule
import ratpack.h2.H2Module
import ratpack.handling.RequestLogger

import static ratpack.groovy.Groovy.ratpack

ratpack {

  serverConfig {
    props(getClass().getResource("/teams.properties"))
  }

  bindings {
    module SqlModule
    module H2Module

    bind TeamsService
  }

  handlers { TeamsService teamsService ->

    post('login', JwtAuthentication.login())

    prefix('teams') {

      all JwtAuthentication.authenticate()

      get(':teamId') {

      }

      get {
        teamsService.list().toList().subscribe { List<Team> teams ->
          response.send JsonOutput.toJson(teams)
        }
      }

    }

  }
}
