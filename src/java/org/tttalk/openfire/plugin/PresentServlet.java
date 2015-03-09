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
public class PresentServlet extends AbstractTranslatorServlet {
	private static final long serialVersionUID = 1159875340630997082L;
	private static final Logger Log = LoggerFactory
			.getLogger(PresentServlet.class);

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Log.info(request.toString());

		String present_id = request.getParameter("present_id");
		String present_name = request.getParameter("present_name");
		String pic_url = request.getParameter("pic_url");
		String translator = request.getParameter("translator");
		plugin.present(translator, present_id, present_name, pic_url);

		PrintWriter out = response.getWriter();
		out.println("success");
	}

	@Override
	String getUri() {
		return "/present";
	}
}
