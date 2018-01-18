package com.bbn.parliament.jena.joseki.bridge.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/** @author rbattle */
public class HeadersFilter implements Filter {
   /** {@inheritDoc} */
   @Override
   public void destroy() {
   }

   /** {@inheritDoc} */
   @Override
   public void doFilter(ServletRequest request, ServletResponse response,
         FilterChain chain) throws IOException, ServletException {
      final HttpServletResponse httpResponse = (HttpServletResponse) response;
      httpResponse.addHeader("Access-Control-Allow-Origin", "*");
      chain.doFilter(request, response);
   }

   /** {@inheritDoc} */
   @Override
   public void init(FilterConfig config) throws ServletException {
   }
}
