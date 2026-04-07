package com.latexeditor.util;

import jakarta.servlet.http.*;

public class SessionUtil {

    public static void createSession(HttpServletRequest request, String username) {
        HttpSession session = request.getSession();
        session.setAttribute("user", username);
    }

    public static boolean isLoggedIn(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("user") != null;
    }

    public static void destroySession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
