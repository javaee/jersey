wrk.method = "PUT"
io.input("json.dat")
wrk.body = io.read("*all")
wrk.headers["Content-Type"] = "application/json"
