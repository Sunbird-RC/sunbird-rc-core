package e2e.registry;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class E2ETests {

    @Test
    void testParallel() {
        System.setProperty("karate.env", System.getenv().getOrDefault("MODE", "sync"));
        Results results = Runner.path("classpath:e2e")
                .tags("~@ignore")
                //.outputCucumberJson(true)
                .parallel(5);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }

}
