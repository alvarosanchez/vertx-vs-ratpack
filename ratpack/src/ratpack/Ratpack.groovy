import static ratpack.groovy.Groovy.ratpack

ratpack {
  handlers {
    all {
      render "Hello World"
    }
  }
}
