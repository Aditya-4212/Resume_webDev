package com.latexeditor.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 * TemplatesServlet
 *
 * Replaces the Express route:
 *   GET /templates  →  reads templates/templates.xml and returns JSON
 *
 * Deployed on Apache Tomcat 10 (Jakarta Servlet 5.0)
 */
@WebServlet("/templates")
public class TemplatesServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(TemplatesServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Allow CORS for local development
        resp.setHeader("Access-Control-Allow-Origin", "*");

        String xmlPath = getServletContext().getRealPath("/templates/templates.xml");

        if (xmlPath == null) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"Could not locate templates.xml\"}");
            return;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable external entities for security
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(xmlPath));
            doc.getDocumentElement().normalize();

            NodeList templateNodes = doc.getElementsByTagName("template");

            StringBuilder json = new StringBuilder();
            json.append("{\"templates\":[");

            for (int i = 0; i < templateNodes.getLength(); i++) {
                Element el = (Element) templateNodes.item(i);

                String id          = el.getAttribute("id");
                String label       = el.getAttribute("label");
                String description = getTextContent(el, "description");
                String content     = getTextContent(el, "content");

                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"id\":").append(jsonString(id)).append(",");
                json.append("\"label\":").append(jsonString(label)).append(",");
                json.append("\"description\":").append(jsonString(description)).append(",");
                json.append("\"content\":").append(jsonString(content));
                json.append("}");
            }

            json.append("]}");

            log.info("Served " + templateNodes.getLength() + " templates from XML");
            resp.getWriter().write(json.toString());

        } catch (Exception e) {
            log.severe("Failed to parse templates.xml: " + e.getMessage());
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"Invalid templates.xml format: " +
                    jsonEscape(e.getMessage()) + "\"}");
        }
    }

    /** Get trimmed text content of the first child element with the given tag. */
    private String getTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return "";
        return nodes.item(0).getTextContent().trim();
    }

    /** Wrap a string in JSON double-quotes with proper escaping. */
    private String jsonString(String value) {
        if (value == null) return "\"\"";
        return "\"" + jsonEscape(value) + "\"";
    }

    private String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
