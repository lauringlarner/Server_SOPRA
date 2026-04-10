package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class ImageAnalysisResult {

    private int found;

    public ImageAnalysisResult(int found) {
        this.found = found;
    }

    public int getFound() {
        return found;
    }

    public void setFound(int found) {
        this.found = found;
    }
}