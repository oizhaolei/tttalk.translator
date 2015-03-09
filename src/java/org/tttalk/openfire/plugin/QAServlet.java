package org.tttalk.openfire.plugin;

import java.io.IOException;

import org.jivesoftware.admin.AuthCheckFilter;
import org.jivesoftware.openfire.XMPPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class QAServlet extends HttpServlet {
	private static final long serialVersionUID = 1159875340630997082L;
	private static final Logger Log = LoggerFactory
			.getLogger(TranslatedServlet.class);

	private TranslatorPlugin plugin;

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		plugin = (TranslatorPlugin) XMPPServer.getInstance().getPluginManager()
				.getPlugin("tttalk.translator");
		AuthCheckFilter.addExclude("tttalk.translator/translated");
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Log.info(request.toString());

		String qa_id = request.getParameter("qa_id");
		String answer = request.getParameter("answer");
		String[] translators = request.getParameter("translators").split(",");
		plugin.qa(translators, qa_id, answer);

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
