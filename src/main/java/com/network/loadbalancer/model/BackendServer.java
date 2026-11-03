package com.network.loadbalancer.model;
import java.util.concurrent.atomic.AtomicInteger;
public class BackendServer {
 private final String name, url;
 private final AtomicInteger requestCount=new AtomicInteger(), activeConnections=new AtomicInteger();
 private volatile boolean healthy=true;
 public BackendServer(String name,String url){this.name=name;this.url=url;}
 public String getName(){return name;} public String getUrl(){return url;}
 public int getRequestCount(){return requestCount.get();}
 public int incrementRequests(){return requestCount.incrementAndGet();}
 public int getActiveConnections(){return activeConnections.get();}
 public int beginConnection(){return activeConnections.incrementAndGet();}
 public int endConnection(){return activeConnections.updateAndGet(v->Math.max(0,v-1));}
 public boolean isHealthy(){return healthy;} public void setHealthy(boolean healthy){this.healthy=healthy;}
}
