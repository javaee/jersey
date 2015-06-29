wrk.method = "POST"
io.input("text.dat")
wrk.body = io.read("*all")
wrk.headers["Content-Type"] = "text/plain"
