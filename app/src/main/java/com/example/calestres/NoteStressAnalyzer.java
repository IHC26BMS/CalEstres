package com.example.calestres;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Small offline Spanish-language sentiment heuristic; it does not send notes off-device. */
public final class NoteStressAnalyzer {
    private static final Map<String, Double> WORD_SCORES = new HashMap<>();

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
    }

    private NoteStressAnalyzer() { }

    /** Returns a value in [-1, 1]: negative suggests distress, positive suggests well-being. */
    public static double sentiment(String note) {
        if (note == null || note.trim().isEmpty()) return 0;
        String normalized = Normalizer.normalize(note.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String[] words = normalized.split("[^a-z0-9]+");
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

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
