import java.io.*;
import java.util.*;

public class Classwork05 {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the path to the .txt file to translate:");
        String inputFilePath = scanner.nextLine();
        System.out.println("Enter the target language (e.g., 'en', 'es', 'fr', etc.):");
        String targetLanguage = scanner.nextLine();

        String openAIToken = System.getenv("OpenAIToken");
        if (openAIToken == null || openAIToken.isEmpty()) {
            System.err.println("Environment variable 'OpenAIToken' not set.");
            return;
        }

        StringBuilder fileContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append("\n");
            }
        }

        String prompt = String.format("Translate the following text to %s:\n%s", targetLanguage, fileContent.toString());

        // Prepare CURL command
        String curlCommand = String.format(
            "curl https://api.openai.com/v1/chat/completions " +
            "-H 'Content-Type: application/json' " +
            "-H 'Authorization: Bearer %s' " +
            "-d '{\"model\":\"gpt-3.5-turbo\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}'",
            openAIToken, prompt.replace("\"", "\\\"")
        );

        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", curlCommand);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line).append("\n");
            }
        }
        process.waitFor();

        // Extract translation from OpenAI response (simple JSON parsing)
        String translatedText = extractTranslation(response.toString());
        String outputFilePath = inputFilePath.replace(".txt", "_translated.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write(translatedText);
        }
        System.out.println("Translated file saved as: " + outputFilePath);
    }

    // Simple method to extract translation from OpenAI response
    private static String extractTranslation(String jsonResponse) {
        // This is a basic extraction, for production use a JSON library
        int contentIdx = jsonResponse.indexOf("\"content\":");
        if (contentIdx == -1) return "Translation not found.";
        int start = jsonResponse.indexOf('"', contentIdx + 10) + 1;
        int end = jsonResponse.indexOf('"', start);
        if (start == 0 || end == -1) return "Translation not found.";
        return jsonResponse.substring(start, end).replace("\\n", "\n");
    }
}
