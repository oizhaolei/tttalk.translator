package org.tttalk.openfire.plugin;

//import java.io.File;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;
import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

/**
 * 1. Accept translate request, then call translate api.<br/>
 * 2. Present a callback interface, send result to a particular user.
 *
 * @author zhaolei
 */
public class TranslatorPlugin implements Plugin, PacketInterceptor {
	private static final Logger log = LoggerFactory
			.getLogger(TranslatorPlugin.class);
	public static final String PLUGIN_NAME = "tttalk.translator";

	private static final String TTTALK_NAMESPACE = "http://tttalk.org/protocol/tttalk";
	private static final String TAG_TRANSLATED = "translated";
	private static final String TAG_TRANSLATING = "translating";
	private static final String TAG_BALANCE = "balance";
	private static final String TAG_QA = "qa";
	private static final String TAG_ANNOUNCEMENT = "announcement";
	private static final String TAG_FRIEND = "friend";
	private static final String TAG_PRESENT = "present";
	private static final String TAG_STORY = "story";
	private static final String TAG_TTTALK = "tttalk";
	private static final String TAG_OLD_VERSION_TRANSLATED = "old_version_translated";

	private static final String CHAT_TYPE = "text";
	private static final int AUTO_BAIDU = 2;
	private static final int AUTO_MANUAL = 1;
	private static final int AUTO_NONE = 0;
	private static final String MANUAL_TRANSLATE_URL = "http://211.149.218.190:8080/litieshuai.api/message/message_request_translate.php";
	private static final String BAIDU_TRANSLATE_URL = "http://211.149.218.190:8080/litieshuai.api/message/message_request_baidu_translate.php";

	private static final String TTTALK_USER_TRANSLATOR = "tttalk.user.translator";
	private final InterceptorManager interceptorManager;

	public String getTranslator() {
		return JiveGlobals.getProperty(TTTALK_USER_TRANSLATOR);
	}

	private final XMPPServer server;
	private final UserManager userManager;

	public TranslatorPlugin() {
		server = XMPPServer.getInstance();
		router = server.getMessageRouter();
		userManager = UserManager.getInstance();
		interceptorManager = InterceptorManager.getInstance();

	}

	@Override
	public void initializePlugin(PluginManager pManager, File pluginDirectory) {
		// register with interceptor manager
		interceptorManager.addInterceptor(this);
	}

	@Override
	public void destroyPlugin() {
		// unregister with interceptor manager
		interceptorManager.removeInterceptor(this);
	}

	private final MessageRouter router;

	public void translated(String messageId, String userId, String toContent,
			String cost, String balance, String auto_translate) {
		Message message = new Message();
		message.setTo(userId);
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = TAG_TRANSLATED;
		message.setSubject(subject);
		message.setBody(toContent);

		Element tttalkNode = message.addChildElement(TAG_TRANSLATED,
				TTTALK_NAMESPACE);
		// tttalkNode.addAttribute("test", "true");
		// tttalkNode.addAttribute("ver", "1");

		tttalkNode.addAttribute("title", subject);
		tttalkNode.addAttribute("message_id", messageId);
		tttalkNode.addAttribute("cost", cost);
		tttalkNode.addAttribute("balance", balance);
		tttalkNode.addAttribute("auto_translate", auto_translate);

		log.info(message.toXML());
		router.route(message);
	}

	public void translating(String messageId, String userId) {
		Message message = new Message();
		message.setTo(userId);
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = TAG_TRANSLATING;
		message.setSubject(subject);
		message.setBody(subject);

		Element tttalkNode = message.addChildElement(TAG_TRANSLATING,
				TTTALK_NAMESPACE);
		// tttalkNode.addAttribute("test", "true");
		// tttalkNode.addAttribute("ver", "1");

		tttalkNode.addAttribute("title", subject);
		tttalkNode.addAttribute("message_id", messageId);

		log.info(message.toXML());
		router.route(message);
	}

	public void qa(String[] translators, String qaId, String answer) {
		Message message = new Message();
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = TAG_QA;
		message.setSubject(subject);
		message.setBody(answer);

		Element tttalkNode = message.addChildElement(TAG_QA, TTTALK_NAMESPACE);

		tttalkNode.addAttribute("title", subject);
		tttalkNode.addAttribute("qa_id", qaId);

		for (String v : translators) {
			message.setTo(v);
			log.info(message.toXML());
			router.route(message);
		}
	}

	public void announcement(String[] translators, String announcementId,
			String title) {
		Message message = new Message();
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = TAG_ANNOUNCEMENT;
		message.setSubject(subject);
		message.setBody(title);

		Element tttalkNode = message.addChildElement(TAG_ANNOUNCEMENT,
				TTTALK_NAMESPACE);

		tttalkNode.addAttribute("title", subject);
		tttalkNode.addAttribute("announcement_id", announcementId);

		for (String v : translators) {
			message.setTo(v);
			log.info(message.toXML());
			router.route(message);
		}
	}

