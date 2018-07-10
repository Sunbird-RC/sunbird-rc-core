package io.opensaber.registry.client;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.pojos.ResponseSerializer;
import io.opensaber.registry.client.data.RequestData;
import io.opensaber.registry.client.data.ResponseData;
import io.opensaber.registry.exception.TransformationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.api.mockito.PowerMockito;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
public class OpensaberClientTest {

    private Gson gson;

    public OpensaberClientTest() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(ResponseSerializer.class, new ResponseSerializer());
        gson = builder.create();
    }

    @Mock
    private OpensaberClient opensaberClient;
    @Mock
    private RequestData<String> requestData;
    @Mock
    private Map<String, String> headers;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            System.out.println("Executing test: " + description.getMethodName());
        }

        @Override
        protected void succeeded(Description description) {
            System.out.println("Successfully executed test: " + description.getMethodName());
        }

        @Override
        protected void failed(Throwable e, Description description) {
            System.out.println(String.format("Test %s failed. Error message: %s", description.getMethodName(), e.getMessage()));
        }
    };

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAddEntity() throws TransformationException {
        Response apiResponse = new Response(Response.API_ID.CREATE, "OK", new ResponseParams());
        when(opensaberClient.addEntity(requestData, headers)).thenReturn(new ResponseData<>(gson.toJson(apiResponse)));
        Response response = gson.fromJson(opensaberClient.addEntity(requestData, headers).getResponseData(), Response.class);
        assertEquals(response.getResponseCode(), "OK");
        assertEquals(response.getId(), Response.API_ID.CREATE.getId());
    }

    @Test
    public void testAddAndAssociateEntity() throws UnsupportedEncodingException, TransformationException {
        Response apiResponse = new Response(Response.API_ID.CREATE, "OK", new ResponseParams());
        URI entityUri = PowerMockito.mock(URI.class);
        URI propertyPredicateUri = PowerMockito.mock(URI.class);
        when(opensaberClient.addAndAssociateEntity(entityUri, propertyPredicateUri, requestData, headers))
                .thenReturn(new ResponseData<>(gson.toJson(apiResponse)));
        Response response = gson.fromJson(opensaberClient.addAndAssociateEntity(entityUri,
                propertyPredicateUri, requestData, headers).getResponseData(), Response.class);
        assertEquals(response.getResponseCode(), "OK");
        assertEquals(response.getId(), Response.API_ID.CREATE.getId());
    }

    @Test
    public void testUpdateEntity() throws TransformationException {
        Response apiResponse = new Response(Response.API_ID.UPDATE, "OK", new ResponseParams());
        when(opensaberClient.updateEntity(requestData, headers)).thenReturn(new ResponseData<>(gson.toJson(apiResponse)));
        Response response = gson.fromJson(opensaberClient.updateEntity(requestData, headers).getResponseData(), Response.class);
        assertEquals(response.getResponseCode(), "OK");
        assertEquals(response.getId(), Response.API_ID.UPDATE.getId());
    }

    @Test
    public void testReadEntity() throws TransformationException {
        Response apiResponse = new Response(Response.API_ID.READ, "OK", new ResponseParams());
        URI entityUri = PowerMockito.mock(URI.class);
        when(opensaberClient.readEntity(entityUri, headers)).thenReturn(new ResponseData<>(gson.toJson(apiResponse)));
        Response response = gson.fromJson(opensaberClient.readEntity(entityUri, headers).getResponseData(), Response.class);
        assertEquals(response.getResponseCode(), "OK");
        assertEquals(response.getId(), Response.API_ID.READ.getId());
    }

    @Test
    public void testDeleteEntity() {
        Response apiResponse = new Response(Response.API_ID.DELETE, "OK", new ResponseParams());
        URI propertyPredicateUri = PowerMockito.mock(URI.class);
        when(opensaberClient.deleteEntity(propertyPredicateUri, headers)).thenReturn(new ResponseData<>(gson.toJson(apiResponse)));
        Response response = gson.fromJson(opensaberClient.deleteEntity(propertyPredicateUri, headers).getResponseData(), Response.class);
        assertEquals(response.getResponseCode(), "OK");
        assertEquals(response.getId(), Response.API_ID.DELETE.getId());
    }
}
