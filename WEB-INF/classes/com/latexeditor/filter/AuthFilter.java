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

        // ✅ Allow public resources
        if (path.endsWith("login.html") ||
            path.endsWith("index.html") ||
            path.contains("/login") ||     // servlet
            path.contains("/register") ||
            path.contains("/css/") ||
            path.contains("/js/") ||
            path.contains("/images/") ||
            path.contains("fonts") ||
            path.contains("favicon")) {

            chain.doFilter(req, res);
            return;
        }

        // ✅ Check session
        if (!SessionUtil.isLoggedIn(request)) {
            response.sendRedirect("login.html");
            return;
        }

        chain.doFilter(req, res);
    }
}
