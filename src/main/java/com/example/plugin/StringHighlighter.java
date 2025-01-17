package com.example.plugin;

import com.example.plugin.MethodGutterIconRenderer;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;


import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;

@Service
public final class StringHighlighter implements EditorFactoryListener {

    private final Set<String> allowedMethods;
    private boolean notificationShown = false; // Ensure the toast is shown only once

    public StringHighlighter() {
        String filePath = "C:\\Users\\gabri\\IdeaProjects\\demo\\DABCS-functions\\numpy-dabcs.csv";
        this.allowedMethods = extractMethodNamesFromCSV(filePath);
    }

    private Set<String> extractMethodNamesFromCSV(String filePath) {
        Set<String> methodNames = new HashSet<>();
        File file = new File(filePath);

        if (!file.exists()) {
            System.err.println("CSV file not found: " + filePath);
            return methodNames;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("method: def")) {
                    int start = line.indexOf("method: def") + 11;
                    int end = line.indexOf("(", start);
                    if (start > -1 && end > -1) {
                        String methodName = line.substring(start, end).trim();
                        methodNames.add(methodName);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return methodNames;
    }

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();

        // Show the toast notification when the first editor is opened
        if (!notificationShown) {
//            showHighlightNotification();
            notificationShown = true;
        }

        // Start the periodic task for highlighting
        startHighlightingTask(editor);
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        // Clean up resources if necessary
    }

    private void startHighlightingTask(Editor editor) {
        // Timer for periodic highlighting
        Timer timer = new Timer(true); // Daemon timer
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (editor.isDisposed()) {
                    timer.cancel(); // Stop the timer if the editor is closed
                    return;
                }

                String documentText = editor.getDocument().getText();
                highlightMethodCalls(editor, documentText); // Highlight methods dynamically
            }
        }, 0, 1000); // Run every second

        // Mouse motion listener for hover-based tooltip display
        EditorFactory.getInstance().getEventMulticaster().addEditorMouseMotionListener(new EditorMouseMotionListener() {
            @Override
            public void mouseMoved(@NotNull EditorMouseEvent e) {
                Editor currentEditor = e.getEditor();
                if (currentEditor != editor) return; // Ensure this listener is for the correct editor

                int offset = currentEditor.getCaretModel().getOffset();
                MarkupModel markupModel = currentEditor.getMarkupModel();

                // Check if the mouse is over a highlighter
                for (RangeHighlighter highlighter : markupModel.getAllHighlighters()) {
                    if (highlighter.getStartOffset() <= offset && offset <= highlighter.getEndOffset()) {
                        Object tooltip = highlighter.getErrorStripeTooltip();
                        if (tooltip != null) {
                            // IntelliJ handles tooltip display automatically via setErrorStripeTooltip.
                            // You don't need to manually trigger anything here.
                            break; // Exit after finding the first relevant highlighter
                        }
                    }
                }
            }
        }, () -> {});
    }





    public void highlightMethodCalls(Editor editor, String documentText) {
        Pattern pattern = Pattern.compile("\\b\\w+\\s*\\(");
        Matcher matcher = pattern.matcher(documentText);

        MarkupModel markupModel = editor.getMarkupModel();
        markupModel.removeAllHighlighters(); // Clear previous highlights

        while (matcher.find()) {
            String match = matcher.group();
            String methodName = match.substring(0, match.indexOf("(")).trim();

            if (allowedMethods.contains(methodName)) {
                int start = matcher.start();
                int end = matcher.end();

                TextAttributes textAttributes = new TextAttributes();
                textAttributes.setEffectType(EffectType.LINE_UNDERSCORE);
                textAttributes.setEffectColor(Color.BLUE);

                RangeHighlighter highlighter = markupModel.addRangeHighlighter(
                        start, end, HighlighterLayer.SELECTION, textAttributes, HighlighterTargetArea.EXACT_RANGE
                );

                highlighter.setErrorStripeTooltip("Method '" + methodName + "' is highlighted because it matches the CSV list.");
                highlighter.setGutterIconRenderer(new MethodGutterIconRenderer(methodName));
            }
        }
    }

    private void showHighlightNotification() {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Method Highlight Notification")
                .createNotification(
                        "Method Highlighter Active",
                        "The plugin is actively highlighting methods defined in your CSV file.",
                        NotificationType.INFORMATION
                ).notify(null);
    }

    private void showToastNotification(String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Method Highlight Notification")
                .createNotification(
                        "Hover Notification",
                        message,
                        NotificationType.INFORMATION
                ).notify(null);
    }

    private void showBalloonTooltip(Editor editor, Point point, String message) {
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, null, JBColor.BLUE, JBColor.WHITE, null)
                .setFadeoutTime(3000) // Balloon disappears after 3 seconds
                .createBalloon()
                .show(RelativePoint.fromScreen(point), Balloon.Position.above);
    }

}
