package com.example.plugin;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;


import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class MethodGutterIconRenderer extends GutterIconRenderer {
    private final String methodName;
    private final Editor editor;
    private final RangeHighlighter highlighter;
    private final StringHighlighter stringHighlighter;

    public MethodGutterIconRenderer(String methodName, Editor editor, RangeHighlighter highlighter, StringHighlighter stringHighlighter) {
        this.methodName = methodName;
        this.editor = editor;
        this.highlighter = highlighter;
        this.stringHighlighter = stringHighlighter;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return AllIcons.General.Warning; // Use IntelliJ's built-in warning icon
    }

    @Nullable
    @Override
    public String getTooltipText() {
        return String.format(
                "<html>Method '%s' has previously suffered from Default Argument Breaking Changes (DABCs).<br>" +
                        "If you want to ignore this warning, click the icon.</html>",
                methodName
        );
    }

    @Nullable
    @Override
    public AnAction getClickAction() {
        return new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // Ensure the highlighter is still valid
                if (highlighter.isValid()) {
                    MarkupModel markupModel = editor.getMarkupModel();
                    markupModel.removeHighlighter(highlighter); // Remove the highlight
                    stringHighlighter.ignoreWarning(methodName);
                    System.out.println("Warning ignored for method: " + methodName); // Debug log
                } else {
                    System.out.println("Highlighter is no longer valid for method: " + methodName);
                }
            }
        };
    }

    @Override
    public Alignment getAlignment() {
        return Alignment.LEFT;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MethodGutterIconRenderer that = (MethodGutterIconRenderer) obj;
        return Objects.equals(methodName, that.methodName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodName);
    }
}
