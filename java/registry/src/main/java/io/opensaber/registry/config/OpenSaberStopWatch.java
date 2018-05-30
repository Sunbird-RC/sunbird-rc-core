package io.opensaber.registry.config;

import org.springframework.util.StopWatch;

public class OpenSaberStopWatch {
    private StopWatch sw = new StopWatch();

    public void start(String str) {
         if(!sw.isRunning()) {
            sw.start(str);
        }
    }

    public void stop() {
        if(sw.isRunning()) {
            sw.stop();
        }
    }

    public String prettyPrint(){
        return sw.prettyPrint();
    }
    public String shortSummary(){
       return sw.shortSummary();
    }
    public long getTotalTimeMillis(){
        return sw.getTotalTimeMillis();
    }
}
