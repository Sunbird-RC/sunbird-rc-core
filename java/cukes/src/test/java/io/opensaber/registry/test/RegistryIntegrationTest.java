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
@CucumberOptions(
    plugin = {
            "pretty",
            "html:target/cucumber-results/integration-test-reports",
            "json:target/cucumber-results/registry_it_report.json",
            "junit:target/cucumber-results/registry_it_report.xml"
    },
    tags = {"@create or @read or @update or @delete or @search"},
    features = "src/test/java/io/opensaber/registry/test",
    glue = {"io.opensaber.registry.test"})
public class RegistryIntegrationTest {

}
