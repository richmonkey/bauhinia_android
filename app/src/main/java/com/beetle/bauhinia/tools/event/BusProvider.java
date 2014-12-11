package com.beetle.bauhinia.tools.event;

import com.squareup.otto.Bus;

/**
 * Created by tsung on 3/31/14.
 */
public class BusProvider {
    private static final Bus BUS = new Bus();

    public static Bus getInstance() {
        return BUS;
    }

    private BusProvider() {
        // No instances.
    }
}
