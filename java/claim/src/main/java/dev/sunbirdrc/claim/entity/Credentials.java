package dev.sunbirdrc.claim.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Credentials {
    @Value("${gcs.api.url}")
    public static String HTTP_LOCALHOST_8080_API_V_1_FILES_DOWNLOAD_FILE_NAME = "http://localhost:8082/api/v1/files/download?fileName=";
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;

    @Column(name = "course",unique=true)
    private String course;

    @Column(name = "credentialName",unique=true)
    private String credentialName;

    @Column(name = "issueDate")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private Date issueDate;

    @Column(name = "credentialURL")
    private String credentialURL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id")
    @JsonIgnore
    private Learner learner;
    @PrePersist
    public void addPrefixAndPostfix() {
        if (credentialURL != null) {
            credentialURL = HTTP_LOCALHOST_8080_API_V_1_FILES_DOWNLOAD_FILE_NAME + credentialURL + ".PDF";
        }
    }

}
