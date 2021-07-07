package cache;

import java.util.HashMap;
import java.util.Map;

public class TransactionalCache {
    private Cache delegate;
    private Map<Object, Object> entriesToAddOnCommit = new HashMap<>();

    public TransactionalCache(Cache delegate) {
        this.delegate = delegate;
    }

    public Object getObject(Object key){
        return delegate.getObject(key);
    }

    public void putObject(Object key, Object value) {
        entriesToAddOnCommit.put(key, value);
    }

    public void commit() {
        entriesToAddOnCommit.forEach((key, value) -> {
            delegate.putObject(key, value);
        });
    }
}
