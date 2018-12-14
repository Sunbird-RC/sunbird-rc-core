package io.opensaber.registry.sink;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component("databaseProviderWrapper")
@Scope(value = WebApplicationContext.SCOPE_REQUEST,
        proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DatabaseProviderWrapper {

    private DatabaseProvider databaseProvider;

    public DatabaseProvider getDatabaseProvider() {
        return databaseProvider;
    }

    public void setDatabaseProvider(DatabaseProvider databaseProvider) {
        this.databaseProvider = databaseProvider;
    }
}
