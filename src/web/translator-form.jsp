<%@ page
   import="org.jivesoftware.openfire.XMPPServer,org.tttalk.openfire.plugin.TranslatorPlugin,org.jivesoftware.util.ParamUtils,java.util.HashMap,java.util.Map"
   errorPage="error.jsp"%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt"%>

<%
	boolean save = request.getParameter("save") != null;	
	String baiduUrl = ParamUtils.getParameter(request, "baidu_url");
	String manualUrl = ParamUtils.getParameter(request, "manual_url");
    
	TranslatorPlugin plugin = (TranslatorPlugin) XMPPServer.getInstance().getPluginManager().getPlugin("tttalk.translator");

	Map<String, String> errors = new HashMap<String, String>();	
	if (save) {
	  if (baiduUrl == null || baiduUrl.trim().length() < 1) {
	     errors.put("missingBaiduUrl", "missingBaiduUrl");
	  }
	  if (manualUrl == null || manualUrl.trim().length() < 1) {
	     errors.put("missingManualUrl", "missingManuaUrl");
	  }
	  if (errors.size() == 0) {
	     plugin.setBaiduTranslateUrl(baiduUrl);
	     plugin.setManualTranslateUrl(manualUrl);
	     response.sendRedirect("translator-form.jsp?settingsSaved=true");
	     return;
	  }		
	}
    
	baiduUrl = plugin.getBaiduTranslateUrl();
	manualUrl = plugin.getManualTranslateUrl();
%>

<html>
	<head>
	  <title>Set Baidu/Manual translation url</title>
	  <meta name="pageID" content="translator-form"/>
	</head>
	<body>

<form action="translator-form.jsp?save" method="post">

<div class="jive-contentBoxHeader">Enter the Translation url</div>
<div class="jive-contentBox">
   
	<% if (ParamUtils.getBooleanParameter(request, "settingsSaved")) { %>
   
	<div class="jive-success">
	<table cellpadding="0" cellspacing="0" border="0" style="margin-bottom:5px">
	<tbody>
	  <tr>
	     <td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
	     <td class="jive-icon-label">Settings saved successfully.</td>
	  </tr>
	</tbody>
	</table>
	</div>
   
	<% } %>

	<table cellpadding="3" cellspacing="0" border="0" width="100%">
	<tbody>
	  <tr>
	     <td width="10%" valign="middle">Baidu URL:&nbsp;</td>
	     <td width="90%"><input type="text" name="baidu_url" value="<%= baiduUrl %>" style="width:70%"></td>
	     <% if (errors.containsKey("missingBaiduUrl")) { %>
	        <span class="jive-error-text">Please enter Baidu url</span>
	     <% } %> 
	  </tr>
	  <tr>
	     <td width="10%" valign="middle">Manual URL:&nbsp;</td>
	     <td width="90%"><input type="text" name="manual_url" value="<%= manualUrl %>" style="width:70%"></td>
	     <% if (errors.containsKey("missingManualSecret")) { %>
	        <span class="jive-error-text">Please enter Manual url</span>
	     <% } %> 
	  </tr>
	</tbody>
	</table>
</div>
<input type="submit" value="Save"/>
</form>

</body>
</html>
