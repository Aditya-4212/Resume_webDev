package com.latexeditor.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.logging.Logger;

/**
 * XmlSourceServlet
 *
 * GET /xml-source  →  returns the raw text of templates/templates.xml
 * Used by the home page XML viewer to display the file as-is.
 */
@WebServlet("/xml-source")
public class XmlSourceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(XmlSourceServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setCharacterEncoding("UTF-8");

        String xmlPath = getServletContext().getRealPath("/templates/templates.xml");

        if (xmlPath == null || !new File(xmlPath).exists()) {
            resp.setStatus(404);
            resp.setContentType("text/plain");
            resp.getWriter().write("templates.xml not found");
            return;
        }

        String xmlContent = Files.readString(Path.of(xmlPath), StandardCharsets.UTF_8);

        resp.setContentType("application/xml");
        resp.setContentLength(xmlContent.getBytes(StandardCharsets.UTF_8).length);
        resp.getWriter().write(xmlContent);

        log.info("Served raw templates.xml (" + xmlContent.length() + " chars)");
    }
}
