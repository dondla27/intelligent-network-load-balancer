package com.network.loadbalancer.controller;
import com.network.loadbalancer.model.BackendServer;
import com.network.loadbalancer.service.BackendServerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;
@RestController
@RequestMapping("/api/lb")
public class LoadBalancerController {
 private final BackendServerService service;
 private final RestTemplate restTemplate=new RestTemplate();
 public LoadBalancerController(BackendServerService service){this.service=service;}

 @GetMapping("/hello")
 public ResponseEntity<?> forward(@RequestParam(defaultValue="round-robin") String strategy){
  List<String> attempted=new ArrayList<>();
  for(int i=0;i<service.getServers().size();i++){
   BackendServer server=service.nextServer(strategy,attempted);
   if(server==null)return ResponseEntity.status(503).body(Map.of("error","No healthy backend servers"));
   attempted.add(server.getName()); server.beginConnection();
   try{
    server.incrementRequests();
    ResponseEntity<String> r=restTemplate.getForEntity(server.getUrl()+"/hello",String.class);
    return ResponseEntity.status(r.getStatusCode()).header("X-Backend-Server",server.getName())
     .header("X-LB-Strategy",strategy).body(r.getBody());
   }catch(Exception e){server.setHealthy(false);}
   finally{server.endConnection();}
  }
  return ResponseEntity.status(502).body(Map.of("error","All backends failed","attempted",attempted));
 }

 @GetMapping("/servers")
 public List<Map<String,Object>> servers(){
  return service.getServers().stream().map(s->{
   Map<String,Object> m=new LinkedHashMap<>();
   m.put("name",s.getName());m.put("url",s.getUrl());m.put("healthy",s.isHealthy());
   m.put("requestCount",s.getRequestCount());m.put("activeConnections",s.getActiveConnections());return m;
  }).toList();
 }

 @GetMapping("/health-check")
 public List<Map<String,Object>> healthCheck(){
  return service.getServers().stream().map(s->{
   boolean ok;try{restTemplate.getForEntity(s.getUrl()+"/health",String.class);ok=true;}catch(Exception e){ok=false;}
   s.setHealthy(ok);return Map.<String,Object>of("server",s.getName(),"healthy",ok);
  }).toList();
 }

 
@PostMapping("/servers/{name}/simulate")
public ResponseEntity<?> simulate(
        @PathVariable String name,
        @RequestParam boolean healthy) {

    BackendServer s = service.findByName(name);

    if (s == null) {
        return ResponseEntity.notFound().build();
    }

    try {
        String url = s.getUrl()
                + "/simulate?healthy=" + healthy;

        restTemplate.getForEntity(url, String.class);

        s.setHealthy(healthy);

        return ResponseEntity.ok(Map.of(
                "server", s.getName(),
                "healthy", s.isHealthy(),
                "message", healthy
                        ? "Backend restored successfully"
                        : "Backend failure simulated successfully"
        ));

    } catch (Exception e) {
        s.setHealthy(false);

        return ResponseEntity.status(502).body(Map.of(
                "server", s.getName(),
                "healthy", false,
                "error", "Unable to connect to backend simulator"
        ));
    }
}
}