package dev.sunbirdrc.registry.middleware.util;

import org.junit.Assert;
import org.junit.Test;

public class DidTest {

	@Test
	public void testDidUrlParse() throws Exception {
		Did did = Did.parse("did:url:https://www.google.com/pdf");
		Assert.assertEquals("url", did.getMethod());
		Assert.assertEquals("https://www.google.com/pdf", did.getMethodIdentifier());
	}

	@Test
	public void testDidPathParse() throws Exception {
		Did did = Did.parse("did:path:/User/osid");
		Assert.assertEquals("path", did.getMethod());
		Assert.assertEquals("/User/osid", did.getMethodIdentifier());
	}

	@Test(expected = Exception.class)
	public void testDidWrongParse() throws Exception {
		Did did = Did.parse("did:path");
	}

}