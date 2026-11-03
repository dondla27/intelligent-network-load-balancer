package com.network.loadbalancer.service;
import com.network.loadbalancer.model.BackendServer;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
@Service
public class BackendServerService {
 private final List<BackendServer> servers=List.of(
  new BackendServer("Backend-1","http://localhost:8081"),
  new BackendServer("Backend-2","http://localhost:8082"),
  new BackendServer("Backend-3","http://localhost:8083"));
 private final AtomicInteger cursor=new AtomicInteger();
 public List<BackendServer> getServers(){return servers;}
 public BackendServer nextServer(String strategy,List<String> excluded){
  List<BackendServer> available=servers.stream().filter(BackendServer::isHealthy)
   .filter(s->!excluded.contains(s.getName())).toList();
  if(available.isEmpty()) return null;
  if("least-connections".equalsIgnoreCase(strategy))
   return available.stream().min(Comparator.comparingInt(BackendServer::getActiveConnections)
    .thenComparingInt(BackendServer::getRequestCount)).orElse(null);
  return available.get(Math.floorMod(cursor.getAndIncrement(),available.size()));
 }
 public BackendServer findByName(String name){
  return servers.stream().filter(s->s.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
 }
}
