package org.globusonline.nexus.utilities;

public class TokenUtilities {
//
//	public String validateToken(token, cache=InMemoryCache(), verify=True):
//
////	    Given a token validate it.
//
////	    Keyword arguments:
////	    :param tokens: A signed authentication token which was provided by Nexus
//
////	    :raises ValueError: If the signature is invalid
//
//	    unencoded_token = urllib.unquote(token)
//	    token_map = {}
//	    for entry in unencoded_token.split('|'):
//	        key, value = entry.split('=')
//	        token_map[key] = value
//	    subject_hash = hashlib.md5(token_map['SigningSubject']).hexdigest()
//	    if not cache.has_public_key(subject_hash):
//	        key_struct = requests.get(token_map['SigningSubject'], verify=verify).content
//	        public_key = json.loads(key_struct)['pubkey']
//	        cache.save_public_key(subject_hash, public_key)
//
//	    public_key = cache.get_public_key(subject_hash)
//	    sig = token_map.pop('sig')
//	    match = re.match('^(.+)\|sig=.*', unencoded_token)
//	    signed_data = match.group(1)
//	    try:
//	        sig = binascii.a2b_hex(sig)
//	        rsa.verify(signed_data, sig, public_key)
//	    except rsa.VerificationError:
//	        exc_value, exc_traceback = sys.exc_info()[1:]
//	        log.debug('RSA Verification error')
//	        log.debug(exc_value)
//	        log.debug(exc_traceback)
//	        raise ValueError('Invalid Signature')
//	    now = time.mktime(datetime.utcnow().timetuple())
//	    if token_map['expiry'] < now:
//	        raise ValueError('TokenExpired')
//	    return token_map['un']
//	
}
