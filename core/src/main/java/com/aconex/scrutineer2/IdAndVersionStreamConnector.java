package com.aconex.scrutineer2;

import java.io.Closeable;

/**
 * Connects to a provider (e.g: jdbc, elasticsearch) to create a Stream
 */
public interface IdAndVersionStreamConnector extends Closeable {
    void open();
    IdAndVersionStream stream();
}
