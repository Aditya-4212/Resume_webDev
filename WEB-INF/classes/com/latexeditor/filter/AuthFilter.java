package com.latexeditor.filter;

import com.latexeditor.util.SessionUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class AuthFilter implements Filter {

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI();

        // Allow public resources
        if (path.contains("login") || path.contains("css") || path.contains("js") || path.contains("images")) {
            chain.doFilter(req, res);
            return;
        }

        if (!SessionUtil.isLoggedIn(request)) {
            response.sendRedirect("login.html");
            return;
        }

        chain.doFilter(req, res);
    }
}
