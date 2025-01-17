package com.example.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class HighlighterStartupActivity implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        // Access the StringHighlighter service
        StringHighlighter highlighter = project.getService(StringHighlighter.class);
        if (highlighter != null) {
            System.out.println("StringHighlighter initialized on startup.");
        } else {
            System.err.println("Failed to initialize StringHighlighter on startup.");
        }
    }
}
