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

        try (Connection conn = DatabaseUtil.getConnection()) {

            String sql = "SELECT * FROM users WHERE username=? AND password=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {

                // ✅ create session
                SessionUtil.createSession(request, username);

                // ✅ redirect to home
                response.sendRedirect("home.html");

            } else {

                // ❌ invalid login
                response.sendRedirect("login.html?error=invalid");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("login.html?error=server");
        }
    }
}
