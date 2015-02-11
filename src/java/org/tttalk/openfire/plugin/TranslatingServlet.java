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
public class TranslatingServlet extends HttpServlet {
	private static final long serialVersionUID = 9008949607840140354L;

	private static final Logger Log = LoggerFactory
			.getLogger(TranslatingServlet.class);

	private TranslatorPlugin plugin;

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		plugin = (TranslatorPlugin) XMPPServer.getInstance().getPluginManager()
				.getPlugin("tttalk.translator");
		AuthCheckFilter.addExclude("tttalk.translator/translating");
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		Log.info(request.toString());
		String messageId = request.getParameter("message_id");
		String user = request.getParameter("to_user");
		plugin.translating(messageId, user);

		PrintWriter out = response.getWriter();
		out.println("success");

	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		doGet(request, response);
	}

	@Override
	public void destroy() {
		super.destroy();
	}

}
