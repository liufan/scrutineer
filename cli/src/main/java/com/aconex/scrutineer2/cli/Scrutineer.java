package com.aconex.scrutineer2.cli;

import com.aconex.scrutineer2.CoincidentFilteredStreamVerifierListener;
import com.aconex.scrutineer2.ConnectorConfig;
import com.aconex.scrutineer2.IdAndVersionFactory;
import com.aconex.scrutineer2.IdAndVersionStreamConnector;
import com.aconex.scrutineer2.IdAndVersionStreamVerifier;
import com.aconex.scrutineer2.IdAndVersionStreamVerifierListener;
import com.aconex.scrutineer2.LogUtils;
import com.aconex.scrutineer2.LongIdAndVersion;
import com.aconex.scrutineer2.PrintStreamOutputVersionStreamVerifierListener;
import com.aconex.scrutineer2.StringIdAndVersion;
import com.aconex.scrutineer2.cli.config.CliConfig;
import org.slf4j.Logger;

import java.io.IOException;

public class Scrutineer {
    private static final Logger LOG = LogUtils.loggerForThisClass();
    private final CliConfig cliConfig;
    private final IdAndVersionStreamVerifier idAndVersionStreamVerifier;

    public Scrutineer(CliConfig cliConfig, IdAndVersionStreamVerifier idAndVersionStreamVerifier) {
        this.cliConfig = cliConfig;
        this.idAndVersionStreamVerifier = idAndVersionStreamVerifier;
    }

    @SuppressWarnings("PMD.NcssMethodCount")
    public void verify() {
        IdAndVersionStreamVerifierListener verifierListener = createVerifierListener();

        try (IdAndVersionStreamConnector primaryStreamConnector = createConnector(cliConfig.getPrimaryConnectorConfig());
             IdAndVersionStreamConnector secondaryStreamConnector = createConnector(cliConfig.getSecondaryConnectorConfig())){
            idAndVersionStreamVerifier.verify(primaryStreamConnector, secondaryStreamConnector, verifierListener);
        } catch (IOException e){
            LOG.warn("Failed to close connector", e);
        }
    }

    IdAndVersionStreamConnector createConnector(ConnectorConfig connectorConfig) {
        IdAndVersionFactory idAndVersionFactory = createIdAndVersionFactory();
        return connectorConfig.createConnector(idAndVersionFactory);
    }

    IdAndVersionStreamVerifierListener createVerifierListener() {
        if (cliConfig.ignoreTimestampsDuringRun()) {
            return createCoincidentPrintStreamListener();
        } else {
            return createStandardPrintStreamListener();
        }
    }

    private IdAndVersionStreamVerifierListener createStandardPrintStreamListener() {
        return new PrintStreamOutputVersionStreamVerifierListener(System.err, cliConfig.versionsAsTimestamps());
    }

    private IdAndVersionStreamVerifierListener createCoincidentPrintStreamListener() {
        return new CoincidentFilteredStreamVerifierListener(new PrintStreamOutputVersionStreamVerifierListener(System.err, cliConfig.versionsAsTimestamps()));
    }

    private IdAndVersionFactory createIdAndVersionFactory() {
        return cliConfig.numeric() ? LongIdAndVersion.FACTORY : StringIdAndVersion.FACTORY;
    }

}
