package org.apache.hadoop.hdfs.server.datanode;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import java.io.IOException;
import org.apache.hadoop.hdfs.server.common.JspHelper;
import org.apache.hadoop.util.ServletUtil;
import org.apache.hadoop.conf.Configuration;

public final class browseDirectory_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {


  //for java.io.Serializable
  private static final long serialVersionUID = 1L;

  private static java.util.List _jspx_dependants;

  public Object getDependants() {
    return _jspx_dependants;
  }

  public void _jspService(HttpServletRequest request, HttpServletResponse response)
        throws java.io.IOException, ServletException {

    JspFactory _jspxFactory = null;
    PageContext pageContext = null;
    HttpSession session = null;
    ServletContext application = null;
    ServletConfig config = null;
    JspWriter out = null;
    Object page = this;
    JspWriter _jspx_out = null;
    PageContext _jspx_page_context = null;


    try {
      _jspxFactory = JspFactory.getDefaultFactory();
      response.setContentType("text/html; charset=UTF-8");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			null, true, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;


/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file 
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

      out.write('\n');
      out.write('\n');
      out.write("\n\n<html>\n<head>\n<style type=text/css>\n<!--\nbody \n  {\n  font-face:sanserif;\n  }\n-->\n</style>\n");
JspHelper.createTitle(out, request, request.getParameter("dir")); 
      out.write("\n</head>\n\n<body onload=\"document.goto.dir.focus()\">\n");
 
  try {
    Configuration conf = 
      (Configuration) application.getAttribute("datanode.conf");
    DatanodeJspHelper.generateDirectoryStructure(out,request,response, conf);
  }
  catch(IOException ioe) {
    String msg = ioe.getLocalizedMessage();
    int i = msg.indexOf("\n");
    if (i >= 0) {
      msg = msg.substring(0, i);
    }
    out.print("<h3>" + msg + "</h3>");
  }

      out.write("\n<hr>\n\n<h2>Local logs</h2>\n<a href=\"/logs/\">Log</a> directory\n\n");

out.println(ServletUtil.htmlFooter());

      out.write('\n');
    } catch (Throwable t) {
      if (!(t instanceof SkipPageException)){
        out = _jspx_out;
        if (out != null && out.getBufferSize() != 0)
          out.clearBuffer();
        if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
      }
    } finally {
      if (_jspxFactory != null) _jspxFactory.releasePageContext(_jspx_page_context);
    }
  }
}