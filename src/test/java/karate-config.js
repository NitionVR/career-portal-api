function fn() {
  var env = karate.env || 'default';
  var config = {
    baseUrl: 'http://localhost:' + karate.properties['local.server.port']
  };
  
  if (env === 'docker') {
    config.baseUrl = karate.properties['test.base.url'] || 'http://localhost:8080';
  }
  
  return config;
}
