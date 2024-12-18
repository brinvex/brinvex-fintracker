import com.brinvex.ptfactivity.connector.ibkr.internal.IbkrModuleImpl;

module com.brinvex.ptfactivity.connector.ibkr {
    exports com.brinvex.ptfactivity.connector.ibkr.api;
    exports com.brinvex.ptfactivity.connector.ibkr.api.model;
    exports com.brinvex.ptfactivity.connector.ibkr.api.model.statement;
    exports com.brinvex.ptfactivity.connector.ibkr.api.service;
    requires transitive com.brinvex.ptfactivity.core;
    requires transitive com.brinvex.finance.types;
    requires transitive com.brinvex.java;
    requires transitive com.brinvex.dms;
    requires java.net.http;
    requires java.xml;
    requires transitive org.slf4j;
    provides com.brinvex.ptfactivity.core.api.ModuleFactory with IbkrModuleImpl.IbkrModuleFactory;
}