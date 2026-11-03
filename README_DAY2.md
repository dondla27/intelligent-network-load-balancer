Day 2 features:
- Round Robin: GET http://localhost:8080/api/lb/hello
- Least Connections: GET http://localhost:8080/api/lb/hello?strategy=least-connections
- Stats: GET http://localhost:8080/api/lb/servers
- Health checks: GET http://localhost:8080/api/lb/health-check

Simulate Backend-2 down (PowerShell):
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/lb/servers/Backend-2/simulate?healthy=false"

Then call /api/lb/hello; it should route to Backend-1 or Backend-3.
Restore Backend-2:
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/lb/servers/Backend-2/simulate?healthy=true"

Educational prototype only; protect administrative endpoints before any real deployment.
