package ca.ubc.cs.cs317.dnslookup;

import javax.swing.plaf.synth.SynthScrollBarUI;
import java.io.Console;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeoutException;


public class DNSLookupService {

    private static final int DEFAULT_DNS_PORT = 53;
    private static final int MAX_INDIRECTION_LEVEL = 10;

    private static InetAddress rootServer;
    private static boolean verboseTracing = false;
    private static DatagramSocket socket;

    private static DNSCache cache = DNSCache.getInstance();

    private static Random random = new Random();


    /**
     * Main function, called when program is first invoked.
     *
     * @param args list of arguments specified in the command line.
     */
    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Invalid call. Usage:");
            System.err.println("\tjava -jar DNSLookupService.jar rootServer");
            System.err.println("where rootServer is the IP address (in dotted form) of the root DNS server to start the search at.");
            System.exit(1);
        }

        try {
            rootServer = InetAddress.getByName(args[0]);
            System.out.println("Root DNS server is: " + rootServer.getHostAddress());
        } catch (UnknownHostException e) {
            System.err.println("Invalid root server (" + e.getMessage() + ").");
            System.exit(1);
        }

        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(5000);
        } catch (SocketException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        Scanner in = new Scanner(System.in);
        Console console = System.console();
        do {
            // Use console if one is available, or standard input if not.
            String commandLine;
            if (console != null) {
                System.out.print("DNSLOOKUP> ");
                commandLine = console.readLine();
            } else
                try {
                    commandLine = in.nextLine();
                } catch (NoSuchElementException ex) {
                    break;
                }
            // If reached end-of-file, leave
            if (commandLine == null) break;

            // Ignore leading/trailing spaces and anything beyond a comment character
            commandLine = commandLine.trim().split("#", 2)[0];

            // If no command shown, skip to next command
            if (commandLine.trim().isEmpty()) continue;

            String[] commandArgs = commandLine.split(" ");

            if (commandArgs[0].equalsIgnoreCase("quit") ||
                    commandArgs[0].equalsIgnoreCase("exit"))
                break;
            else if (commandArgs[0].equalsIgnoreCase("server")) {
                // SERVER: Change root nameserver
                if (commandArgs.length == 2) {
                    try {
                        rootServer = InetAddress.getByName(commandArgs[1]);
                        System.out.println("Root DNS server is now: " + rootServer.getHostAddress());
                    } catch (UnknownHostException e) {
                        System.out.println("Invalid root server (" + e.getMessage() + ").");
                        continue;
                    }
                } else {
                    System.out.println("Invalid call. Format:\n\tserver IP");
                    continue;
                }
            } else if (commandArgs[0].equalsIgnoreCase("trace")) {
                // TRACE: Turn trace setting on or off
                if (commandArgs.length == 2) {
                    if (commandArgs[1].equalsIgnoreCase("on"))
                        verboseTracing = true;
                    else if (commandArgs[1].equalsIgnoreCase("off"))
                        verboseTracing = false;
                    else {
                        System.err.println("Invalid call. Format:\n\ttrace on|off");
                        continue;
                    }
                    System.out.println("Verbose tracing is now: " + (verboseTracing ? "ON" : "OFF"));
                } else {
                    System.err.println("Invalid call. Format:\n\ttrace on|off");
                    continue;
                }
            } else if (commandArgs[0].equalsIgnoreCase("lookup") ||
                    commandArgs[0].equalsIgnoreCase("l")) {
                // LOOKUP: Find and print all results associated to a name.
                RecordType type;
                if (commandArgs.length == 2)
                    type = RecordType.A;
                else if (commandArgs.length == 3)
                    try {
                        type = RecordType.valueOf(commandArgs[2].toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        System.err.println("Invalid query type. Must be one of:\n\tA, AAAA, NS, MX, CNAME");
                        continue;
                    }
                else {
                    System.err.println("Invalid call. Format:\n\tlookup hostName [type]");
                    continue;
                }
                findAndPrintResults(commandArgs[1], type);
            } else if (commandArgs[0].equalsIgnoreCase("dump")) {
                // DUMP: Print all results still cached
                cache.forEachNode(DNSLookupService::printResults);
            } else {
                System.err.println("Invalid command. Valid commands are:");
                System.err.println("\tlookup fqdn [type]");
                System.err.println("\ttrace on|off");
                System.err.println("\tserver IP");
                System.err.println("\tdump");
                System.err.println("\tquit");
                continue;
            }

        } while (true);

        socket.close();
        System.out.println("Goodbye!");
    }

    /**
     * Finds all results for a host name and type and prints them on the standard output.
     *
     * @param hostName Fully qualified domain name of the host being searched.
     * @param type     Record type for search.
     */
    private static void findAndPrintResults(String hostName, RecordType type) {


        DNSNode node = new DNSNode(hostName, type);
        printResults(node, getResults(node, 0));
    }

    /**
     * Finds all the result for a specific node.
     *
     * @param node             Host and record type to be used for search.
     * @param indirectionLevel Control to limit the number of recursive calls due to CNAME redirection.
     *                         The initial call should be made with 0 (zero), while recursive calls for
     *                         regarding CNAME results should increment this value by 1. Once this value
     *                         reaches MAX_INDIRECTION_LEVEL, the function prints an error message and
     *                         returns an empty set.
     * @return A set of resource records corresponding to the specific query requested.
     */
    private static Set<ResourceRecord> getResults(DNSNode node, int indirectionLevel) {
        InetAddress nameServer = rootServer;
        Set<ResourceRecord> rrInCache;
        Set<ResourceRecord> emptySet = Collections.emptySet();

        if (indirectionLevel > MAX_INDIRECTION_LEVEL) { // max CNAME searches
            System.err.println("Maximum number of indirection levels reached.");
            return emptySet;
        }
        if (node.getHostName().substring(0,1).equals( ".")){ //fail if hostname begins with an dot
            return emptySet;
        }
        if (node.getHostName().substring(node.getHostName().length()-1,node.getHostName().length()).equals(".")){ //if host name has a dot in the end take it out because it is stilla  valid address
            node.setHostName(node.getHostName().substring(0,node.getHostName().length()-1));
        }
        if(!cache.getCachedResults(node).isEmpty() ){// if cache is not empty in relation to given node, then return cache results
            return cache.getCachedResults(node);
        }

        DNSNode myCnameNode = new DNSNode(node.getHostName(), RecordType.CNAME); // makes a node that is type CNAME
        // to check for CNAME-ANSWER in the cache after we do query the first time thats why we loop 2 times
            int i = 0;
            while(i<2){
            // Check if we have CNAME in the cache
            // the FIRST TIME  --> empty cacheResults
            // the SECOND TIME i=1, cacheResult has results if CNAME is an answer
            rrInCache = cache.getCachedResults(myCnameNode);
            if (rrInCache.isEmpty()) {
                // We don't have CNAME in cache
                // GO START A NEW SEARCH AT THE ROOT

                retrieveResultsFromServer(node, nameServer, true); // update cache results from: retrieveResult

                rrInCache = cache.getCachedResults(node); // no results for node at i=0

                if (!rrInCache.isEmpty()) {
                    return rrInCache;
                }
            } else {
                // new Query for CNAME given from answer
                Set<ResourceRecord> cnameResults = new HashSet<ResourceRecord>();
                for (ResourceRecord rr : rrInCache) {
                        String myCname = rr.getTextResult();
                        DNSNode cn = new DNSNode(myCname, node.getType());
                    cnameResults.addAll(getResults(cn, indirectionLevel + 1));
                }
                return cnameResults;
            }
            i++;
        }
        return emptySet;
    }


    /**
     * Retrieves DNS results from a specified DNS server. Queries are sent in iterative mode,
     * and the query is repeated with a new server if the provided one is non-authoritative.
     * Results are stored in the cache.
     *
     * @param node   Host name and record type to be used for the query.
     * @param server Address of the server to be used for the query.
     *
     */

    private static void retrieveResultsFromServer(DNSNode node, InetAddress server, Boolean repeat) {
        QueryHeader h = new QueryHeader();
        QueryQuestion q = new QueryQuestion();

        byte[] h1 = h.setBuffer(); //write header
        byte[] q1 = q.setBuffer(node.getHostName(), node.getType());//write question
        byte[] combined = new byte[h1.length + q1.length];
        byte[] responseBytes = new byte[1024]; //make byte array for response

        for (int i = 0; i < combined.length; ++i) { //combine header and question into packet
            combined[i] = i < h1.length ? h1[i] : q1[i - h1.length];
        }
        DatagramPacket requestPacket = new DatagramPacket(combined, combined.length, server, DEFAULT_DNS_PORT); //datagram packet to send
        DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length);//datagram packet to receive

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.send(requestPacket); //send datagram
            socket.setSoTimeout(5000); //set timeout for response

            socket.receive(responsePacket); //receive datagram back
            DecodePacket response = new DecodePacket(responsePacket, q); //decode datagarm into response
            System.out.print("Query ID" + "     " + h.getHeaderID() + " " + node.getHostName() + "  " + q.getType() + " " + "-->" + server + "\n"); //TODO gettype()

            if (response.getRcode()>= 1 && response.getRcode()<= 5){ //check Rcode for errors
                return;
            }

            if(verboseTracing == true) { // if trace on then print information about received packet
            System.out.print("Response ID:" + " " + response.getResponseID() + " " + "Authoritative" + "  " + "=" + " " + response.isAuthoritative() + "\n");

            System.out.print("  " + "Answers" + " (" + response.getAnswerCount() + ")" + "\n");
            if (response.getAnswerCount() > 0) {
                for (RRecord i : response.getAnswers()) {
                    i.printInfo();
                }
            }
            System.out.print("  " + "Nameservers" + " (" + response.getAuthorityCount() + ")" + "\n");

            if (response.getAuthorityCount() > 0) {
                for (RRecord i : response.getAuthoritativeNameServers()) {
                    i.printInfo();
                }
            }
            System.out.print("  " + "Additional Information" + " (" + response.getAdditionalCount() + ")" + "\n");

            if (response.getAdditionalCount() > 0) {
                for (RRecord i : response.getAdditionalRecords())
                    i.printInfo();
            }
            }

            if (response.getAnswerCount() > 0) { //if response has answer records then parse to node object and place on cache
                response.convertDecodedPacketToResourceRecord(response.getAnswers(), cache);
            }
            if (response.getAuthorityCount() > 0) { //if response has authoritative records then parse to node object and place on cache
                response.convertDecodedPacketToResourceRecord(response.getAuthoritativeNameServers(), cache);
            }
            if (response.getAdditionalCount() > 0) { //if response has additional records answers then parse to node object and place on cache
                response.convertDecodedPacketToResourceRecord(response.getAdditionalRecords(), cache);
            }

            if (response.getAnswerCount() > 0) { // terminates recursion (base case)
                return;
            }

            if(response.getAuthorityCount() == 0){
                return;
            }

            String newAddress = response.getAuthoritativeNameServers()[0].getNameServer(); //get IP address for first authoritative record
            try {
                InetAddress newAddress_IA = InetAddress.getByName(newAddress);
                retrieveResultsFromServer(node, newAddress_IA, true); // recursive call - it will send packet to new server acquired in first call
                socket.close();
                return;

            } catch (UnknownHostException e) {
                return;
            }

        } catch (SocketTimeoutException e) {
            if(repeat == true){ //if socket response doesn't get back in a set time then call function again with no possibility to trye again if timeout happens again
                retrieveResultsFromServer(node,rootServer, false);
            }else if (repeat == false){
                return;// if it times out for a second time then return
            }
        } catch (IOException e) {
                e.printStackTrace();
            }
    }

        private static void verbosePrintResourceRecord(ResourceRecord record, int rtype) {
        if (verboseTracing)
            System.out.format("       %-30s %-10d %-4s %s\n", record.getHostName(),
                    record.getTTL(),
                    record.getType() == RecordType.OTHER ? rtype : record.getType(),
                    record.getTextResult());
    }

    /**
     * Prints the result of a DNS query.
     *
     * @param node    Host name and record type used for the query.
     * @param results Set of results to be printed for the node.
     */
    private static void printResults(DNSNode node, Set<ResourceRecord> results) {
        if (results.isEmpty())
            System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(),
                    node.getType(), -1, "0.0.0.0");
        for (ResourceRecord record : results) {
            System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(),
                    node.getType(), record.getTTL(), record.getTextResult());
        }
    }
}
