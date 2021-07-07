package cache;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class Cache {

    private String id;

    private Map<Object, Object> cache = new HashMap<Object, Object>();

    public void putObject(Object key, Object value) {
        cache.put(key, value);
    }

    public Object getObject(Object key) {
        return cache.get(key);
    }

    public void clear() {
        cache.clear();
    }
}
