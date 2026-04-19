package ch.uzh.ifi.hase.soprafs26;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

            String[] objectWords = object.toLowerCase().replaceAll("[^a-z ]", "").split("\\s+");
            boolean allMatch = true;
            for (String word : objectWords) {
                if (word.length() <= 2) continue;
                boolean found = allWords.stream().anyMatch(w -> w.contains(word));
                if (!found) {
                    allMatch = false;
                    break;
                }
            }

            if (allMatch) return 1;
        }

        return 0;
    }
}
