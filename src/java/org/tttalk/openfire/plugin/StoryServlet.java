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
public class StoryServlet extends AbstractTranslatorServlet {
	private static final long serialVersionUID = 1159875340630997082L;
	private static final Logger Log = LoggerFactory
			.getLogger(StoryServlet.class);

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Log.info(request.toString());

		String photo_id = request.getParameter("photo_id");
		String title = request.getParameter("title");
		String content = request.getParameter("content");
		String fullname = request.getParameter("fullname");
		String pic_url = request.getParameter("pic_url");
		String[] translators = request.getParameter("translators").split(",");
		plugin.story(translators, photo_id, title, content, fullname, pic_url);

		PrintWriter out = response.getWriter();
		out.println("success");
	}

	@Override
	String getUri() {
		return "/story";
	}
}
