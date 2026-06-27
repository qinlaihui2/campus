package com.campus.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * 为每个请求生成 traceId 并写入 MDC，方便日志追踪。
 * 同时放入 request attribute，业务代码可取出返回给前端。
 */
public class TraceIdFilter implements Filter {

    public static final String TRACE_ID_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(TRACE_ID_KEY, traceId);
        request.setAttribute(TRACE_ID_KEY, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
