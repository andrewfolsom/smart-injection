package com.template.webserver;

import com.template.flows.ProposeWellFlow;
import com.template.states.WellState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
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
        InputStream jarFile = new FileInputStream("/home/andrew/Desktop/attachments/test.jar");
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

}