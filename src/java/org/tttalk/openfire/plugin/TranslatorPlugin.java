package org.tttalk.openfire.plugin;

//import java.io.File;
import java.io.File;

import org.dom4j.Element;
import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Message;

/**
 * 1. Accept translate request, then call translate api.<br/>
 * 2. Present a callback interface, send result to a particular user.
 * 
 * @author zhaolei
 */
public class TranslatorPlugin implements Plugin {
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
	private static final String TAG_OLD_VERSION_TRANSLATED = "old_version_translated";

	private static final String TTTALK_USER_TRANSLATOR = "tttalk.user.translator";

	public String getTranslator() {
		return JiveGlobals.getProperty(TTTALK_USER_TRANSLATOR);
	}

	private final XMPPServer server;

	public TranslatorPlugin() {
		server = XMPPServer.getInstance();
		router = server.getMessageRouter();
	}

	@Override
	public void initializePlugin(PluginManager pManager, File pluginDirectory) {
		// register with interceptor manager
	}

	@Override
	public void destroyPlugin() {
		// unregister with interceptor manager
	}

	private final MessageRouter router;

	public void translated(String messageId, String userId, String toContent,
			int cost, int balance) {
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
		tttalkNode.addAttribute("cost", String.valueOf(cost));
		tttalkNode.addAttribute("balance", String.valueOf(balance));

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

	public void balance(String translator, String balance) {
		Message message = new Message();
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = TAG_BALANCE;
		message.setSubject(subject);
		message.setBody(balance);

		Element tttalkNode = message.addChildElement(TAG_BALANCE,
				TTTALK_NAMESPACE);

		tttalkNode.addAttribute("balance", balance);

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
}
