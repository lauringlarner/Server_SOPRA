package ch.uzh.ifi.hase.soprafs26;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.LocalizedObjectAnnotation;
import com.google.cloud.vision.v1.WebDetection;
import com.google.rpc.Status;
import org.junit.jupiter.api.Test;

public class VisionQuickstartObjectLocalizationTest {

    @Test
    public void responseMatchesObject_matchesAcceptedLabelSynonym() {
        AnnotateImageResponse response = AnnotateImageResponse.newBuilder()
                .addLabelAnnotations(EntityAnnotation.newBuilder()
                        .setDescription("Automobile")
                        .setScore(0.9f))
                .build();

        assertTrue(VisionQuickstartObjectLocalization.responseMatchesObject(response, "car"));
    }

    @Test
    public void responseMatchesObject_matchesWebDetectionMultiWordSynonym() {
        AnnotateImageResponse response = AnnotateImageResponse.newBuilder()
                .setWebDetection(WebDetection.newBuilder()
                        .addWebEntities(WebDetection.WebEntity.newBuilder()
                                .setDescription("fire engine")
                                .setScore(0.9f)))
                .build();

        assertTrue(VisionQuickstartObjectLocalization.responseMatchesObject(response, "fire truck"));
    }

    @Test
    public void responseMatchesObject_matchesLocalizedObjectSynonym() {
        AnnotateImageResponse response = AnnotateImageResponse.newBuilder()
                .addLocalizedObjectAnnotations(LocalizedObjectAnnotation.newBuilder()
                        .setName("Road Bike")
                        .setScore(0.9f))
                .build();

        assertTrue(VisionQuickstartObjectLocalization.responseMatchesObject(response, "bicycle"));
    }

    @Test
    public void responseMatchesObject_ignoresLowConfidenceDetection() {
        AnnotateImageResponse response = AnnotateImageResponse.newBuilder()
                .addLabelAnnotations(EntityAnnotation.newBuilder()
                        .setDescription("Automobile")
                        .setScore(0.49f))
                .build();

        assertFalse(VisionQuickstartObjectLocalization.responseMatchesObject(response, "car"));
    }

    @Test
    public void responseMatchesObject_returnsFalseForVisionError() {
        AnnotateImageResponse response = AnnotateImageResponse.newBuilder()
                .setError(Status.newBuilder()
                        .setMessage("Vision API failed"))
                .build();

        assertFalse(VisionQuickstartObjectLocalization.responseMatchesObject(response, "car"));
    }
}
