package com.example.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class MethodHighlightAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Editor editor = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);

        if (editor == null) {
            Messages.showErrorDialog("No editor is currently open.", "Error");
            return;
        }

        String documentText = editor.getDocument().getText();
        StringHighlighter highlighter = new StringHighlighter();
        highlighter.highlightMethodCalls(editor, documentText);

        Messages.showInfoMessage("Methods highlighted successfully!", "Highlight Complete");
    }
}
