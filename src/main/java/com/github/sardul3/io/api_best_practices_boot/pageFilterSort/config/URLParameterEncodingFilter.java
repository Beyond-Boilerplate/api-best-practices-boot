package com.github.sardul3.io.api_best_practices_boot.pageFilterSort.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class URLParameterEncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(httpRequest) {
                @Override
                public String getParameter(String name) {
                    String value = super.getParameter(name);
                    return encodeSpecialCharacters(value);
                }

                @Override
                public Map<String, String[]> getParameterMap() {
                    Map<String, String[]> paramMap = super.getParameterMap();
                    paramMap.replaceAll((key, values) -> {
                        for (int i = 0; i < values.length; i++) {
                            values[i] = encodeSpecialCharacters(values[i]);
                        }
                        return values;
                    });
                    return paramMap;
                }

                private String encodeSpecialCharacters(String value) {
                    if (value != null && (value.contains(">") || value.contains("<"))) {
                        value = value.replace(">", URLEncoder.encode(">", StandardCharsets.UTF_8));
                        value = value.replace("<", URLEncoder.encode("<", StandardCharsets.UTF_8));
                    }
                    return value;
                }
            };

            chain.doFilter(wrappedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void destroy() {
        // No resources to clean up
    }
}

