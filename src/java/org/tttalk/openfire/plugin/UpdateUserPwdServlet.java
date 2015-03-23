package org.tttalk.openfire.plugin;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class UpdateUserPwdServlet extends AbstractTranslatorServlet {
	private static final long serialVersionUID = 1159875340630997082L;
	private static final Logger Log = LoggerFactory
			.getLogger(UpdateUserPwdServlet.class);

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Log.info(request.toString());

		String username = request.getParameter("user");// devicetoken
		String newPwd = request.getParameter("new_pwd");
		// TODO 更改用户密码
	}

	@Override
	String getUri() {
		return "/change_user_pwd";
	}
}