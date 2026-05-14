function fn() {
  var env = karate.env; // get system property 'karate.env'
  karate.log('karate.env system property was:', env);
  if (!env) {
    env = 'dev';
  }
  var config = {
    env: env,
    baseurl: 'http://localhost:8081'
  }
  // CI can be slow with 16+ containers running; give services 90s to respond
  karate.configure('connectTimeout', 10000);
  karate.configure('readTimeout', 90000);
  if (env == 'dev') {
    // customize
    // e.g. config.foo = 'bar';
  } else if (env == 'e2e') {
    // customize
  }
  return config;
}