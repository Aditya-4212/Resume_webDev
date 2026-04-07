package com.latexeditor.servlet;

import com.latexeditor.util.SessionUtil;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class LogoutServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        SessionUtil.destroySession(request);

        // redirect with message
        response.sendRedirect("login.html?error=session");
    }
}
