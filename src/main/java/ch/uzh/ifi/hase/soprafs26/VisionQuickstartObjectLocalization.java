package ch.uzh.ifi.hase.soprafs26;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;

import java.io.InputStream;
import java.util.List;

public class VisionQuickstartObjectLocalization {

    public static int analyzeimage(byte[] imageBytes, String object) throws Exception {

        float THRESHOLD = 0.6f;

        // Load credentials from resources folder
        InputStream credentialsStream = VisionQuickstartObjectLocalization.class
                .getClassLoader()
                .getResourceAsStream("google-credentials.json");

        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped("https://www.googleapis.com/auth/cloud-platform");

        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();

        ByteString imgBytes = ByteString.copyFrom(imageBytes);

        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create(settings)) {

            Image img = Image.newBuilder().setContent(imgBytes).build();

            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.OBJECT_LOCALIZATION)
                    .build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(img)
                    .build();

            BatchAnnotateImagesResponse response =
                    vision.batchAnnotateImages(List.of(request));

            for (AnnotateImageResponse res : response.getResponsesList()) {
                if (res.hasError()) return 0;

                for (LocalizedObjectAnnotation label : res.getLocalizedObjectAnnotationsList()) {
                    if (label.getName().equalsIgnoreCase(object)
                            && label.getScore() >= THRESHOLD) {
                        return 1;
                    }
                }
            }
        }

        return 0;
    }
}