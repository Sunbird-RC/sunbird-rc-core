package io.opensaber.registry.service;

import io.opensaber.registry.exception.SignatureException;
import java.util.Map;

public interface SignatureService {

    public Object sign(Object propertyValue) throws SignatureException;

    public Object verify(Object propertyValue) throws SignatureException;

    public boolean isServiceUp();

}
