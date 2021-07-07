package cache;

import java.util.HashMap;
import java.util.Map;

public class TransactionCacheManager {

    private Map<Cache, TransactionalCache> transactionalCaches = new HashMap<>();

    public Object getObject(Cache cache, Object key){
        return geTransactionalCache(cache).getObject(key);
    }

    public void putObject(Cache cache, Object key, Object value){
        geTransactionalCache(cache).putObject(key, value);
    };

    public void commit() {
        for (TransactionalCache txCache : transactionalCaches.values()) {
            txCache.commit();
        }
    }

    public TransactionalCache geTransactionalCache(Cache cache) {
        TransactionalCache txCache = transactionalCaches.get(cache);
        if(txCache == null) {
            txCache = new TransactionalCache(cache);
            transactionalCaches.put(cache, txCache);
        }
        return txCache;
    }

}
