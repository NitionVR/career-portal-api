function fn() {
  var env = karate.env || 'test';
  
  // Get the server port from system property
  var port = karate.properties['karate.server.port'];
  
  // If not set, try common defaults
  if (!port) {
    karate.log('WARNING: karate.server.port not set, using default 8080');
    port = '8080';
  }
  
  var config = {
    baseUrl: 'http://localhost:' + port
  };
  
  // Override for docker environment
  if (env === 'docker') {
    config.baseUrl = karate.properties['test.base.url'] || 'http://host.docker.internal:8080';
  }
  
  karate.log('Karate environment:', env);
  karate.log('Base URL:', config.baseUrl);
  
  return config;
}