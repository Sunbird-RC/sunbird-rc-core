package io.opensaber.registry.service;

import io.opensaber.registry.exception.SignatureException;

import java.util.Map;

public interface ISignatureService {

    public Map<String,Object> sign(Object propertyValue) throws SignatureException;

    public Map<String,Object> verify(Map<String,Object> propertyValue) throws SignatureException;

    public boolean isServiceUp();

}
