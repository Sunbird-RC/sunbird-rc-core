package io.opensaber.registry.operators;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.pojos.APIMessage;
import io.opensaber.pojos.Request;
import io.opensaber.registry.helper.RegistryHelper;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.util.KeycloakAdminUtil;
import io.opensaber.validators.IValidate;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class InviteOperatorTest {

    @Mock
    private IValidate validationService;

    @Mock
    private KeycloakAdminUtil keycloakAdminUtil;

    @Mock
    private RegistryHelper registryHelper;

    @InjectMocks
    private InviteOperator inviteOperator;

    @Mock
    private APIMessage apiMessage;

    public void testShouldCreateAnInvite() throws Exception {
        when(apiMessage.getRequest()).thenReturn(new Request());
        inviteOperator.execute(apiMessage);
    }
}