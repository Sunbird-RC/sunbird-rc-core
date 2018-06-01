package io.opensaber.pojos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

public class OpenSaberInstrumentation {
    private StopWatch sw = new StopWatch();
    private boolean performanceMonitoingEnabled;
    private static Logger perfLogger = LoggerFactory.getLogger("PERFORMANCE_INSTRUMENTATION");

    public OpenSaberInstrumentation(boolean performance_monitoring) {
        this.performanceMonitoingEnabled = performance_monitoring;
    }

    public void start(String str) {
        if (performanceMonitoingEnabled) {
            perfLogger.info(str);
            if (!sw.isRunning()) {
                sw.start(str);
            }
        }
    }

    public void stop() {
        if (performanceMonitoingEnabled) {
            if (sw.isRunning()) {
                sw.stop();
                perfLogger.info(sw.prettyPrint());
            }
        }
    }

    public void prettyPrint() {
        if (performanceMonitoingEnabled) {
            perfLogger.info(sw.prettyPrint());
        }
    }

    public void shortSummary() {
        if (performanceMonitoingEnabled) {
            perfLogger.info(sw.shortSummary());
        }
    }

    public String getTotalTimeMillis(){
        if (performanceMonitoingEnabled) {
            perfLogger.info(Long.toString(sw.getTotalTimeMillis()));
        }
        return Long.toString(sw.getTotalTimeMillis());
    }
}
