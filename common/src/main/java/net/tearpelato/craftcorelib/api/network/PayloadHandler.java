package net.tearpelato.craftcorelib.api.network;

@FunctionalInterface
public interface PayloadHandler<T> {
    void handle(T payload, NetworkContext context);
}
