package ch.uzh.ifi.hase.soprafs26;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;

import java.io.FileInputStream;
import java.util.List;



public class VisionQuickstart_arda {

    public static void analyzeimage(String filePath, String object) throws Exception {

        // 🔁 Put your image path here

        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {

            ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

            Image img = Image.newBuilder().setContent(imgBytes).build();

            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.OBJECT_LOCALIZATION)
                    .build();

            AnnotateImageRequest request =
                    AnnotateImageRequest.newBuilder()
                            .addFeatures(feature)
                            .setImage(img)
                            .build();

            BatchAnnotateImagesResponse response =
                    vision.batchAnnotateImages(List.of(request));

            boolean found = false;
            float matchScore = 0f;
            float THRESHOLD = 0.90f;

            for (AnnotateImageResponse res : response.getResponsesList()) {

                if (res.hasError()) {
                    System.out.println("Error: " + res.getError().getMessage());
                    return;
                }

                System.out.println("Objects:");

                for (LocalizedObjectAnnotation label : res.getLocalizedObjectAnnotationsList()) {
                    System.out.printf(" - %s (%.2f)%n",
                            label.getName(),
                            label.getScore());

                    if (label.getName().equalsIgnoreCase(object)) {
                        matchScore = label.getScore();
                        if (matchScore >= THRESHOLD) {
                            found = true;
                        }
                    }
                }
            }

            if (matchScore > 0) {
                System.out.printf("%nFound \"%s\" with score %.2f — %s (threshold: %.2f)%n",
                        object, matchScore,
                        found ? "DETECTED" : "SCORE TOO LOW",
                        THRESHOLD);
            } else {
                System.out.printf("%nResult: \"%s\" not in labels%n", object);
            }

        }
    }

    public static void main(String[] args) throws Exception {
        String file = args[0];
        String word = args[1];

        // Sonuçları hem konsola hem dosyaya yaz
        String outPath = args.length > 2 ? args[2] : "vision_results_arda.txt";
        java.io.OutputStream tee = new java.io.OutputStream() {
            final java.io.OutputStream console = System.out;
            final java.io.OutputStream file = new java.io.FileOutputStream(outPath);
            @Override public void write(int b) throws java.io.IOException { console.write(b); file.write(b); }
            @Override public void write(byte[] b, int off, int len) throws java.io.IOException { console.write(b, off, len); file.write(b, off, len); }
            @Override public void flush() throws java.io.IOException { console.flush(); file.flush(); }
            @Override public void close() throws java.io.IOException { file.close(); }
        };
        System.setOut(new java.io.PrintStream(tee));

        System.out.println("=== Vision Test: " + file + " | target: " + word + " ===");
        analyzeimage(file, word);
        tee.close();
    }
}
