package com.example.plugin;

import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class MethodGutterIconRenderer extends GutterIconRenderer {
    private final String methodName;

    public MethodGutterIconRenderer(String methodName) {
        this.methodName = methodName;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return AllIcons.General.Warning; // Use IntelliJ's built-in warning icon
    }

    @Nullable
    @Override
    public String getTooltipText() {
        return "Method '" + methodName + "' is highlighted because it matches the CSV list.";
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
