function fn(token) {
  if (!token) {
    karate.log('ERROR: JWT token is null or undefined');
    karate.fail('JWT token is null or undefined');
  }
  
  karate.log('Token type:', typeof token);
  karate.log('Token value:', token);
  
  // Convert to string if needed
  var tokenStr = token + '';
  
  var parts = tokenStr.split('.');
  if (parts.length !== 3) {
    karate.fail('Invalid JWT format. Expected 3 parts, got ' + parts.length);
  }
  
  try {
    // Decode the payload (second part)
    var payload = parts[1];
    
    // Add padding if needed
    while (payload.length % 4 !== 0) {
      payload += '=';
    }
    
    // Replace URL-safe characters
    payload = payload.replace(/-/g, '+').replace(/_/g, '/');
    
    // Decode using Java's Base64
    var Base64 = Java.type('java.util.Base64');
    var decoder = Base64.getDecoder();
    var decodedBytes = decoder.decode(payload);
    
    // Convert bytes to string
    var String = Java.type('java.lang.String');
    var jsonStr = new String(decodedBytes, 'UTF-8');
    
    karate.log('Decoded JWT payload:', jsonStr);
    
    // Parse and return JSON
    return JSON.parse(jsonStr);
  } catch (e) {
    karate.log('ERROR parsing JWT:', e);
    karate.fail('Failed to parse JWT: ' + e);
  }
}
