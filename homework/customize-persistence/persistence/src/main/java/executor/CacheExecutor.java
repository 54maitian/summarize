package executor;

import cache.Cache;
import cache.TransactionCacheManager;
import lombok.AllArgsConstructor;
import model.Configuration;
import model.MapperStatement;

import java.util.List;

@AllArgsConstructor
public class CacheExecutor implements Executor {

    private Executor executor;

    private Configuration configuration;


    private TransactionCacheManager cacheManager = new TransactionCacheManager();

    public CacheExecutor(Executor executor, Configuration configuration) {
        this.executor = executor;
        this.configuration = configuration;
    }

    @Override
    public <T> List<T> select(String statementId, Object... params) throws Exception {
        MapperStatement mapperStatement = configuration.getMappedStatementMap().get(statementId);
        Cache cache = mapperStatement.getCache();
        Object object = cacheManager.getObject(cache, statementId);
        if (object != null) {
            return (List<T>)object;
        }
        List<Object> resultList = executor.select(statementId, params);
        cacheManager.putObject(cache, statementId, resultList);
        return (List<T>)resultList;
    }

    @Override
    public int update(String statementId, Object... params) throws Exception {
        return this.executor.update(statementId, params);
    }

    @Override
    public int insert(String statementId, Object... params) throws Exception {
        return this.executor.insert(statementId, params);
    }

    @Override
    public int delete(String statementId, Object... params) throws Exception {
        return this.executor.delete(statementId, params);
    }

    @Override
    public void commit() {
        executor.commit();
        cacheManager.commit();
    }
}
