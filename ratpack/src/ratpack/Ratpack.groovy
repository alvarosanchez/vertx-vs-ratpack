import com.alvarosanchez.teams.ratpack.TeamsService
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

  handlers {

    all(RequestLogger.ncsa())

    prefix('teams') {

      get(':teamId') {

      }

    }

  }
}
