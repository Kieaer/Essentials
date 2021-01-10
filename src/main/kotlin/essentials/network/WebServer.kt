package essentials.network

class WebServer {
    fun run(){
/*
        val server = Server() // default port is 9000
        server.staticFiles("/public") // specify path to static content folder
        server.get("/hello", { req, res -> res.send("Hello") }) // use res.send to send data to response explicitly
        server.get("/hello_simple", { req, res -> "Hello" }) // or just return some value and that will be sent to response automatically
*/
        //server.get("/do/.*/smth", { req, res -> res.send("Hello world") }) // also you could bind handlers by regular expressions
/*
        server.post("/data", { req, res -> res.send(req.content, Status.Created) }) // send method accepts status
        // Filters
        server.before("/hello", { req, res -> res.send("before\n") })
        server.before({ req, res -> res.send("ALL before\n") })
        server.after("/hello", { req, res -> res.send("\nafter\n") })
        server.after({ req, res -> res.send("ALL after\n") })
        // exceptions handler
        server.exception(IllegalStateException::class, { req, res -> "Illegal State" })
        server.start(9443, true, "./keystore.jks", "password") // for secured conection
        server.start()
*/
    }
}