wrk.method = "PUT"
io.input("kryo.dat")
wrk.body = io.read("*all")
wrk.headers["Content-Type"] = "application/x-kryo"
