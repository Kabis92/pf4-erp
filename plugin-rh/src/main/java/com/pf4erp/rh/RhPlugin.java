package com.pf4erp.rh;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class RhPlugin extends Plugin {

    public RhPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        // Runtime wiring is handled by erp-core (child Spring context).
    }

    @Override
    public void stop() {
        // no-op
    }
}
