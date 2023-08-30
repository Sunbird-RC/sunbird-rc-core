package dev.sunbirdrc.registry.digilocker.pulluriresponse;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IssuedTo {

    private Persons persons;

    @XmlElement(name = "persons")
    public Persons getPersons() {
        return persons;
    }

    public void setPersons(Persons persons) {
        this.persons = persons;
    }
    
}
