package com.example.plugin;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

@Service
public final class StringHighlighter implements EditorFactoryListener {

    private final Map<String, MethodMetadata> allowedMethods = new HashMap<>();
    private final Map<String, String> libraryToCSV;
    private final Set<String> ignoredWarnings = new HashSet<>(); // Track ignored warnings

    public StringHighlighter() {
        libraryToCSV = Map.of(
                "numpy", "C:\\Users\\gabri\\IdeaProjects\\demo\\DABCS-functions\\numpy-dabcs.csv",
                "pandas", "C:\\Users\\gabri\\IdeaProjects\\demo\\DABCS-functions\\pandas-dabcs.csv",
                "sklearn", "C:\\Users\\gabri\\IdeaProjects\\demo\\DABCS-functions\\sklearn-dabcs.csv"
        );
    }

    private Map<String, MethodMetadata> extractMethodNamesFromCSV(String filePath) {
        Map<String, MethodMetadata> methodMap = new HashMap<>();
        File file = new File(filePath);

        if (!file.exists()) {
            System.err.println("CSV file not found: " + filePath);
            return methodMap;
        }

        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] headers = reader.readNext(); // primeira linha
            if (headers == null) return methodMap;

            int fqnIndex = -1, versionIndex = -1;
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().toLowerCase();
                if (h.equals("fqn")) fqnIndex = i;
                else if (h.equals("version")) versionIndex = i;
            }

            if (fqnIndex == -1 || versionIndex == -1) {
                System.err.println("Erro: colunas 'fqn' e 'version' não encontradas.");
                return methodMap;
            }

            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length <= Math.max(fqnIndex, versionIndex)) continue;

                String fqn = row[fqnIndex].trim();
                String version = row[versionIndex].trim();

                // Extrai o nome do método
                String methodName = null;
                if (fqn.contains("method: def")) {
                    int start = fqn.indexOf("method: def") + "method: def".length();
                    int end = fqn.indexOf("(", start);
                    if (start >= 0 && end > start) {
                        methodName = fqn.substring(start, end).trim();
                    }
                }

                // Extrai o nome do argumento
                String param = null;
                if (fqn.contains("param:")) {
                    int start = fqn.indexOf("param:") + "param:".length();
                    int end = fqn.indexOf(":", start);
                    if (start >= 0 && end > start) {
                        param = fqn.substring(start, end).trim();
                    }
                }

                if (methodName != null && param != null && !version.isEmpty()) {
                    methodMap.put(methodName, new MethodMetadata(param, version));
                    System.out.println("Adicionado: " + methodName + " | param: " + param + " | version: " + version);
                }
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }

        return methodMap;
    }



    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        if (editor != null) {
            attachDocumentListener(editor);
            detectLibrariesAndLoadMethods(editor.getDocument());
            highlightMethodCalls(editor);
        }
    }

    private void attachDocumentListener(Editor editor) {
        Document document = editor.getDocument();
        document.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                System.out.println("Document changed. Rechecking imports...");
                detectLibrariesAndLoadMethods(document);
                highlightMethodCalls(editor);
            }
        });
    }

    private void detectLibrariesAndLoadMethods(Document document) {
        Set<String> detectedLibraries = detectImportedLibraries(document);
        allowedMethods.clear(); // Clear previously loaded methods

        for (String library : detectedLibraries) {
            String csvPath = libraryToCSV.get(library);
            if (csvPath != null) {
                allowedMethods.putAll(extractMethodNamesFromCSV(csvPath));
                System.out.println("Loaded methods from: " + csvPath);
            }
        }
    }

    private Set<String> detectImportedLibraries(Document document) {
        Set<String> detectedLibraries = new HashSet<>();
        String[] lines = document.getText().split("\n");

        for (String line : lines) {
            if (line.startsWith("import ") || line.startsWith("from ")) {
                if (line.contains("numpy")) {
                    detectedLibraries.add("numpy");
                } else if (line.contains("pandas")) {
                    detectedLibraries.add("pandas");
                } else if (line.contains("sklearn")) {
                    detectedLibraries.add("sklearn");
                }
            }
        }

        System.out.println("Detected libraries: " + detectedLibraries);
        return detectedLibraries;
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        // Clean up resources if necessary
    }

    private void highlightMethodCalls(Editor editor) {
        Document document = editor.getDocument();
        MarkupModel markupModel = editor.getMarkupModel();
        markupModel.removeAllHighlighters();

        String text = document.getText();
        Pattern pattern = Pattern.compile("\\b(?:\\w+\\.)?(\\w+)\\s*\\("); // matches methodName(
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String methodName = matcher.group(1);

            if (!allowedMethods.containsKey(methodName)) continue;

            if (ignoredWarnings.contains(methodName)) {
                System.out.println("Ignorando manualmente: " + methodName);
                continue;
            }

            MethodMetadata metadata = allowedMethods.get(methodName);
            int startParen = matcher.end();

            int endParen = findClosingParen(text, startParen - 1);
            if (endParen == -1) continue;

            String argsContent = text.substring(startParen, endParen);

            if (argsContent.contains(metadata.param + " =") || argsContent.contains(metadata.param + "=")) {
                System.out.println("Ignorando destaque para " + methodName + ": argumento '" + metadata.param + "' foi definido.");
                continue;
            }

            // Destaque
            TextAttributes textAttributes = new TextAttributes();
            textAttributes.setEffectType(EffectType.LINE_UNDERSCORE);
            textAttributes.setEffectColor(Color.BLUE);

            RangeHighlighter highlighter = markupModel.addRangeHighlighter(
                    matcher.start(1),
                    matcher.end(1),
                    HighlighterLayer.SELECTION,
                    textAttributes,
                    HighlighterTargetArea.EXACT_RANGE
            );

            String tooltip = String.format(
                    "Method '%s' may have breaking changes in argument '%s' since version %s.",
                    methodName, metadata.param, metadata.version
            );

            highlighter.setErrorStripeTooltip(tooltip);
            highlighter.setGutterIconRenderer(
                    new MethodGutterIconRenderer(methodName, editor, highlighter, this, metadata)
            );
        }
    }


    public void ignoreWarning(String methodName) {
        ignoredWarnings.add(methodName);
        System.out.println("Added method to ignored warnings: " + methodName);
    }

    private int findClosingParen(String text, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

}
