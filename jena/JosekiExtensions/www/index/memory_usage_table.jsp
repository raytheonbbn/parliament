<%@page import="java.lang.management.ManagementFactory"%>
<%@page import="java.lang.management.MemoryMXBean"%>
<%
MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
%>
<table>
	<tr>
		<th>Type</th>
		<th>Heap</th>
		<th>Non-Heap</th>
	</tr>
<!--
	<tr>
		<td>Init</td>
		<td align=right><%=memBean.getHeapMemoryUsage().getInit() / 1024 %></td>
		<td align=right><%=memBean.getNonHeapMemoryUsage().getInit() / 1024 %></td>
	</tr>
	<tr>
		<td>Committed</td>
		<td align=right><%=memBean.getHeapMemoryUsage().getCommitted() / 1024 %></td>
		<td align=right><%=memBean.getNonHeapMemoryUsage().getCommitted() / 1024 %></td>
	</tr>
	 -->
	<tr>
		<td>Used</td>
		<td align=right><%=memBean.getHeapMemoryUsage().getUsed() / 1024 %></td>
		<td align=right><%=memBean.getNonHeapMemoryUsage().getUsed() / 1024 %></td>
	</tr>
	<tr>
		<td>Max</td>
		<td align=right><%=memBean.getHeapMemoryUsage().getMax() / 1024 %></td>
		<td align=right><%=memBean.getNonHeapMemoryUsage().getMax() / 1024 %></td>
	</tr>

</table>
<%

%>
