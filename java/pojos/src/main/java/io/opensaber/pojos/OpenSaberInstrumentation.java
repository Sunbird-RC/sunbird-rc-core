package io.opensaber.pojos;

import org.perf4j.slf4j.Slf4JStopWatch;

public class OpenSaberInstrumentation {
    // private StopWatch sw = new StopWatch();
    private org.perf4j.StopWatch perf4jStopWatch = new Slf4JStopWatch();
    private boolean performanceMonitoingEnabled;
    // private static Logger perfLogger = LoggerFactory.getLogger("PERFORMANCE_INSTRUMENTATION");

    public OpenSaberInstrumentation(boolean performanceMonitoringEnabled) {
        this.performanceMonitoingEnabled = performanceMonitoringEnabled;
    }

    // public OpenSaberInstrumentation(){}

    public void start(String tag) {
        if (performanceMonitoingEnabled) {
            /*
            if (!sw.isRunning()) {
                sw.start(tag);
            }
            */
            perf4jStopWatch.start(tag);
        }
    }

    public void stop(String tag) {
        if (performanceMonitoingEnabled) {
            /*
            if (sw.isRunning()) {
                sw.stop();
                perfLogger.info(sw.prettyPrint());
            }
            */
            perf4jStopWatch.stop(tag);
        }
    }

    /*
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
    */
}
