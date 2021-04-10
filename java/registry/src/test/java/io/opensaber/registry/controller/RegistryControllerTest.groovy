package io.opensaber.registry.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.util.ReflectionUtils

import java.lang.reflect.Field

class RegistryControllerTest extends GroovyTestCase {
    static void testPopulateSchema() {
        def controller = new RegistryController()
        def field = RegistryController.getDeclaredField("objectMapper")
        field.setAccessible(true);
        ReflectionUtils.setField(field, controller, new ObjectMapper())
        def actions = controller.populateEntityActions("Vehicle")
        println(actions)
    }
}
