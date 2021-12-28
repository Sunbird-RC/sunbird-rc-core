package dev.sunbirdrc.registry.test;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 *
 * @author jyotsna
 *
 */

@RunWith(Cucumber.class)
@CucumberOptions(plugin = { "pretty", "html:target/cucumber-results/integration-test-reports",
		"json:target/cucumber-results/registry_it_report.json",
		"junit:target/cucumber-results/registry_it_report.xml" }, tags = {
				"@create or @read or @update or @delete or @search" }, features = "src/test/java/io/sunbirdrc/registry/test", glue = {
						"dev.sunbirdrc.registry.test" })
public class RegistryIntegrationTest {

}
