<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<%@page import="java.util.List"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.bbn.parliament.jena.joseki.graph.ModelManager"%>
<%@page import="com.bbn.parliament.jena.joseki.bridge.tracker.*"%>

<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>Administrator Console</title>
  <link rel="stylesheet" type="text/css" href="stylesheets/joseki.css" />
  <script type="text/javascript" src="javascripts/jquery-1.4.2.min.js"></script>
  </head>

  <body>
    <h1>Administrator Console</h1>
    <div class="op-list"><h3><a href="index.jsp">Home</a></h3> <jsp:include page="index/000_operations.jsp"/></div>

<br/>
    <div class="moreindent infobox">
      <h2>Flush Data</h2>
      <form action="bulk/flush" method="post">
	     <p>
	     Flush in-memory statements to disk: &nbsp;&nbsp; <input type="submit" value="Flush Data" />
        </p>
      </form>
    </div>
    <br/>
    <!--
    <div class="moreindent">
      <br />
      <h2>Shutdown</h2>
      <form action="bulk/shutdown" method="post">
        <p>
        Shutdown the server: &nbsp;&nbsp; <input type="submit" value="Shutdown" />
        </p>
      </form>
      <br/>
    </div>
    -->
   <div class="moreindent infobox">
   <h2 id="numQueries"><span>No</span> Queries</h2>
   <table border=0 width=100%>
   <tr>
   <td align="right">
      <input type=button id=pauseUpdateButton value='Pause Updates' onclick='javascript:pause();'>
      &nbsp;
      <input type=button id="cancelAllButton" value="Cancel All" onclick="javascript:cancelAll();" disabled=true>
   </td>
   </tr>
   </table>
    <div id="queries">

   </div>
   </div>
<script type="text/javascript"><!--
var times = 0;
var paused = false;
var tracked = null;
function loadTrackers() {
   if (paused) {
      return;
   }
   $.ajax({
      url:'tracker',
      cache:false,
      dataType: "json",
      success: function(data) {
         $('#queries').empty();

         if (null != data) {
            html = data.length + " Queries";
            $('#numQueries').html(html);
            $('#cancelAllButton').attr("disabled", data.length == 0);
            tracked = data;
            for (var i = 0; i < data.length; i++) {
               processTracker(data[i]);
            }
         } else {
            $('#numQueries').html("No Queries");
         }
         setTimeout(loadTrackers, 1000);
      },
      error: function(e, xhr) {
         $('#numQueries').html("Error while loading queries");

         $('#queries').html(xhr);
         setTimeout(loadTrackers, 1000);
      }
   });
}

loadTrackers();

function cancelAll() {
   if (tracked == null) {
      return;
   }
   for (var i = 0; i < tracked.length; i++) {
      stopQuery(tracked[i].id);
   }
}
function pause() {
   if (!paused) {
      paused = true;
      $('#pauseUpdateButton').val("Resume Updates");

   } else {
      paused = false;
      $('#pauseUpdateButton').val("Pause Updates");
      loadTrackers();
   }
}

function processTracker(tracker) {
   var query = tracker.display;
/* query = query.replace(RegExp("\n","g"),"\<br\>\n");*/
   query = query.replace(RegExp("<","g"), "&lt;");
   query = query.replace(RegExp(">","g"), "&gt;");
   var date = new Date(tracker.created);



   var now = new Date();
   var numSeconds = (tracker.currentTime - tracker.created) / 1000;
   var html = "<ul><li class='trackerId'>ID:" + tracker.id + "(" + numSeconds + " seconds old)";
   if (tracker.cancellable) {
      html +=" <input type=button value=Stop onclick='javascript:stopQuery("+tracker.id+");'></li>";
   }
   html += "<li><span class='trackerMetaDataType'>From:</span><span class='trackerMetaDataValue'>" + tracker.creator + "</span></li>";
// alert(html);
   html += "<li><span class='trackerMetaDataType'>Created:</span><span class='trackerMetaDataValue'>" + date + "</span></li>";
// alert(html);
   if (tracker.started) {
      var started = new Date(tracker.started);
      html += "<li><span class='trackerMetaDataType'>Started:</span><span class='trackerMetaDataValue'>" + started + "</span></li>";
   }
// alert(html);
   html += "<li><span class='trackerMetaDataType'>Status:</span><span class='trackerMetaDataValue'>" + tracker.status + "</span></li>";
// alert(html);
   html += "<li><pre>" + query + "</pre></li>";
   html += "</ul>" ;

   $('#queries').html($('#queries').html() + html);
}

function stopQuery(id) {

   $.ajax({
      type: "POST",
      url:"tracker",
      data: "id=" + id
   });

}
--></script>
  </body>
</html>
