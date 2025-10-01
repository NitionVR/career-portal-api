function(token) {
  var Base64 = Java.type('java.util.Base64');
  var String = Java.type('java.lang.String');
  var parts = token.split('.');
  if (parts.length !== 3) {
    karate.fail('Invalid JWT token format');
  }
  var decoder = Base64.getUrlDecoder();
  var payload = new String(decoder.decode(parts[1]));
  return JSON.parse(payload);
}
