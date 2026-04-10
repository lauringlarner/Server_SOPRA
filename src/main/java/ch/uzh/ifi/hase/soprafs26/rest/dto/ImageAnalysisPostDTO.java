package ch.uzh.ifi.hase.soprafs26.rest.dto;

import org.springframework.web.multipart.MultipartFile;

public class ImageAnalysisPostDTO {

    private MultipartFile image;
    private String object;

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }
}
