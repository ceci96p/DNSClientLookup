# DNSClientLookup
Command-line DNS recursive resolver that queries messages to find a given domain implemented in Java. Project developed 
for a computer science course.

After compiling in the command-line please run: java -jar DNSLookupService.jar (root server ip #), for example:

```java -jar DNSLookupService.jar  198.41.0.4```

The DNS Resolver will take commands from a client like:

Change root server: ```SERVER (root server ip #)```

Quit connection: ```QUIT``` or ```EXIT```

Trace resolver queries: ```TRACE (on or off)```

Lookup address: ```LOOKUP (ip # or domain name)```
