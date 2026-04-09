package ch.uzh.ifi.hase.soprafs26;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;

import java.io.FileInputStream;
import java.util.List;



public class VisionQuickstart {

    public static void analyzeimage(String filePath, String object) throws Exception {

        // 🔁 Put your image path here

        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {

            ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

            Image img = Image.newBuilder().setContent(imgBytes).build();

            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.LABEL_DETECTION)
                    .build();

            AnnotateImageRequest request =
                    AnnotateImageRequest.newBuilder()
                            .addFeatures(feature)
                            .setImage(img)
                            .build();

            BatchAnnotateImagesResponse response =
                    vision.batchAnnotateImages(List.of(request));

            boolean foundBicycle = false;

            for (AnnotateImageResponse res : response.getResponsesList()) {

                if (res.hasError()) {
                    System.out.println("Error: " + res.getError().getMessage());
                    return;
                }

                System.out.println("Labels:");

                for (EntityAnnotation label : res.getLabelAnnotationsList()) {
                    System.out.printf(" - %s (%.2f)%n",
                            label.getDescription(),
                            label.getScore());

                    if (label.getDescription().equalsIgnoreCase(object)) {
                        foundBicycle = true;
                    }
                }
            }

            System.out.println("\nResult: " +
                    (foundBicycle ? "✅ Object detected" : "❌ Object not in picture"));

        }
    }

    public static void main(String[] args)throws Exception{
        String file = args[0];
        String word = args [1];
    
        analyzeimage(file, word);

    }
}

//./gradlew run --args="/'Users/laurinprivate/Desktop/WhatsApp Image 2026-03-23 at 14.54.36.jpeg' guardrail"
//./gradlew run --args="'/Users/laurinprivate/Desktop/WhatsApp Image 2026-03-23 at 13.36.41.jpeg' bottle"

//./gradlew run --args="'/Users/laurinprivate/Desktop/WhatsApp Image 2026-03-23 at 17.06.02.jpeg' TV"
//./gradlew run --args="'/Users/laurinprivate/Desktop/WhatsApp Image 2026-03-23 at 17.08.40.jpeg' Ski"
//./gradlew run --args="'/Users/laurinprivate/Desktop/C2kxjwBoKSFAx1yUsCyzV-.jpg' sun"

//rest specification file in the backend