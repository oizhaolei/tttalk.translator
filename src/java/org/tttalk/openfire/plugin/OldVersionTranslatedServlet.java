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
public class OldVersionTranslatedServlet extends AbstractTranslatorServlet {
	private static final long serialVersionUID = 1159875340630997082L;
	private static final Logger Log = LoggerFactory
			.getLogger(OldVersionTranslatedServlet.class);

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Log.info(request.toString());

		String message_id = request.getParameter("message_id");
		String userid = request.getParameter("userid");
		String from_lang = request.getParameter("from_lang");
		String to_lang = request.getParameter("to_lang");
		String file_path = request.getParameter("file_path");
		String file_length = request.getParameter("file_length");
		String file_type = request.getParameter("file_type");
		String from_content = request.getParameter("from_content");
		String to_content = request.getParameter("to_content");
		String create_date = request.getParameter("create_date");
		String[] to_users = request.getParameter("to_user").split(",");

		plugin.oldVersionTranslated(to_users, message_id, userid, from_lang,
				to_lang, file_path, file_type, file_length, from_content,
				to_content, create_date);

		PrintWriter out = response.getWriter();
		out.println("success");
	}

	@Override
	String getUri() {
		return "/old_version_translated";
	}
}