	public void friend(String[] translators, String friend_id, String fullname) {
		Message message = new Message();
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = TAG_FRIEND;
		message.setSubject(subject);

		Element tttalkNode = message.addChildElement(TAG_FRIEND,
				TTTALK_NAMESPACE);

		tttalkNode.addAttribute("title", subject);
		tttalkNode.addAttribute("fullname", fullname);
		tttalkNode.addAttribute("friend_id", friend_id);

		for (String v : translators) {
			message.setTo(v);
			log.info(message.toXML());
			router.route(message);
		}
	}

	public void balance(String translator, String balance, String body) {
		Message message = new Message();
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = TAG_BALANCE;
		message.setSubject(subject);
		message.setBody(balance);

		Element tttalkNode = message.addChildElement(TAG_BALANCE,
				TTTALK_NAMESPACE);

		tttalkNode.addAttribute("balance", balance);
		if (body != null && body.length() > 0)
			tttalkNode.addAttribute("notify", "1");
		else
			tttalkNode.addAttribute("notify", "0");

		message.setTo(translator);
		log.info(message.toXML());
		router.route(message);
	}

	public void story(String[] translators, String photo_id, String title,
			String content, String fullname, String pic_url) {
		Message message = new Message();
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = TAG_STORY;
		message.setSubject(subject);
		message.setBody(content);

		Element tttalkNode = message.addChildElement(TAG_STORY,
				TTTALK_NAMESPACE);

		tttalkNode.addAttribute("photo_id", photo_id);
		tttalkNode.addAttribute("title", title);
		tttalkNode.addAttribute("content", content);
		tttalkNode.addAttribute("fullname", fullname);
		tttalkNode.addAttribute("pic_url", pic_url);

		for (String v : translators) {
			message.setTo(v);
			log.info(message.toXML());
			router.route(message);
		}
	}

	public void present(String translator, String present_id,
			String to_user_photo_id, String fullname, String present_name,
			String pic_url) {
		Message message = new Message();
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = TAG_PRESENT;
		message.setSubject(subject);

		Element tttalkNode = message.addChildElement(TAG_PRESENT,
				TTTALK_NAMESPACE);

		tttalkNode.addAttribute("present_id", present_id);
		tttalkNode.addAttribute("to_user_photo_id", to_user_photo_id);
		tttalkNode.addAttribute("fullname", fullname);
		tttalkNode.addAttribute("present_name", present_name);
		tttalkNode.addAttribute("pic_url", pic_url);

		message.setTo(translator);
		log.info(message.toXML());
		router.route(message);
	}

	public void delete(String[] translators) {
		Message message = new Message();
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = "delete";
		message.setSubject(subject);
		message.addChildElement("tttalk", TTTALK_NAMESPACE);

		for (String v : translators) {
			message.setTo(v);
			log.info(message.toXML());
			router.route(message);
		}
	}

	public void oldVersionTranslated(String[] to_users, String message_id,
			String userid, String from_lang, String to_lang, String file_path,
			String file_type, String file_length, String from_content,
			String to_content, String create_date) {
		Message message = new Message();

		message.setFrom(createXMPPuser(userid));
		String subject = TAG_OLD_VERSION_TRANSLATED;
		message.setSubject(subject);

		Element tttalkNode = message.addChildElement(
				TAG_OLD_VERSION_TRANSLATED, TTTALK_NAMESPACE);

		tttalkNode.addAttribute("title", subject);
		tttalkNode.addAttribute("message_id", message_id);
		tttalkNode.addAttribute("userid", userid);
		tttalkNode.addAttribute("from_lang", from_lang);
		tttalkNode.addAttribute("to_lang", to_lang);
		tttalkNode.addAttribute("file_path", file_path);
		tttalkNode.addAttribute("file_type", file_type);
		tttalkNode.addAttribute("file_length", file_length);
		tttalkNode.addAttribute("from_content", from_content);
		tttalkNode.addAttribute("to_content", to_content);
		tttalkNode.addAttribute("create_date", create_date);

		for (String v : to_users) {
			message.setTo(v);
			log.info(message.toXML());
			router.route(message);
		}
	}

	public String createXMPPuser(String userid) {
		return "chinatalk_" + userid + "@tttalk.org/tttalk";
	}

	public void updateUserPwd(String jid, String newPwd) {

		String username = getUserName(jid);
		try {
			User user = userManager.getUser(username);
			user.setPassword(newPwd);
			log.info(String.format("updateUserPwd:%s,%s", username, newPwd));
		} catch (UserNotFoundException e) {
			log.error(e.getMessage(), e);
		}
	}

