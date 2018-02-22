package io.opensaber.registry.controller;

import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import gherkin.pickles.Compiler;

/**
 * 
 * @author jyotsna
 *
 */
@ActiveProfiles("dev")
@RunWith(Cucumber.class)
@CucumberOptions(features = "src/test/java/io/opensaber/registry/controller", glue = {"io.opensaber.registry.controller"})
public class RegistryControllerTest {

}
