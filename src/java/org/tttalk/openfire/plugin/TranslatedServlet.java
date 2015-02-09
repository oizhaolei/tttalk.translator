package org.tttalk.openfire.plugin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jivesoftware.admin.AuthCheckFilter;
import org.jivesoftware.openfire.XMPPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class TranslatedServlet extends HttpServlet {

	private static final Logger Log = LoggerFactory
			.getLogger(TranslatedServlet.class);

	private TranslatorPlugin plugin;

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		plugin = (TranslatorPlugin) XMPPServer.getInstance().getPluginManager()
				.getPlugin("tttalk.translator");
		AuthCheckFilter.addExclude("tttalk.translator/translate_callback");
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		Log.info("=========doGet : " + request.toString());
		String originId = request.getParameter("callback_id");
		String user = request.getParameter("from_user");
		String toContent = request.getParameter("to_content");
		int cost = Integer.valueOf(request.getParameter("cost"));
		plugin.translateCallback(originId, user, toContent, cost);

		doResponse(request, response);
	}

	private void doResponse(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		PrintWriter out = response.getWriter();
		out.println("success");

	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		Log.info("==========doPost : " + request.toString());
		plugin.processPostCallback(request);
		doResponse(request, response);
	}

	@Override
	public void destroy() {
		super.destroy();
	}

}
