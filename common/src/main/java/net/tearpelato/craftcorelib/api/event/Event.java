package net.tearpelato.craftcorelib.api.event;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class Event<T> {

    private final List<T> listeners = new ArrayList<>();
    private final Class<T> type;

    public Event(Class<T> type) {
        this.type = type;
    }

    public void register(T listener) {
        listeners.add(listener);
    }


    @SuppressWarnings("unchecked")
    public T post() {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    for (T listener : listeners) {
                        method.invoke(listener, args);
                    }
                    return null;
                }
        );
    }
}