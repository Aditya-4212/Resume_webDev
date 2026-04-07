package com.latexeditor.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.nio.file.*;
import java.util.logging.Logger;

/**
 * CompileServlet
 *
 * Replaces the Express route:
 *   POST /compile  →  writes .tex to a temp dir, runs pdflatex, returns PDF bytes
 *
 * Deployed on Apache Tomcat 10 (Jakarta Servlet 5.0)
 *
 * Request body (JSON):  { "latex": "\\documentclass..." }
 * Response:             application/pdf  OR  application/json { "error": "..." }
 */
@WebServlet("/compile")
public class CompileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(CompileServlet.class.getName());

    private String pdflatexPath;

    @Override
    public void init() throws ServletException {
        pdflatexPath = getInitParameter("pdflatexPath");
        if (pdflatexPath == null || pdflatexPath.isBlank()) {
            pdflatexPath = "/usr/bin/pdflatex";
        }
        log.info("CompileServlet initialised – pdflatex: " + pdflatexPath);
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

        // ── Read JSON body ────────────────────────────────────────────
        String body = readBody(req);
        String latex = extractLatex(body);

        if (latex == null || latex.isBlank()) {
            sendJsonError(resp, 400, "No valid LaTeX code provided");
            return;
        }

        log.info("--- COMPILE REQUEST ---  length: " + latex.length());

        // ── Create temp directory ─────────────────────────────────────
        Path tempDir = Files.createTempDirectory("latexeditor_");

        try {
            Path texFile = tempDir.resolve("document.tex");
            Path pdfFile = tempDir.resolve("document.pdf");

            Files.writeString(texFile, latex);

            // ── Check pdflatex exists ─────────────────────────────────
            File pdflatexBin = new File(pdflatexPath);
            if (!pdflatexBin.exists() || !pdflatexBin.canExecute()) {
                sendJsonError(resp, 500, "pdflatex not found at " + pdflatexPath +
                        ". Please install TeX Live / MiKTeX on the server.");
                return;
            }

            // ── Run pdflatex ──────────────────────────────────────────
            ProcessBuilder pb = new ProcessBuilder(
                pdflatexPath,
                "-output-directory=" + tempDir.toAbsolutePath(),
                "-halt-on-error",
                "-interaction=nonstopmode",
                "-no-shell-escape",
                texFile.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            pb.directory(tempDir.toFile());

            Process proc = pb.start();
            String output = new String(proc.getInputStream().readAllBytes());

            boolean finished;
            try {
                finished = proc.waitFor() == 0;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendJsonError(resp, 500, "Compilation interrupted");
                return;
            }

            if (!finished) {
                sendJsonError(resp, 400, output.isBlank() ? "pdflatex returned non-zero exit code" : output);
                return;
            }

            // ── Read and stream PDF ───────────────────────────────────
            if (!Files.exists(pdfFile)) {
                sendJsonError(resp, 500, "pdflatex finished but no PDF was produced");
                return;
            }

            byte[] pdfBytes = Files.readAllBytes(pdfFile);
            resp.setContentType("application/pdf");
            resp.setContentLength(pdfBytes.length);
            resp.getOutputStream().write(pdfBytes);

        } finally {
            // ── Cleanup temp directory ────────────────────────────────
            try (var stream = Files.walk(tempDir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                      .map(Path::toFile)
                      .forEach(File::delete);
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    /**
     * Minimal JSON extraction for { "latex": "..." }
     * For production, replace with a proper JSON library (Gson / Jackson).
     */
    private String extractLatex(String json) {
        if (json == null || json.isBlank()) return null;
        // Find "latex": "<value>"  – handles multi-line values via unescaping
        int keyIdx = json.indexOf("\"latex\"");
        if (keyIdx == -1) return null;
        int colon = json.indexOf(':', keyIdx);
        if (colon == -1) return null;
        int quote1 = json.indexOf('"', colon + 1);
        if (quote1 == -1) return null;
        // Walk to find unescaped closing quote
        StringBuilder val = new StringBuilder();
        for (int i = quote1 + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(++i);
                switch (next) {
                    case '"'  -> val.append('"');
                    case '\\' -> val.append('\\');
                    case 'n'  -> val.append('\n');
                    case 'r'  -> val.append('\r');
                    case 't'  -> val.append('\t');
                    default   -> { val.append('\\'); val.append(next); }
                }
            } else if (c == '"') {
                break;
            } else {
                val.append(c);
            }
        }
        return val.toString();
    }

    private void sendJsonError(HttpServletResponse resp, int status, String message)
            throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("{\"error\":\"" + escapeJson(message) + "\"}");
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }
}
