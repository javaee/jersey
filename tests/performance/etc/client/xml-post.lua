wrk.method = "POST"
io.input("xml.dat")
wrk.body = io.read("*all")
wrk.headers["Content-Type"] = "application/xml"
