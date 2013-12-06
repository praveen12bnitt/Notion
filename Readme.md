
# Create a pool
curl -X POST -H "Content-Type: application/json" -d '{"name":"default","applicationEntityTitle" : "default", "description":"bar"}' http://localhost:11118/rest/pool

# Create a device
curl -X POST -H "Content-Type: application/json" -d '{"applicationEntityTitle" : ".*", "hostName":".*", "port": 11117}' http://localhost:11118/rest/pool/1/device

### A loopback destination
curl -X POST -H "Content-Type: application/json" -d '{"applicationEntityTitle" : "destination", "hostName":"localhost", "port": 11117}' http://localhost:11118/rest/pool/1/device


###  Second pool
curl -X POST -H "Content-Type: application/json" -d '{"name":"default","applicationEntityTitle" : "destination", "description":"bar"}' http://localhost:11118/rest/pool

# Create a device
curl -X POST -H "Content-Type: application/json" -d '{"applicationEntityTitle" : ".*", "hostName":".*", "port": 11117}' http://localhost:11118/rest/pool/2/device




### Send some data
./dcm4che-2.0.28/bin/dcmsnd default@localhost:11117 DICOMTestData/


### Query?
./dcm4che-2.0.28/bin/dcmqr default@localhost:11117

### Move some data
./dcm4che-2.0.28/bin/dcmqr default@localhost:11117 -cmove destination -q StudyInstanceUID=1.2.40.0.13.1.1.172.22.2.90.20080613130257614.49733

### Query the other pool
./dcm4che-2.0.28/bin/dcmqr destination@localhost:11117
