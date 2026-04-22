package ch.uzh.ifi.hase.soprafs26;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.LocalizedObjectAnnotation;
import com.google.cloud.vision.v1.WebDetection;
import com.google.protobuf.ByteString;



public class VisionQuickstartObjectLocalization {

    private static final float THRESHOLD = 0.5f;
    private static ImageAnnotatorClient vision;

    static {
        try {
            InputStream stream = VisionQuickstartObjectLocalization.class
                    .getClassLoader()
                    .getResourceAsStream("sopra-fs26-group-18-server-254a2c84c9e1.json");
            GoogleCredentials credentials = (stream != null
                    ? GoogleCredentials.fromStream(stream)
                    : GoogleCredentials.getApplicationDefault())
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();
            vision = ImageAnnotatorClient.create(settings);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Vision client", e);
        }
    }

    public static int analyzeimage(byte[] imageBytes, String object) throws Exception {

        ByteString imgBytes = ByteString.copyFrom(imageBytes);
        Image img = Image.newBuilder().setContent(imgBytes).build();

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).setMaxResults(50))
                .addFeatures(Feature.newBuilder().setType(Feature.Type.OBJECT_LOCALIZATION).setMaxResults(50))
                .addFeatures(Feature.newBuilder().setType(Feature.Type.WEB_DETECTION).setMaxResults(50))
                .setImage(img)
                .build();

        BatchAnnotateImagesResponse response = vision.batchAnnotateImages(List.of(request));

        for (AnnotateImageResponse res : response.getResponsesList()) {
            if (res.hasError()) return 0;

            List<String> allWords = new ArrayList<>();

            // LABEL_DETECTION
            for (EntityAnnotation label : res.getLabelAnnotationsList()) {
                System.out.println("Label: " + label.getDescription() + " score=" + label.getScore());
                if (label.getScore() >= THRESHOLD) {
                    Arrays.stream(label.getDescription().toLowerCase().split("\\s+"))
                            .filter(w -> w.length() > 2)
                            .forEach(allWords::add);
                }
            }
            // web detection
            WebDetection web = res.getWebDetection();
                for (WebDetection.WebEntity entity : web.getWebEntitiesList()) {
                    if (entity.getScore() >= THRESHOLD && !entity.getDescription().isEmpty()) {
                        Arrays.stream(entity.getDescription().toLowerCase().split("\\s+"))
                            .filter(w -> w.length() > 2)
                            .forEach(allWords::add);
    }
}

            // OBJECT_LOCALIZATION
            for (LocalizedObjectAnnotation loc : res.getLocalizedObjectAnnotationsList()) {
                System.out.println("Object: " + loc.getName() + " score=" + loc.getScore());
                if (loc.getScore() >= THRESHOLD) {
                    Arrays.stream(loc.getName().toLowerCase().split("\\s+"))
                            .filter(w -> w.length() > 2)
                            .forEach(allWords::add);
                }
            }

            System.out.println("Word list: " + allWords);

             List<String> acceptedTerms = SynonymMap.getAcceptedTerms(object);
            System.out.println("Accepted terms for '" + object + "': " + acceptedTerms);

            for (String term : acceptedTerms) {
                String[] termWords = term.replaceAll("[^a-z ]", "").split("\\s+");
                boolean allMatch = true;
                for (String word : termWords) {
                    if (word.length() <= 2) continue;
                    boolean found = allWords.stream().anyMatch(w -> w.contains(word));
                    if (!found) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) {
                    System.out.println("Matched via term: " + term);
                    return 1;
                }
            }
        }

        return 0;
    }
}
