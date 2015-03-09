package org.tttalk.openfire.plugin;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class DeleteServlet extends AbstractTranslatorServlet {
	private static final long serialVersionUID = 1159875340630997082L;
	private static final Logger Log = LoggerFactory
			.getLogger(DeleteServlet.class);

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Log.info(request.toString());

		String[] translators = request.getParameter("translators").split(",");
		plugin.delete(translators);

		PrintWriter out = response.getWriter();
		out.println("success");
	}

	@Override
	String getUri() {
		return "/delete";
	}
}
