Instructions on how to run the program:
1. Compile HttpServer.java.
2. Run httpserver.HttpServer with arguments {(port number), (relative path to public directory)}.

For example: 
javac httpserver/HttpServer.java
java httpserver.HttpServer 80 public

Contributions:
oe222fh_olof_enström : 50%
ce223af_christoffer_eid: 50%

Testing HTTP response codes:
302 - Go to URL http://hostip:portnumber/a/redirect.html
404 - Go to URL http://hostip:portnumber/q/abc.html
500 - Make a PUT request to the server.