@Grab('io.ratpack:ratpack-groovy:1.0.0')
import static ratpack.groovy.Groovy.ratpack

ratpack {
    serverConfig { port 8080 }
    handlers {
        all { render "Hello World!\n" }
    }
}
