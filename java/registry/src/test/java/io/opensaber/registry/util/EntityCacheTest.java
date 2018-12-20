package io.opensaber.registry.util;

import static org.junit.Assert.assertEquals;

import io.opensaber.registry.middleware.util.Constants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { EntityCacheManager.class, ConfigurationTest.class })
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class EntityCacheTest {

	@Mock
	EntityCacheManager entityCacheManager;

	@InjectMocks
	EntityCache entityCache;

	private Map<String, List<String>> shardUUidsMapMock;

	@Before
	public void setUp() {
		List<String> mockUUIDForShard1 = new ArrayList<>();
		mockUUIDForShard1.add("UUID1");
		mockUUIDForShard1.add("UUID2");
		List<String> mockUUIDForShard2 = new ArrayList<>();
		mockUUIDForShard2.add("UUID3");
		mockUUIDForShard2.add("UUID4");
		shardUUidsMapMock = new ConcurrentHashMap<>();
		shardUUidsMapMock.put("Shard1", mockUUIDForShard1);
		shardUUidsMapMock.put("Shard2", mockUUIDForShard2);

		ReflectionTestUtils.setField(entityCache, "recordShardMap", shardUUidsMapMock);
		MockitoAnnotations.initMocks(this);
	}

	@Test(expected = IOException.class)
	public void testGetShardWithEmptyRecordValue() throws IOException {
		entityCache.getShard("");

	}

	@Test(expected = IOException.class)
	public void testGetShardWithNullRecordValue() throws IOException {
		entityCache.getShard(null);
	}

	@Test(expected = IOException.class)
	public void testGetShardForRecordValueNotPresent() throws IOException {
		entityCache.getShard("UUID_NOT_PRESENT");

	}

	@Test
	public void testGetShardOneforPresentRecord() throws IOException {
		assertEquals("Shard1", entityCache.getShard("UUID1"));
	}
	
	@Test
	public void testGetShardTwoforPresentRecord() throws IOException {
		assertEquals("Shard2", entityCache.getShard("UUID3"));
	}

}
