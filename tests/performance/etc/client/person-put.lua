wrk.method = "PUT"
io.input("person.dat")
wrk.body = io.read("*all")
wrk.headers["Content-Type"] = "application/person"
