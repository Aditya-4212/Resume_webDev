package com.latexeditor.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * AiResumeServlet
 *
 * Replaces the Express route:
 *   POST /ai-resume  →  calls Gemini API, returns { "latex": "..." }
 *
 * Deployed on Apache Tomcat 10 (Jakarta Servlet 5.0)
 * Uses Java 11+ HttpClient (no extra dependencies needed).
 *
 * Configure GEMINI_API_KEY via:
 *   1. context.xml  <Parameter name="GEMINI_API_KEY" value="..."/>
 *   2. web.xml      <init-param> (see web.xml)
 *   3. Environment  variable GEMINI_API_KEY
 */
@WebServlet("/ai-resume")
public class AiResumeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(AiResumeServlet.class.getName());

    private static final String[] MODELS = {
        "gemini-2.0-flash",
        "gemini-2.0-flash-lite",
        "gemini-1.5-flash-latest",
        "gemini-1.5-pro-latest"
    };

    private static final String BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private String apiKey;
    private HttpClient httpClient;

    @Override
    public void init() throws ServletException {
        // Priority: init-param → environment variable
        apiKey = getInitParameter("geminiApiKey");
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${")) {
            apiKey = System.getenv("GEMINI_API_KEY");
        }
        if (apiKey != null && !apiKey.isBlank()) {
            log.info("AiResumeServlet: Gemini API key loaded ✓");
        } else {
            log.warning("AiResumeServlet: GEMINI_API_KEY is MISSING");
        }

        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        setCorsHeaders(resp);
        resp.setStatus(204);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        setCorsHeaders(resp);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if (apiKey == null || apiKey.isBlank()) {
            sendError(resp, 500, "GEMINI_API_KEY missing – configure it in web.xml or as an environment variable");
            return;
        }

        String body  = readBody(req);
        String prompt = extractStringField(body, "prompt");

        if (prompt == null || prompt.isBlank()) {
            sendError(resp, 400, "No prompt provided");
            return;
        }

        log.info("--- AI RESUME REQUEST ---  length: " + prompt.length());

        String lastError = "Unknown error";

        for (String model : MODELS) {
            String url = String.format(BASE_URL, model, apiKey);

            String requestBody = buildGeminiRequest(prompt);

            try {
                HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

                HttpResponse<String> httpResp =
                    httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());

                String responseBody = httpResp.body();

                if (httpResp.statusCode() != 200) {
                    lastError = "HTTP " + httpResp.statusCode() + " from " + model;
                    log.warning("Model " + model + " failed: " + lastError);
                    continue;
                }

                String latex = extractLatexFromGeminiResponse(responseBody);

                if (latex == null || latex.isBlank()) {
                    lastError = "Empty response from model " + model;
                    log.warning(lastError);
                    continue;
                }

                // Strip any accidental markdown fences
                latex = latex
                    .replaceAll("(?i)^```latex\\s*", "")
                    .replaceAll("(?i)^```\\s*", "")
                    .replaceAll("```\\s*$", "")
                    .trim();

                log.info("Success with model: " + model + ", length: " + latex.length());
                resp.getWriter().write("{\"latex\":" + jsonString(latex) + "}");
                return;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                lastError = "Request interrupted for model " + model;
                log.warning(lastError);
            } catch (Exception e) {
                lastError = e.getMessage();
                log.warning("Model " + model + " threw: " + e.getMessage());
            }
        }

        sendError(resp, 502, "All Gemini models failed. Last error: " + lastError);
    }

    // ── Gemini request builder ─────────────────────────────────────────

    private String buildGeminiRequest(String prompt) {
        String systemText = "You are a LaTeX expert. Output ONLY raw compilable LaTeX code. " +
                            "No markdown fences, no explanations, nothing outside the LaTeX document.";
        return "{"
            + "\"systemInstruction\":{\"parts\":[{\"text\":" + jsonString(systemText) + "}]},"
            + "\"contents\":[{\"role\":\"user\",\"parts\":[{\"text\":" + jsonString(prompt) + "}]}],"
            + "\"generationConfig\":{\"temperature\":0.3,\"maxOutputTokens\":4096}"
            + "}";
    }

    /** Pull the text value out of candidates[0].content.parts[0].text */
    private String extractLatexFromGeminiResponse(String json) {
        // Minimal extraction without a JSON library
        int idx = json.indexOf("\"text\"");
        if (idx == -1) return null;
        int colon = json.indexOf(':', idx);
        if (colon == -1) return null;
        int quote = json.indexOf('"', colon + 1);
        if (quote == -1) return null;

        StringBuilder sb = new StringBuilder();
        for (int i = quote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(++i);
                switch (next) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    default   -> { sb.append('\\'); sb.append(next); }
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ── Utilities ─────────────────────────────────────────────────────

    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private String extractStringField(String json, String field) {
        if (json == null) return null;
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int colon = json.indexOf(':', idx);
        int quote  = json.indexOf('"', colon + 1);
        if (quote == -1) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = quote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(++i);
                switch (next) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    default   -> { sb.append('\\'); sb.append(next); }
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void sendError(HttpServletResponse resp, int status, String msg) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write("{\"error\":" + jsonString(msg) + "}");
    }

    private String jsonString(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\r", "\\r")
                       .replace("\n", "\\n")
                       .replace("\t", "\\t") + "\"";
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }
}
