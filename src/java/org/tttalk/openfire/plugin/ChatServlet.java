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
		String content = request.getParameter("content");
		String from_lang = request.getParameter("from_lang");
		String to_lang = request.getParameter("to_lang");
		String file_type = request.getParameter("file_type");
		String file_path = request.getParameter("file_path");
		String file_length = request.getParameter("file_length");

		plugin.chat(from_user_id, to_user_id, from_lang, to_lang, file_path, file_type, file_length, content);
		PrintWriter out = response.getWriter();
		out.println("success");
	}

}
