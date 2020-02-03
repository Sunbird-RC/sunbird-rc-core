package io.opensaber.registry.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.service.DecryptionHelper;
import io.opensaber.registry.service.IReadService;
import io.opensaber.registry.service.ISearchService;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.sink.shard.ShardManager;
import io.opensaber.registry.util.ViewTemplateManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
@SpringBootTest(classes = { ObjectMapper.class})
public class RegistryHelperTest {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@InjectMocks
	private RegistryHelper registerHelper;

	@Autowired
	private ObjectMapper objectMapper;

	@Mock
	private ObjectMapper objectMapperMock;

	@Mock
	private ISearchService searchService;
	
	@Mock
	private ViewTemplateManager viewTemplateManager;
	
	@Mock
    private ShardManager shardManager;

	@Mock
    RegistryService registryService;

	@Mock
    IReadService readService;

    @Mock
    private DBConnectionInfoMgr dbConnectionInfoMgr;

    @Mock
    private DecryptionHelper decryptionHelper;

	@Before
    public void initMocks(){
		ReflectionTestUtils.setField(registerHelper, "auditSuffix", "Audit");
		ReflectionTestUtils.setField(registerHelper, "auditSuffixSeparator", "_");
        MockitoAnnotations.initMocks(this);
    }
	
	@Test
	public void getAuditLogTest() throws Exception {

		// Data creation
		String inputJson = "{\"Teacher\":{ \"filters\":{ \"recordId\":{\"eq\":\"12c61cc3-cc6a-4a96-8409-e506fb26ddbb\"} } } }";

		String result = "{ \"Teacher_Audit\": [{ \"auditId\": \"66fecb96-b87c-44b5-a930-3de96503aa13\", \"recordId\": \"12c61cc3-cc6a-4a96-8409-e506fb26ddbb\","
				+ " \"timeStamp\": \"2019-12-23 16:56:50.905\", \"date\": 1578566074000, \"@type\": \"Teacher_Audit\", \"action\": \"ADD\", "
				+ "\"auditJson\": [ \"op\", \"path\" ], \"osid\": \"1-d28fd315-bc28-4db0-b7f8-130ff164ba01\", \"userId\": \"35448199-0a7b-4491-a796-b053b9fcfd29\","
				+ " \"transactionId\": [ 870924631 ] }] }";

		JsonNode jsonNode = null;
		JsonNode resultNode = null;
		jsonNode = objectMapper.readTree(inputJson);
		resultNode = objectMapper.readTree(result);
		
        Mockito.when(objectMapperMock.createArrayNode()).thenReturn(objectMapper.createArrayNode());
		Mockito.when(searchService.search(ArgumentMatchers.any())).thenReturn(resultNode);
		Mockito.when(viewTemplateManager.getViewTemplate(ArgumentMatchers.any())).thenReturn(null);
		
		JsonNode node = registerHelper.getAuditLog(jsonNode);
		Assert.assertEquals(jsonNode.get("Teacher").get("filters").get("recordId").get("eq"), node.get("Teacher_Audit").get(0).get("recordId"));
	}




}