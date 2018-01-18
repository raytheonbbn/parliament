<%@page import="java.io.File"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Collections"%>

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">


<%@page import="java.util.Comparator"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.io.FilenameFilter"%><html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
  <head>
    <title>Parliament Query Server</title>
    <link rel="stylesheet" type="text/css" href="stylesheets/joseki.css" />
    <script type="text/javascript" src="javascripts/jquery-1.4.2.min.js"></script>
  </head>
<body>
    <h1>Parliament Query Server</h1>
    <p>&nbsp;</p>
<%
File dir = new File(this.getServletContext().getRealPath("index"));
if (dir.exists()) {
   List<String> files = Arrays.asList(dir.list(new FilenameFilter() {
      public boolean accept(File dir, String name) {
         try {
	         Integer i = Integer.parseInt(name.substring(0, 2));
         } catch (NumberFormatException e) {
            return false;
         }
         return name.endsWith(".jsp");
      }
   }));
   if (files != null && files.size() > 0) {
      Collections.sort(files);
      for (String path : files) {
%>
<jsp:include page='<%="index/" + path %>'/>
<%    }
   }
}%>



</body>
</html>
