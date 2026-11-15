package com.example.calestres;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Small offline Spanish-language sentiment heuristic; it does not send notes off-device. */
public final class NoteStressAnalyzer {
    private static final Map<String, Double> WORD_SCORES = new HashMap<>();
    private static final Map<String, String> EMOTIONS = new HashMap<>();

    static {
        addWords(-1.0, "estres", "estresado", "estresada", "estresados", "estresadas",
                "agobiado", "agobiada", "abrumado", "abrumada", "ansiedad", "ansioso",
                "ansiosa", "preocupado", "preocupada", "nervioso", "nerviosa", "tension",
                "presionado", "presionada", "agotado", "agotada", "frustrado", "frustrada",
                "triste", "solo", "sola", "miedo", "mal", "dificil", "duro", "dura",
                "irritable", "enojado", "enojada", "agitado", "agitada", "desbordado",
                "desbordada", "insomnio", "conflicto", "pelea", "llore", "llorar");
        addWords(1.0, "tranquilo", "tranquila", "tranquilos", "tranquilas", "calma", "calmado",
                "calmada", "relajado", "relajada", "feliz", "contento", "contenta", "bien",
                "genial", "mejor", "aliviado", "aliviada", "agradecido", "agradecida",
                "disfrute", "disfrute", "apoyado", "apoyada", "acompanado", "acompanada",
                "orgulloso", "orgullosa", "optimista", "descansado", "descansada", "logre",
                "pude", "avance", "divertido", "divertida", "motivado", "motivada");

            addEmotion("Frustrado", "frustrado", "frustrada", "frustracion", "frustrante");
            addEmotion("Ansioso", "ansiedad", "ansioso", "ansiosa", "nervioso", "nerviosa",
                "preocupado", "preocupada", "miedo", "estres", "estresado", "estresada");
            addEmotion("Agobiado", "agobiado", "agobiada", "abrumado", "abrumada", "desbordado",
                "desbordada", "presionado", "presionada", "agotado", "agotada");
            addEmotion("Triste", "triste", "solo", "sola", "llore", "llorar", "desanimado", "desanimada");
            addEmotion("Enojado", "enojado", "enojada", "irritable", "rabia", "furioso", "furiosa");
            addEmotion("Tranquilo", "tranquilo", "tranquila", "calma", "calmado", "calmada",
                "relajado", "relajada", "aliviado", "aliviada");
            addEmotion("Alegre", "feliz", "contento", "contenta", "genial", "divertido", "divertida");
            addEmotion("Agradecido", "agradecido", "agradecida", "agradecimiento", "orgulloso", "orgullosa");
            addEmotion("Motivado", "motivado", "motivada", "optimista", "logre", "pude", "avance");
    }

    private NoteStressAnalyzer() { }

    /** Returns a value in [-1, 1]: negative suggests distress, positive suggests well-being. */
    public static double sentiment(String note) {
        if (note == null || note.trim().isEmpty()) return 0;
        String[] words = normalize(note).split("[^a-z0-9]+");
        double total = 0;
        int matched = 0;
        int negationWindow = 0;
        double emphasis = 1.0;

        for (String word : words) {
            if (word.isEmpty()) continue;
            if (isNegation(word)) {
                negationWindow = 3;
                continue;
            }
            if (isEmphasis(word)) {
                emphasis = 1.25;
                continue;
            }

            Double score = WORD_SCORES.get(word);
            if (score != null) {
                double adjusted = score * emphasis;
                if (negationWindow > 0) adjusted *= -1;
                total += adjusted;
                matched++;
                emphasis = 1.0;
            }
            if (negationWindow > 0) negationWindow--;
        }

        if (matched == 0) return 0;
        double average = total / matched;
        double confidence = 1.0 - Math.exp(-matched / 2.0);
        return clamp(average * confidence, -1.0, 1.0);
    }

    /** Returns the most frequently detected emotion, or Neutral when no known terms occur. */
    public static String sentimentLabel(String note) {
        if (note == null || note.trim().isEmpty()) return "Neutral";
        String[] words = normalize(note).split("[^a-z0-9]+");
        Map<String, Integer> counts = new HashMap<>();
        int negationWindow = 0;
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (isNegation(word)) {
                negationWindow = 3;
                continue;
            }
            String emotion = EMOTIONS.get(word);
            if (emotion != null) {
                if (negationWindow > 0) emotion = isPositiveEmotion(emotion) ? "Desanimado" : "Tranquilo";
                counts.put(emotion, counts.getOrDefault(emotion, 0) + 1);
            }
            if (negationWindow > 0) negationWindow--;
        }
        String best = "Neutral";
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best;
    }

    public static boolean isPositiveLabel(String label) {
        return label.equals("Alegre") || label.equals("Agradecido") || label.equals("Motivado")
                || label.equals("Tranquilo");
    }

    private static boolean isPositiveEmotion(String emotion) {
        return emotion.equals("Alegre") || emotion.equals("Agradecido") || emotion.equals("Motivado")
                || emotion.equals("Tranquilo");
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private static boolean isNegation(String word) {
        return word.equals("no") || word.equals("nunca") || word.equals("jamas")
                || word.equals("sin") || word.equals("tampoco");
    }

    private static boolean isEmphasis(String word) {
        return word.equals("muy") || word.equals("bastante") || word.equals("demasiado")
                || word.equals("super");
    }

    private static void addWords(double score, String... words) {
        for (String word : words) WORD_SCORES.put(word, score);
    }

    private static void addEmotion(String emotion, String... words) {
        for (String word : words) EMOTIONS.put(word, emotion);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
