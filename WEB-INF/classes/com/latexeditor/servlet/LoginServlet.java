package com.latexeditor.servlet;

import com.latexeditor.util.DatabaseUtil;
import com.latexeditor.util.SessionUtil;

import java.io.IOException;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

public class LoginServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        response.setContentType("application/json");

        if (username == null || password == null) {
            response.getWriter().write("{\"success\":false}");
            return;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {

            String sql = "SELECT * FROM users WHERE username=? AND password=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                SessionUtil.createSession(request, username);
                response.getWriter().write("{\"success\":true}");
            } else {
                response.getWriter().write("{\"success\":false}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{\"success\":false}");
        }
    }
}
