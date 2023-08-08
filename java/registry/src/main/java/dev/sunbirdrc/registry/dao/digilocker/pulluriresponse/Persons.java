package dev.sunbirdrc.registry.dao.digilocker.pulluriresponse;


import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
@XmlRootElement
public class Persons {

    public List<Person> getPerson() {
        return person;
    }


    public void setPerson(List<Person> person) {
        this.person = person;
    }

    List<Person> person = new ArrayList<>();

}
