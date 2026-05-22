package ch.uzh.ifi.hase.soprafs26;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class SynonymMapTest {

    @Test
    public void getAcceptedTerms_knownAndUnknownWords_returnsNormalizedTerms() {
        List<String> carTerms = SynonymMap.getAcceptedTerms("  CAR  ");

        assertEquals("car", carTerms.get(0));
        assertTrue(carTerms.contains("automobile"));
        assertTrue(carTerms.contains("vehicle"));

        List<String> unknownTerms = SynonymMap.getAcceptedTerms(" Mystery Object ");

        assertEquals(List.of("mystery object"), unknownTerms);
    }
}
