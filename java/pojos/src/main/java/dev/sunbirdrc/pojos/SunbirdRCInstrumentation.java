package dev.sunbirdrc.pojos;

import org.perf4j.slf4j.Slf4JStopWatch;

public class SunbirdRCInstrumentation {
	private org.perf4j.StopWatch perf4jStopWatch = new Slf4JStopWatch();
	private boolean performanceMonitoingEnabled;

	public SunbirdRCInstrumentation(boolean performanceMonitoringEnabled) {
		this.performanceMonitoingEnabled = performanceMonitoringEnabled;
	}

	public void start(String tag) {
		if (performanceMonitoingEnabled) {
			perf4jStopWatch.start(tag);
		}
	}

	public void stop(String tag) {
		if (performanceMonitoingEnabled) {
			perf4jStopWatch.stop(tag);
		}
	}
}
