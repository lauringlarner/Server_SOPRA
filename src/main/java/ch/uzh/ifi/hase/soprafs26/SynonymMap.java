package ch.uzh.ifi.hase.soprafs26;
 
import java.util.*;
 
public class SynonymMap {
 
    private static final Map<String, List<String>> SYNONYMS = new HashMap<>();
 
    static {
        // ── Transport / Vehicles ──────────────────────────────────
        SYNONYMS.put("bicycle",           List.of("bike", "cycle", "push bike", "road bike", "mountain bike", "cycling"));
        SYNONYMS.put("car",               List.of("automobile", "vehicle", "sedan", "coupe", "hatchback", "saloon", "motorcar"));
        SYNONYMS.put("bus",               List.of("coach", "minibus", "double-decker", "transit bus", "public transport"));
        SYNONYMS.put("truck",             List.of("lorry", "pickup truck", "semi truck", "freight truck", "cargo truck", "van"));
        SYNONYMS.put("motorcycle",        List.of("motorbike", "moped", "bike", "chopper", "dirt bike", "two-wheeler"));
        SYNONYMS.put("scooter",           List.of("motor scooter", "moped", "vespa", "kick scooter", "e-scooter"));
        SYNONYMS.put("motor scooter",     List.of("scooter", "moped", "vespa", "motorized scooter"));
        SYNONYMS.put("train",             List.of("railway", "locomotive", "rail", "commuter train", "freight train", "passenger train"));
        SYNONYMS.put("tram",              List.of("streetcar", "light rail", "cable car", "trolley", "trolleybus"));
        SYNONYMS.put("taxi",              List.of("cab", "taxicab", "yellow cab", "hackney"));
        SYNONYMS.put("ambulance",         List.of("emergency vehicle", "paramedic vehicle", "rescue vehicle", "medical vehicle"));
        SYNONYMS.put("fire truck",        List.of("fire engine", "firetruck", "ladder truck", "fire apparatus", "pumper truck", "emergency vehicle"));
        SYNONYMS.put("police car",        List.of("patrol car", "squad car", "cop car", "police vehicle", "cruiser"));
        SYNONYMS.put("skateboard",        List.of("longboard", "skate deck", "deck"));
        SYNONYMS.put("roller skates",     List.of("inline skates", "rollerblades", "skates", "quad skates"));
        SYNONYMS.put("stroller",          List.of("pram", "baby carriage", "pushchair", "buggy", "baby stroller"));
        SYNONYMS.put("shopping cart",     List.of("trolley", "grocery cart", "shopping trolley", "cart"));
        SYNONYMS.put("wheelchair",        List.of("mobility chair", "disability chair", "electric wheelchair", "powerchair"));
        SYNONYMS.put("bike",              List.of("bicycle", "cycle", "push bike", "road bike"));
 
        // ── Traffic / Street ─────────────────────────────────────
        SYNONYMS.put("traffic light",     List.of("traffic signal", "stoplight", "signal light", "semaphore", "traffic lamp"));
        SYNONYMS.put("streetlight",       List.of("street lamp", "lamppost", "lamp post", "road light", "light pole"));
        SYNONYMS.put("bridge",            List.of("overpass", "viaduct", "flyover", "footbridge", "pedestrian bridge"));
        SYNONYMS.put("tunnel",            List.of("underpass", "subway", "passage", "underground passage"));
        SYNONYMS.put("roundabout",        List.of("traffic circle", "rotary", "traffic island", "road circle"));
        SYNONYMS.put("manhole",           List.of("manhole cover", "drain cover", "sewer cover", "utility cover"));
 
        // ── Urban Furniture / Infrastructure ─────────────────────
        SYNONYMS.put("bench",             List.of("seat", "park bench", "public seat", "outdoor seating", "garden bench"));
        SYNONYMS.put("fountain",          List.of("water fountain", "ornamental fountain", "water feature"));
        SYNONYMS.put("drinking fountain", List.of("water dispenser", "water bubbler", "water tap", "bubbler"));
        SYNONYMS.put("fence",             List.of("railing", "barrier", "paling", "picket fence", "wire fence"));
        SYNONYMS.put("gate",              List.of("entrance gate", "garden gate", "iron gate", "barrier"));
        SYNONYMS.put("elevator",          List.of("lift", "platform lift", "hoist"));
        SYNONYMS.put("staircase",         List.of("stairs", "steps", "stairway", "flight of stairs"));
        SYNONYMS.put("security camera",   List.of("cctv", "surveillance camera", "cctv camera", "monitoring camera"));
        SYNONYMS.put("atm",               List.of("cash machine", "cashpoint", "automated teller", "bank machine"));
        SYNONYMS.put("vending machine",   List.of("snack machine", "drinks machine", "dispenser machine", "automat"));
        SYNONYMS.put("payphone",          List.of("pay phone", "public phone", "telephone box", "coin phone"));
        SYNONYMS.put("telephone booth",   List.of("phone box", "call box", "payphone", "public telephone", "red phone box"));
        SYNONYMS.put("train station",     List.of("railway station", "rail station", "terminal", "depot", "transit hub"));
        SYNONYMS.put("gas station",       List.of("petrol station", "fuel station", "filling station", "service station"));
        SYNONYMS.put("car wash",          List.of("vehicle wash", "auto wash", "drive-through wash"));
 
        // ── Buildings / Architecture ──────────────────────────────
        SYNONYMS.put("church",            List.of("cathedral", "chapel", "place of worship", "basilica", "abbey"));
        SYNONYMS.put("door",              List.of("entrance", "doorway", "entryway", "portal"));
        SYNONYMS.put("window",            List.of("glass pane", "pane", "skylight", "bay window", "casement"));
        SYNONYMS.put("chimney",           List.of("smokestack", "flue", "chimney stack", "chimney pot"));
        SYNONYMS.put("satellite dish",    List.of("dish antenna", "parabolic antenna", "tv dish", "satellite antenna"));
        SYNONYMS.put("graffiti",          List.of("street art", "mural", "tag", "spray paint", "wall art"));
 
        // ── Nature / Park ─────────────────────────────────────────
        SYNONYMS.put("tree",              List.of("oak", "pine", "palm", "birch", "maple", "deciduous tree", "conifer"));
        SYNONYMS.put("hedge",             List.of("hedgerow", "bush", "shrub", "topiary", "plant border"));
        SYNONYMS.put("flower",            List.of("blossom", "bloom", "rose", "daisy", "tulip", "floral"));
        SYNONYMS.put("leaf",              List.of("leaves", "foliage", "frond", "plant leaf"));
        SYNONYMS.put("cactus",            List.of("succulent", "desert plant", "prickly pear", "saguaro"));
        SYNONYMS.put("lake",              List.of("pond", "reservoir", "body of water", "lagoon", "tarn"));
 
        // ── Sports / Recreation ───────────────────────────────────
        SYNONYMS.put("playground",        List.of("play area", "play park", "recreation area", "jungle gym"));
        SYNONYMS.put("basketball hoop",   List.of("hoop", "basketball ring", "basketball net", "backboard"));
        SYNONYMS.put("soccer goal",       List.of("football goal", "goalpost", "goal net", "net"));
        SYNONYMS.put("tennis court",      List.of("court", "tennis club", "tennis facility"));
        SYNONYMS.put("basketball court",  List.of("court", "hardcourt", "basketball facility"));
        SYNONYMS.put("football field",    List.of("soccer field", "pitch", "sports field", "playing field"));
        SYNONYMS.put("helmet",            List.of("hard hat", "cycling helmet", "protective helmet", "crash helmet"));
 
        // ── Personal Items / Accessories ─────────────────────────
        SYNONYMS.put("sunglasses",        List.of("shades", "glasses", "eyewear", "tinted glasses"));
        SYNONYMS.put("laptop",            List.of("notebook", "computer", "macbook", "portable computer"));
        SYNONYMS.put("headphones",        List.of("earphones", "earbuds", "headset", "earpods"));
        SYNONYMS.put("flag",              List.of("banner", "pennant", "national flag", "ensign"));
        SYNONYMS.put("newspaper",         List.of("paper", "broadsheet", "tabloid", "journal"));
 
        // ── Furniture / Indoor ────────────────────────────────────
        SYNONYMS.put("table",             List.of("desk", "counter", "dining table", "coffee table"));
        SYNONYMS.put("chair",             List.of("seat", "stool", "armchair", "office chair"));
        SYNONYMS.put("refrigerator",      List.of("fridge", "freezer", "cooler", "icebox"));
        SYNONYMS.put("microwave",         List.of("microwave oven", "microwave cooker", "kitchen appliance"));
        SYNONYMS.put("oven",              List.of("stove", "cooker", "range", "kitchen oven"));
        SYNONYMS.put("sink",              List.of("basin", "washbasin", "kitchen sink", "hand basin"));
        SYNONYMS.put("toilet",            List.of("wc", "lavatory", "loo", "commode", "water closet"));
        SYNONYMS.put("shower",            List.of("shower cabin", "shower cubicle", "shower unit"));
        SYNONYMS.put("bathtub",           List.of("bath", "soaking tub", "tub"));
        SYNONYMS.put("clock",             List.of("wall clock", "timepiece", "alarm clock", "timer"));

        // --New Words--------------------------------------
        SYNONYMS.put("bottle",     List.of("flask", "container", "water bottle", "jar"));
        SYNONYMS.put("mailbox",    List.of("postbox", "letter box", "mail slot", "post box"));
        SYNONYMS.put("glasses",    List.of("spectacles", "eyewear", "frames", "lenses"));
        SYNONYMS.put("swan",       List.of("waterfowl", "bird", "cygnet", "aquatic bird"));
        SYNONYMS.put("pigeon",     List.of("dove", "city bird", "rock pigeon", "bird"));
        SYNONYMS.put("billboard",  List.of("hoarding", "advertisement board", "signboard", "display board"));
        SYNONYMS.put("doorbell",   List.of("bell", "chime", "entry bell", "ringer"));
        SYNONYMS.put("balcony",    List.of("terrace", "veranda", "patio", "deck"));
        SYNONYMS.put("hydrant",    List.of("fire hydrant", "water outlet", "fire plug"));
        SYNONYMS.put("dog",        List.of("canine", "puppy", "hound", "pooch"));
        SYNONYMS.put("cat",        List.of("feline", "kitten", "kitty", "house cat"));
        SYNONYMS.put("umbrella",   List.of("parasol", "rain shield", "sunshade"));
        SYNONYMS.put("tv screen",  List.of("television", "monitor", "display", "screen"));
        SYNONYMS.put("slippers",   List.of("house shoes", "slides", "indoor shoes", "sandals"));
        SYNONYMS.put("chessboard", List.of("checkerboard", "game board", "board"));
        SYNONYMS.put("charger",    List.of("power adapter", "battery charger", "adapter"));
        SYNONYMS.put("statue",     List.of("sculpture", "monument", "figure", "idol"));
        SYNONYMS.put("pizza",      List.of("pie", "pizza pie", "flatbread", "slice"));
        SYNONYMS.put("plate",      List.of("dish", "platter", "serving plate"));
        SYNONYMS.put("spoon",      List.of("tablespoon", "teaspoon", "utensil"));
        SYNONYMS.put("fork",       List.of("dining fork", "table fork", "utensil"));
        SYNONYMS.put("knife",      List.of("blade", "kitchen knife", "cutlery"));
        SYNONYMS.put("towel",      List.of("bath towel", "hand towel", "cloth"));
    }
 
    /**
     * Returns all accepted terms for a given object:
     * the original phrase + all its synonyms.
     */
    public static List<String> getAcceptedTerms(String object) {
        String key = object.toLowerCase().trim();
        List<String> terms = new ArrayList<>();
        terms.add(key);
        if (SYNONYMS.containsKey(key)) {
            terms.addAll(SYNONYMS.get(key));
        }
        return terms;
    }
}
 