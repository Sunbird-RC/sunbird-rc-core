package dev.sunbirdrc.claim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileDto {

    private String fileName;
    private String fileUrl;

    private String status;

    public FileDto(String fileName, String fileUrl) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
    }

    public FileDto() {

    }
}
