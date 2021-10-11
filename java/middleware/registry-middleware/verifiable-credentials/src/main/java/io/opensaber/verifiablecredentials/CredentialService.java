package io.opensaber.verifiablecredentials;

import com.danubetech.keyformats.crypto.provider.Ed25519Provider;
import com.danubetech.keyformats.crypto.provider.impl.TinkEd25519Provider;
import foundation.identity.jsonld.JsonLDException;
import foundation.identity.jsonld.JsonLDObject;
import foundation.identity.jsonld.JsonLDUtils;
import info.weboftrust.ldsignatures.LdProof;
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts;
import info.weboftrust.ldsignatures.signer.Ed25519Signature2018LdSigner;
import info.weboftrust.ldsignatures.verifier.Ed25519Signature2018LdVerifier;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.Date;

public class CredentialService {
    private static final Logger logger = LoggerFactory.getLogger(CredentialService.class);
    private static final String ASSERTION_PROOF_PURPOSE = "AssertionProofPurpose";
    private static final String DATE_FORMAT = "2017-10-24T05:33:31Z";
    private byte[] privateKey, publicKey;

    private String domain, creator, nonce;

    public CredentialService(String privateKey, String publicKey, String domain, String creator, String nonce) {
        try {
            Ed25519Provider.set(new TinkEd25519Provider());
            this.privateKey = Hex.decodeHex(privateKey.toCharArray());
            this.publicKey = Hex.decodeHex(publicKey.toCharArray());
            this.domain = domain;
            this.creator = creator;
            this.nonce = nonce;
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }


    public LdProof sign(String data) throws IOException, JsonLDException, GeneralSecurityException, ParseException {
        JsonLDObject jsonLdObject = JsonLDObject.fromJson(data);
        jsonLdObject.setDocumentLoader(LDSecurityContexts.DOCUMENT_LOADER);
        URI creator = URI.create(this.creator);
        Date created = JsonLDUtils.DATE_FORMAT.parse(DATE_FORMAT);
        Ed25519Signature2018LdSigner signer = new Ed25519Signature2018LdSigner(privateKey);
        signer.setCreator(creator);
        signer.setCreated(created);
        signer.setDomain(domain);
        signer.setProofPurpose(ASSERTION_PROOF_PURPOSE);
        signer.setNonce(nonce);
        LdProof ldProof = signer.sign(jsonLdObject);
        logger.debug("LdProof: {}", ldProof);
        return ldProof;
    }

    public boolean verify(String data, LdProof ldProof) throws JsonLDException, GeneralSecurityException, IOException {
        JsonLDObject jsonLdObject = JsonLDObject.fromJson(data);
        jsonLdObject.setDocumentLoader(LDSecurityContexts.DOCUMENT_LOADER);
        Ed25519Signature2018LdVerifier verifier = new Ed25519Signature2018LdVerifier(publicKey);
        boolean verify = verifier.verify(jsonLdObject, ldProof);
        return verify;
    }
}
