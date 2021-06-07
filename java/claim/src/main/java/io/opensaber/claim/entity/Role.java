package io.opensaber.claim.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = Role.TABLE_NAME)
public class Role {
    public static final String TABLE_NAME = "roles";
    @Id
    private String name;
    @ManyToMany(targetEntity = Claim.class, mappedBy = "roles")
    private List<Claim> claims;

    public static List<Role> createRoles(List<String> roles) {
        return roles.stream()
                .map(Role::new)
                .collect(Collectors.toList());
    }

    public Role() {
    }

    public Role(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Claim> getClaims() {
        return claims;
    }

    public void setClaims(List<Claim> claims) {
        this.claims = claims;
    }
}
