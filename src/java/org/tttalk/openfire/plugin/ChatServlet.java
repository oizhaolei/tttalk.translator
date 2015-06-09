package org.tttalk.openfire.plugin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class ChatServlet extends AbstractTranslatorServlet {
	private static final long serialVersionUID = 9008949607840140354L;

	@Override
	String getUri() {
		return "/chat";
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Log.info(request.toString());

		String from_user_id = request.getParameter("from_user_id");
		String to_user_id = request.getParameter("to_user_id");
		String subject = request.getParameter("subject");
		String content = request.getParameter("content");
		String link = request.getParameter("link");
		String pic = request.getParameter("pic");

		plugin.chat(from_user_id, to_user_id, subject, content, link, pic);
		PrintWriter out = response.getWriter();
		out.println("success");
	}

}
