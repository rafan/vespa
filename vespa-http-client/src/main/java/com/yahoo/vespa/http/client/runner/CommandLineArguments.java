// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.http.client.runner;

import com.google.common.base.Splitter;
import com.yahoo.vespa.http.client.config.Cluster;
import com.yahoo.vespa.http.client.config.ConnectionParams;
import com.yahoo.vespa.http.client.config.Endpoint;
import com.yahoo.vespa.http.client.config.FeedParams;
import com.yahoo.vespa.http.client.config.SessionParams;
import io.airlift.command.Command;
import io.airlift.command.HelpOption;
import io.airlift.command.Option;
import io.airlift.command.SingleCommand;
import org.apache.http.Header;
import org.apache.http.ParseException;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.message.BasicLineParser;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Commandline interface for the binary.
 *
 * @author dybis
 */
@Command(name = "vespa-http-client",
         description = "This is a tool for feeding xml or json data to a Vespa application.")
public class CommandLineArguments {

    /**
     * Creates a CommandLineArguments instance and populates it with data.
     *
     * @param args array of arguments.
     * @return null on failure or if help option is set to true.
     */
    static CommandLineArguments build(String[] args) {
        CommandLineArguments cmdArgs;
        try {
            cmdArgs =  SingleCommand.singleCommand(CommandLineArguments.class).parse(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("Use --help to show usage.\n");
            return null;
        }
        if (cmdArgs.helpOption.showHelpIfRequested()) {
            return null;
        }
        if (cmdArgs.endpointArg != null) {
            if (cmdArgs.hostArg != null) {
                System.err.println("Cannot set both '--host' and '--endpoint' ");
                return null;
            }
            try {
                URL url = new URL(cmdArgs.endpointArg);
            } catch (MalformedURLException e) {
                e.printStackTrace(System.err);
                return null;
            }
        } else {
            if (cmdArgs.hostArg == null) {
                System.err.println("'--host' or '--endpoint' not set.");
                return null;
            }
        }
        if (cmdArgs.priorityArg != null && ! checkPriorityFlag(cmdArgs.priorityArg)) {
            return null;
        }

        for (String header : cmdArgs.headers) {
            try {
                cmdArgs.parsedHeaders.add(BasicLineParser.parseHeader(header, null));
            } catch (ParseException e) {
                System.err.printf("Invalid header: '%s' (%s)%n", header, e.getMessage());
                return null;
            }
        }

        if (cmdArgs.privateKeyPath == null && cmdArgs.certificatePath != null ||
                cmdArgs.privateKeyPath != null && cmdArgs.certificatePath == null) {
            System.err.println("Both '--privateKey' and '--certificate' must be set");
            return null;
        }

        return cmdArgs;
    }

    private static boolean checkPriorityFlag(String priorityArg) {
        switch (priorityArg) {
            case "HIGHEST":
            case "VERY_HIGH":
            case "HIGH_1":
            case "HIGH_2":
            case "HIGH_3":
            case "NORMAL_1":
            case "NORMAL_2":
            case "NORMAL_3":
            case "NORMAL_4":
            case "NORMAL_5":
            case "NORMAL_6":
            case "LOW_1":
            case "LOW_2":
            case "LOW_3":
            case "VERY_LOW":
            case "LOWEST":
                return true;
            default:
                System.err.println("Not valid value for priority. Allowed values are HIGHEST, VERY_HIGH, HIGH_[1-3], " +
                                   "NORMAL_[1-6], LOW_[1-3], VERY_LOW, and LOWEST.");
                return false;
        }
    }

    // TODO Don't duplicate default values from ConnectionParams.Builder. Some defaults are already inconsistent.

    @Inject
    private HelpOption helpOption;

    @Option(name = {"--useV3Protocol"}, description = "Use V3 protocol to gateway. This is the default protocol.")
    private boolean enableV3Protocol = true;

    @Option(name = {"--file"},
            description = "The name of the input file to read.")
    private String fileArg = null;

    @Option(name = {"--add-root-element-to-xml"},
            description = "Add <vespafeed> tag to XML document, makes it easier to feed raw data.")
    private boolean addRootElementToXml = false;

    @Option(name = {"--route"},
            description = "(=default)The route to send the data to.")
    private String routeArg = "default";

    @Option(name = {"--endpoint"},
            description = "Vespa endpoint.")
    private String endpointArg;

    @Option(name = {"--host"},
            description = "The host(s) for the gateway. If using several, use comma to separate them.")
    private String hostArg;

    @Option(name = {"--port"},
            description = "The port for the host of the gateway.")
    private int portArg = 4080;

    @Option(name = {"--proxyHost"},
            description = "proxy host")
    private String proxyHostArg;

    @Option(name = {"--proxyPort"},
            description = "proxy port")
    private int proxyPortArg = 4080;

    @Option(name = {"--timeout"},
            description = "(=180) The time (in seconds) allowed for sending operations.")
    private long timeoutArg = 180;

    @Option(name = {"--useCompression"},
            description = "Use compression over network.")
    private boolean useCompressionArg = false;

    @Option(name = {"--useDynamicThrottling"},
            description = "Try to maximize throughput by using dynamic throttling.")
    private boolean useDynamicThrottlingArg = false;

    @Option(name = {"--maxpending"},
            description = "The maximum number of operations that are allowed " +
                    "to be pending at any given time.")
    private int maxPendingOperationCountArg = 10000;

    @Option(name = {"-v", "--verbose"},
            description = "Enable verbose output of progress.")
    private boolean verboseArg = false;

    @Option(name = {"--noretry"},
            description = "Turns off retries of recoverable failures..")
    private boolean noRetryArg = false;

    @Option(name = {"--retrydelay"},
            description = "The time (in seconds) to wait between retries of a failed operation.")
    private int retrydelayArg = 1;

    @Option(name = {"--trace"},
            description = "(=0 (=off)) The trace level of network traffic.")
    private int traceArg = 0;

    @Option(name = {"--printTraceEveryXOperation"},
            description = "(=1) How often to to tracing.")
    private int traceEveryXOperation = 1;

    @Option(name = {"--validate"},
            description = "Run validation tool on input files instead of feeding them.")
    private boolean validateArg = false;

    @Option(name = {"--priority"},
            description = "Specify priority of sent messages, see documentation ")
    private String priorityArg = null;

    @Option(name = {"--numPersistentConnectionsPerEndpoint"},
            description = "How many tcp connections to establish per endoint.)")
    private int numPersistentConnectionsPerEndpoint = 4;

    @Option(name = {"--maxChunkSizeBytes"},
            description = "How much data to send to gateway in each message.")
    private int maxChunkSizeBytes = 20 * 1024;

    @Option(name = {"--maxSleepTimeMs"},
            description = "maxSleepTimeMs")
    private int maxSleepTimeMs = 3 * 1000; // same as FeedParams default

    @Option(name = {"--whenVerboseEnabledPrintMessageForEveryXDocuments"},
            description = "How often to print verbose message.)")
    private int whenVerboseEnabledPrintMessageForEveryXDocuments = 1000;

    @Option(name = {"--useTls"},
            description = "Use TLS when connecting to endpoint")
    private boolean useTls = false;

    @Option(name = {"--insecure", "--disable-hostname-verification"},
            description = "Skip hostname verification when using TLS")
    private boolean insecure = false;

    @Option(name = {"--header"},
            description = "Add http header to every request. Header must have the format '<Name>: <Value>'. Use this parameter multiple times for multiple headers")
    private List<String> headers = new ArrayList<>();

    @Option(name = {"--vespaTls"},
            description = "BETA! Use Vespa TLS configuration from environment if available. Other HTTPS/TLS configuration will be ignored if this is set.")
    private boolean useTlsConfigFromEnvironment = false;

    @Option(name = {"--connectionTimeToLive"},
            description = "Maximum time to live for persistent connections. Specified as integer, in seconds.")
    private long connectionTimeToLive = 15;

    @Option(name = {"--certificate"},
            description = "Path to a file containing a PEM encoded x509 certificate")
    private String certificatePath;

    @Option(name = {"--privateKey"},
            description = "Path to a file containing a PEM encoded private key")
    private String privateKeyPath;

    @Option(name = "--caCertificates",
            description = "Path to a file containing a PEM encoded CA certificates")
    private String caCertificatesPath;

    private final List<Header> parsedHeaders = new ArrayList<>();

    int getWhenVerboseEnabledPrintMessageForEveryXDocuments() {
        return whenVerboseEnabledPrintMessageForEveryXDocuments;
    }

    public String getFile() { return fileArg; };

    public boolean getVerbose() { return verboseArg; }

    public boolean getAddRootElementToXml() { return addRootElementToXml; }

    SessionParams createSessionParams(boolean useJson) {
        int minThrottleValue = useDynamicThrottlingArg ? 10 : 0;
        Path privateKeyPath = Optional.ofNullable(this.privateKeyPath).map(Paths::get).orElse(null);
        Path certificatePath = Optional.ofNullable(this.certificatePath).map(Paths::get).orElse(null);
        Path caCertificatesPath = Optional.ofNullable(this.caCertificatesPath).map(Paths::get).orElse(null);
        ConnectionParams.Builder connectionParamsBuilder = new ConnectionParams.Builder();
        parsedHeaders.forEach(header -> connectionParamsBuilder.addHeader(header.getName(), header.getValue()));
        SessionParams.Builder builder = new SessionParams.Builder()
                .setFeedParams(
                        new FeedParams.Builder()
                                .setDataFormat(useJson
                                        ? FeedParams.DataFormat.JSON_UTF8
                                        : FeedParams.DataFormat.XML_UTF8)
                                .setRoute(routeArg)
                                .setMaxInFlightRequests(maxPendingOperationCountArg)
                                .setClientTimeout(timeoutArg, TimeUnit.SECONDS)
                                .setServerTimeout(timeoutArg, TimeUnit.SECONDS)
                                .setLocalQueueTimeOut(timeoutArg * 1000)
                                .setPriority(priorityArg)
                                .setMaxChunkSizeBytes(maxChunkSizeBytes)
                                .setMaxSleepTimeMs(maxSleepTimeMs)
                                .build()
                )
                .setConnectionParams(
                        connectionParamsBuilder
                                .setHostnameVerifier(insecure ? NoopHostnameVerifier.INSTANCE :
                                        SSLConnectionSocketFactory.getDefaultHostnameVerifier())
                                .setUseCompression(useCompressionArg)
                                .setMaxRetries(noRetryArg ? 0 : 100)
                                .setMinTimeBetweenRetries(retrydelayArg, TimeUnit.SECONDS)
                                .setDryRun(validateArg)
                                .setTraceLevel(traceArg)
                                .setTraceEveryXOperation(traceEveryXOperation)
                                .setPrintTraceToStdErr(traceArg > 0)
                                .setNumPersistentConnectionsPerEndpoint(numPersistentConnectionsPerEndpoint)
                                .setCertificateAndPrivateKey(privateKeyPath, certificatePath)
                                .setCaCertificates(caCertificatesPath)
                                .setUseTlsConfigFromEnvironment(useTlsConfigFromEnvironment)
                                .setConnectionTimeToLive(Duration.ofSeconds(connectionTimeToLive))
                                .setProxyHost(proxyHostArg)
                                .setProxyPort(proxyPortArg)
                                .build()
                )
                        // Enable dynamic throttling.
                .setThrottlerMinSize(minThrottleValue)
                .setClientQueueSize(maxPendingOperationCountArg*2); // XXX match VespaRecordWriter
        if (endpointArg != null) {
            try {
                builder.addCluster(new Cluster.Builder()
                        .addEndpoint(Endpoint.create(new URL(endpointArg)))
                        .build());
            }
            catch (MalformedURLException e) {} // already checked when parsing arguments
        }
        else {
            Iterable<String> hosts = Splitter.on(',').trimResults().split(hostArg);
            for (String host : hosts) {
                builder.addCluster(new Cluster.Builder().addEndpoint(Endpoint.create(host, portArg, useTls))
                        .build());
            }
        }
        return builder.build();
    }

}
