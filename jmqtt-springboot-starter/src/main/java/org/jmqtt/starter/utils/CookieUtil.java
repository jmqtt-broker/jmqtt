package org.jmqtt.starter.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;

public class CookieUtil {

    public static String getCookie(HttpServletRequest request, String key) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<Cookie> op = Arrays.stream(cookies).filter(c -> key.equals(c.getName())).findAny();
            if (op.isPresent()) {
                return op.get().getValue();
            }
        }
        return null;
    }

    public static void setCookie(HttpServletResponse response, String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60);
        response.addCookie(cookie);
    }

}
