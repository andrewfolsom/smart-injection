package com.template.webserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.flows.ProposeWellFlow;
import com.template.states.WellState;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.FieldInfo;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.time.LocalDate;
import java.util.*;

/**
 * Define your API endpoints here.
 */
@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    @Configuration
    class Plugin {
        @Bean
        public ObjectMapper registerModule() { return JacksonSupport.createNonRpcMapper(); }
    }

    @GetMapping(value = "/wells", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<StateAndRef<WellState>> getWells() {
        return proxy.vaultQuery(WellState.class).getStates();
    }

    @GetMapping(value = "/templateendpoint", produces = "text/plain")
    private String templateendpoint() {
        return "Define an endpoint here.";
    }

    @GetMapping(value = "/flows", produces = "text/plain")
    private String flows() { return proxy.registeredFlows().toString(); }

    @GetMapping(value = "/propose", produces = "text/plain")
    public ResponseEntity<String> createWell() throws FileNotFoundException, FileAlreadyExistsException {
        String name = "O=PartyB,L=New York,C=US";
        String wellName = "Wellbert";
        String lease = "Your Field";
        List<Float> location = new ArrayList<>();
        location.add(27.777f);
        location.add(39.111f);
        String locationType = "Lat/Long";
        InputStream jarFile = new FileInputStream("/home/andrewfolsom/Desktop/attachments/test.jar");
        SecureHash hash = proxy.uploadAttachmentWithMetadata(jarFile, "Andrew", "test.txt");

        Party me = proxy.nodeInfo().getLegalIdentities().get(0);
        Party calGem = Optional.ofNullable(proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(name))).orElseThrow(() -> new IllegalArgumentException("Unknown party name."));

        try {
            SignedTransaction result = proxy.startTrackedFlowDynamic(ProposeWellFlow.class, wellName, lease, calGem, location, locationType, hash).getReturnValue().get();
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction ID " + result.getId() + " comitted to ledger.\n" + result.getTx().getOutput(0));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @PostMapping(value = "/create", consumes = "application/json", produces = "text/plain")
    public ResponseEntity<String> createWellTwo(@RequestBody JSObject well) throws FileNotFoundException, FileAlreadyExistsException {
        Party me = proxy.nodeInfo().getLegalIdentities().get(0);
        List<Float> location = new ArrayList<>();
        location.add(Float.parseFloat((String) well.getMember("xLoc")));
        location.add(Float.parseFloat((String) well.getMember("yLoc")));
        location.add(Float.parseFloat((String) well.getMember("zLoc")));
        String name = "O=PartyB,L=New York,C=US";
        Party calGem = Optional.ofNullable(proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(name))).orElseThrow(() -> new IllegalArgumentException("Unknown party name."));

        try {
            SignedTransaction result = proxy.startTrackedFlowDynamic(ProposeWellFlow.class, well.getMember("wellName"), well.getMember("lease"), calGem, location, well.getMember("locationType"), null).getReturnValue().get();
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction ID " + result.getId() + " comitted to ledger.\n" + result.getTx().getOutput(0));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

}