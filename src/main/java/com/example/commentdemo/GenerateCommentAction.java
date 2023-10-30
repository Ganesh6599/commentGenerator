package com.example.commentdemo;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class GenerateCommentAction extends AnAction {

    private static final String OPENAI_API_KEY = "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"; // provide you API Key from OpenAI
    private static final String OPENAI_API_ENDPOINT = "https://api.openai.com/v1/engines/gpt-3.5-turbo/completions";
    private String generatedComment;
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (project != null && editor != null) {
            // Get the selected code from the editor
            String selectedCode = editor.getSelectionModel().getSelectedText();

            if (selectedCode != null && !selectedCode.isEmpty()) {
                // Make a request to the OpenAI API to generate a comment
                 generatedComment = generateComment(selectedCode);
                // Get the start and end offsets of the selected text
                int selectionStart = editor.getSelectionModel().getSelectionStart();
                int selectionEnd = editor.getSelectionModel().getSelectionEnd();

                // Find the end of the line containing the selected code
                int lineEnd = editor.getDocument().getLineEndOffset(editor.getDocument().getLineNumber(selectionEnd - 1));


                // Replace the selected code with the generated comment
                // ...
                Application application = ApplicationManager.getApplication();
                Runnable runnable = () -> {
                    //String codeWithComment = selectedCode  + " // " + generatedComment;
                    String comment = generatedComment;
                    //String comment="this is a comment";
                    if (!comment.startsWith("//")) {
                        // If not, add // at the beginning
                        comment = "//" + comment;
                    }
                    editor.getDocument().replaceString(lineEnd, lineEnd, comment);
                };
                WriteCommandAction.runWriteCommandAction(project, runnable);

            }
        }
    }

    private String generateComment(String code) {
        String formattedCode=code.replace("\"", "\\\"");
        String assistantResponse="";
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost request = new HttpPost("https://api.openai.com/v1/chat/completions");
        HttpResponse response = null;
        try {
            String requestBody = "{\n" +
                    "  \"messages\": [\n" +
                    "    {\"role\": \"system\", \"content\": \"You are a helpful assistant.\"},\n" +
                    "    {\"role\": \"user\", \"content\": \""+formattedCode+"\\n\\ngenerate the comment/s for the above line of code\"}\n" +
                    "  ],\n" +
                    "  \"model\": \"gpt-3.5-turbo\",\n" +
                    "  \"temperature\": 0.9,\n" +
                    "  \"max_tokens\": 256,\n" +
                    "  \"top_p\": 0.9,\n" +
                    "  \"frequency_penalty\": 0,\n" +
                    "  \"presence_penalty\": 0\n" +
                    "}";

            StringEntity entity = new StringEntity(requestBody);
            request.setEntity(entity);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer "+OPENAI_API_KEY);

            response= httpClient.execute(request);

            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            JSONObject jsonResponse = new JSONObject(result.toString());

            // Extract content for the assistant role
             assistantResponse = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            // Print the assistant's response content
            System.out.println("Assistant's Response: " + assistantResponse);

            // Handle the response here (e.g., read response.getEntity() for the API response content)
        } catch (Exception e) {
            e.printStackTrace();
        }
        return assistantResponse;
    }
}
