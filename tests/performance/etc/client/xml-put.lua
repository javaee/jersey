wrk.method = "PUT"
io.input("xml.dat")
wrk.body = io.read("*all")
wrk.headers["Content-Type"] = "application/xml"
