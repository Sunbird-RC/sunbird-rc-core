package io.opensaber.registry.test;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

/**
 * 
 * @author jyotsna
 *
 */

@RunWith(Cucumber.class)
@CucumberOptions(features = "src/test/java/io/opensaber/registry/test", glue = {"io.opensaber.registry.test"})
public class RegistryIntegrationTest {

}
