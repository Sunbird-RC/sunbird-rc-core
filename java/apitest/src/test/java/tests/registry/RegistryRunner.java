package examples.users;

import com.intuit.karate.junit5.Karate;

class RegistryRunner {
    
    @Karate.Test
    Karate testUsers() {
        return Karate.run("registry").relativeTo(getClass());
    }    

}