	private String getUserName(String jid) {
		return jid.substring(0, jid.indexOf('@'));
	}

	@Override
	public void interceptPacket(Packet packet, Session session,
			boolean incoming, boolean processed) throws PacketRejectedException {

		if ((!processed) && (incoming) && (packet instanceof Message)) {
			Message msg = (Message) packet;
			if (msg.getType() == Message.Type.chat) {
				Element tttalk = msg.getChildElement(TAG_TTTALK,
						TTTALK_NAMESPACE);
				if (tttalk != null) {
					String auto_translate = tttalk
							.attributeValue("auto_translate");
					String type = tttalk.attributeValue("type");

					log.info(msg.toXML());
					if (CHAT_TYPE.equalsIgnoreCase(type)
							&& auto_translate != null) {
						int mode = Integer.parseInt(auto_translate);
						log.info(String.format("mode=%d", mode));
						switch (mode) {
						case AUTO_NONE:
							log.info("AUTO_NONE");
							break;
						case AUTO_MANUAL:
							log.info("AUTO_MANUAL START");
							requestBaiduTranslate(msg);
							log.info("AUTO_MANUAL END");
							break;
						case AUTO_BAIDU:
							log.info("AUTO_BAIDU");
							requestBaiduTranslate(msg);
							break;

						}
					}
				}
			}
		}
	}

	private String getTTTalkId(JID jid) {
		String temp = jid.toString();
		String tttalkId = null;
		String pattern = "chinatalk_";
		if (temp.contains(pattern)) {
			tttalkId = temp.substring("chinatalk_".length(), temp.indexOf("@"));
		}
		return tttalkId;
	}

	public void requestBaiduTranslate(Message msg) {
		log.info("requestBaiduTranslate");

		Element tttalk = msg.getChildElement(TAG_TTTALK, TTTALK_NAMESPACE);
		tttalk.addAttribute("message_id",
				String.valueOf(System.currentTimeMillis()));
		String userid = getTTTalkId(msg.getTo());
		String from_lang = tttalk.attributeValue("from_lang");
		String to_lang = tttalk.attributeValue("to_lang");

		final Map<String, String> postParams = new HashMap<String, String>();
		postParams.put("userid", userid);
		postParams.put("loginid", userid);
		postParams.put("from_lang", from_lang);
		postParams.put("to_lang", to_lang);
		postParams.put("text", msg.getBody());

		Utils.logParameters(postParams);
		log.info("after add" + msg.toXML());
		new Thread(new BaiduTranslateRunnable(msg), "Baidu").start();

	}

	public class BaiduTranslateRunnable implements Runnable {

		private final Message msg;

		public BaiduTranslateRunnable(Message msg) {
			this.msg = msg;
		}

		@Override
		public void run() {
			Element tttalk = msg.getChildElement(TAG_TTTALK, TTTALK_NAMESPACE);
			String userid = getTTTalkId(msg.getTo());
			String message_id = tttalk.attributeValue("message_id");

			final Map<String, String> postParams = new HashMap<String, String>();
			postParams.put("userid", userid);
			postParams.put("loginid", userid);
			postParams.put("from_lang", tttalk.attributeValue("from_lang"));
			postParams.put("to_lang", tttalk.attributeValue("to_lang"));
			postParams.put("text", msg.getBody());
			String response = Utils.post(BAIDU_TRANSLATE_URL, postParams);
			String to_content = parseBaiduResponse(response);

			translated(message_id, msg.getTo().toString(), to_content, "0",
					"0", "1");
		}
	}

	public void requestManualTranslate(String message, String from_lang,
			String to_lang, String filetype, String userid,
			String content_length, String callback_id) {

		Map<String, String> postParams = new HashMap<String, String>();

		postParams.put("userid", userid);
		postParams.put("loginid", userid);
		postParams.put("local_id", String.valueOf(System.currentTimeMillis()));
		postParams.put("from_lang", from_lang);
		postParams.put("to_lang", to_lang);
		postParams.put("text", message);
		postParams.put("filetype", "text");
		postParams.put("content_length", content_length);
		postParams.put("to_userid", String.valueOf(-1));
		postParams.put("sign", userid);

		String response = Utils.post(MANUAL_TRANSLATE_URL, postParams);
		String to_content = parseBaiduResponse(response);

	}

	private String parseBaiduResponse(String body) {
		try {
			JSONObject obj = new JSONObject(body);
			boolean success = obj.getBoolean("success");
			if (success) {
				JSONObject data = obj.getJSONObject("data");
				return data.getString("to_content");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
