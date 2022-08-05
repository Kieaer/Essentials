package essentials.network

class WebServer {
    fun run() {/*
        val server = Server() // default port is 9000
        server.staticFiles("/public") // specify path to static content folder
        server.get("/hello", { req, res -> res.send("Hello") }) // use res.send to send data to response explicitly
        server.get("/hello_simple", { req, res -> "Hello" }) // or just return some value and that will be sent to response automatically
*/ //server.get("/do/.*/smth", { req, res -> res.send("Hello world") }) // also you could bind handlers by regular expressions
    }
}