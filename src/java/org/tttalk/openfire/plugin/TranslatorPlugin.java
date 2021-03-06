package org.tttalk.openfire.plugin;

//import java.io.File;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;
import org.gearman.client.GearmanClient;
import org.gearman.client.GearmanClientImpl;
import org.gearman.client.GearmanJob;
import org.gearman.client.GearmanJobImpl;
import org.gearman.common.GearmanNIOJobServerConnection;
import org.gearman.util.ByteUtils;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.OfflineMessageStore;
import org.jivesoftware.openfire.PresenceManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
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

	private static final String APP_NAME = "chinatalk";

	private static final String TTTALK_NAMESPACE = "http://tttalk.org/protocol/tttalk";
	private static final String TAG_TRANSLATED = "translated";
	private static final String TAG_TRANSLATING = "translating";
	private static final String TAG_TTTALK = "tttalk";
	private static final String TAG_OLD_VERSION_TRANSLATED = "old_version_translated";
	private static final String TAG_CHAT = "chat";

	private static final String REQUEST_TAG = "request";
	private static final String RECEIVED_TAG = "received";
	private static final String RECEIVED_NAMASPACE = "urn:xmpp:receipts";
	private static final String DELAY_TAG = "delay";
	private static final String DELAY_NAMASPACE = "urn:xmpp:delay";

	private static final String VOLUNTEER_NAMESPACE = "http://tttalk.org/protocol/volunteer";
	private static final String TAG_REQUEST = "request";
	private static final String TAG_CANCEL = "cancel";

	private static final String CHAT_TYPE_TEXT = "text";
	private static final String CHAT_TYPE_VOICE = "voice";
	private static final int AUTO_BAIDU = 2;
	private static final int AUTO_MANUAL = 1;
	private static final int AUTO_NONE = 0;

	private static final String TTTALK_USER_VOLUNTEER = "tttalk.user.volunteer";
	private static final String TTTALK_USER_TRANSLATOR = "tttalk.user.translator";
	private final InterceptorManager interceptorManager;

	public String getTranslator() {
		return JiveGlobals.getProperty(TTTALK_USER_TRANSLATOR);
	}

	public String getVolunteer() {
		return JiveGlobals.getProperty(TTTALK_USER_VOLUNTEER);
	}

	private final XMPPServer server;
	private final UserManager userManager;
	private final PresenceManager presenceManager;
	private final OfflineMessageStore offlineMessageStore;

	public TranslatorPlugin() {
		server = XMPPServer.getInstance();
		router = server.getMessageRouter();
		userManager = server.getUserManager();
		interceptorManager = InterceptorManager.getInstance();
		presenceManager = server.getPresenceManager();
		gearmanClient = genGearmanClient();
		offlineMessageStore = OfflineMessageStore.getInstance();

	}

	private GearmanClient genGearmanClient() {
		GearmanClientImpl gearmanClient = new GearmanClientImpl();
		GearmanNIOJobServerConnection gearmanConnection = new GearmanNIOJobServerConnection(
				Utils.getGearmanHost(), Utils.getGearmanPort());
		gearmanClient.addJobServer(gearmanConnection);
		return gearmanClient;
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
	private GearmanClient gearmanClient;

	public void translated(String messageId, String userId, String toContent,
			String cost, String auto_translate) {
		Message message = new Message();
		userId = userId.split("/")[0];
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
		tttalkNode.addAttribute("auto_translate", auto_translate);

		addRequestReceipts(message);

		log.info("translated=" + message.toXML());
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

		addRequestReceipts(message);

		log.info(message.toXML());
		router.route(message);
	}

	public void oldVersionTranslated(String[] to_users, String message_id,
			String userid, String from_lang, String to_lang, String file_path,
			String file_type, String file_length, String from_content,
			String to_content, String create_date) {
		log.info("oldVersionTranslated");
		Message message = new Message();
		message.setType(Message.Type.chat);
		message.setID(userid);

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

		addRequestReceipts(message);

		for (String v : to_users) {
			message.setTo(v);
			log.info(message.toXML());
			router.route(message);
		}
	}

	public void chat(String from_user_id, String to_user_id, String subject,
			String from_content, String link, String pic, String type) {
		Message message = new Message();
		message.setType(Message.Type.chat);
		message.setID(from_user_id);

		message.setFrom(createXMPPuser(from_user_id));
		message.setSubject(subject);
		message.setBody(from_content);

		Element tttalkNode = message
				.addChildElement(TAG_CHAT,
				TTTALK_NAMESPACE);

		tttalkNode.addAttribute("link", link);
		tttalkNode.addAttribute("pic", pic);
        tttalkNode.addAttribute("type", type);

		addRequestReceipts(message);

		message.setTo(createXMPPuser(to_user_id));
		log.info(message.toXML());
		router.route(message);
	}

	public String createXMPPuser(String userid) {
		return "chinatalk_" + userid + "@tttalk.org/tttalk";
	}

	public void changePassword(String username, String password) {

		try {
			User user = userManager.getUser(username);
			user.setPassword(password);
			log.info(String.format("changePassword:%s,%s", username, password));
		} catch (UserNotFoundException e) {
			log.info(username, e);
		}
	}

	public void createAccount(String username, String password) {
		try {
			User user = userManager.createUser(username, password, null, null);
			log.info(String.format("createAccount:%s,%s", user.getUID(),
					user.getUsername()));
		} catch (UserAlreadyExistsException e) {
			log.info(username + " UserAlreadyExists.");
			changePassword(username, password);
		}
	}

	@Override
	public void interceptPacket(Packet packet, Session session,
			boolean incoming, boolean processed) throws PacketRejectedException {

		// Save to offline table
		saveMessageToOfflineTable(packet, session, incoming, processed);

		if ((!processed) && (incoming) && (packet instanceof Message)) {
			Message msg = (Message) packet;
			if (msg.getType() == Message.Type.chat) {
				Element tttalk = msg.getChildElement(TAG_TTTALK,
						TTTALK_NAMESPACE);
				if (tttalk != null) {
                  String from_lang = tttalk.attributeValue("from_lang");
                  String to_lang = tttalk.attributeValue("to_lang");
                  String type = tttalk.attributeValue("type");
                  String filePath = tttalk.attributeValue("file_path");
				    //qa
				    String toUserId = getTTTalkId(msg.getTo());
				    if(toUserId.equals(Utils.getSystemId())){
				      requestAddQa(msg.getFrom(), msg.getBody());
                      log.info("ADD_QA END");
				      return;
				    }
					increaseChatBadgeCount(msg.getTo());
					// String auto_translate = tttalk
					// .attributeValue("auto_translate");
					log.info(msg.toXML());
					if (from_lang != null
							&& to_lang != null
							&& (CHAT_TYPE_TEXT.equalsIgnoreCase(type) || CHAT_TYPE_VOICE
									.equalsIgnoreCase(type))
							&& !from_lang.equalsIgnoreCase(to_lang)) {
						String auto_translate = null;
						String username = getUsernameFromJID(msg.getTo());
						String key = getUsernameFromJID(msg.getFrom())
								+ "_auto_translate";
						auto_translate = getProperty(username, key,
								String.valueOf(AUTO_BAIDU));

						String message_id = tttalk.attributeValue("message_id");
						if (message_id == null || "null".equals(message_id)) {
							message_id = String.valueOf(System
									.currentTimeMillis());
							tttalk.addAttribute("message_id", message_id);
						}

						tttalk.addAttribute("auto_translate", auto_translate);
						int at_mode = Integer.parseInt(auto_translate);
						log.info(String.format("auto_translate=%d", at_mode));
						switch (at_mode) {
						case AUTO_NONE:
							log.info("AUTO_NONE");
							break;
						case AUTO_MANUAL:
							log.info("AUTO_MANUAL START");
							requestManualTranslate(msg.getFrom(), msg.getTo(),
									message_id, from_lang, to_lang,
									msg.getBody(), type, filePath,
									tttalk.attributeValue("content_length"));
							log.info("AUTO_MANUAL END");
							break;
						case AUTO_BAIDU:

							log.info("AUTO_BAIDU");
							if (CHAT_TYPE_TEXT.equalsIgnoreCase(type)) {
								requestBaiduTranslate(msg.getFrom(),
										msg.getTo(), message_id, from_lang,
										to_lang, msg.getBody());
							}
							break;
						}
					} else {
						String message_id = tttalk.attributeValue("message_id");
						if (message_id == null || "null".equals(message_id)) {
							message_id = String.valueOf(System
									.currentTimeMillis());
							tttalk.addAttribute("message_id", message_id);
						}
					}

				}

				/**
				 * 百度推送，apns推送通知
				 */

				// tttalk
				// old_version_translated
				try {
					String packetId = msg.getID();
					String fromTTTalkId = getTTTalkId(msg.getFrom());
					String toTTTalkId = getTTTalkId(msg.getTo());
					String body = msg.getBody();

					if (tttalk != null) {
						log.info("submitTTTalkJob");

						submitTTTalkJob(packetId, fromTTTalkId, toTTTalkId,
								body, tttalk);
						return;
					}
					Element oldVersion = msg.getChildElement(
							TAG_OLD_VERSION_TRANSLATED, TTTALK_NAMESPACE);
					if (oldVersion != null) {
						log.info("submitOldVersionJob");
						submitOldVersionJob(packetId, toTTTalkId, oldVersion);
						return;
					}

				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}

			}
			deleteMessageFromOfflineTable(msg);
		}
	}

	private void addRequestReceipts(Message message) {
		if (message.getID() == null) {
			message.setID(String.valueOf(System.currentTimeMillis()));
		}
		message.addChildElement(REQUEST_TAG, RECEIVED_NAMASPACE);
	}

	private void saveMessageToOfflineTable(Packet packet, Session session,
			boolean incoming, boolean processed) {
		if (!Utils.getOfflineHandle()) {
			return;
		}

		if ((processed) && (!incoming) && (packet instanceof Message)
				&& isUserAvailable(packet.getTo().getNode())) {
			Message msg = (Message) packet;
			Element received = msg.getChildElement(RECEIVED_TAG,
					RECEIVED_NAMASPACE);
			Element delay = msg.getChildElement(DELAY_TAG, DELAY_NAMASPACE);
			Element volunteer_request = msg.getChildElement(TAG_REQUEST,
					VOLUNTEER_NAMESPACE);
			Element volunteer_cancel = msg.getChildElement(TAG_CANCEL,
					VOLUNTEER_NAMESPACE);
			if (delay == null && received == null && volunteer_request == null
					&& volunteer_cancel == null) {
				log.info(String.format("saveto offline:%s", packet.getID()));
				offlineMessageStore.addMessage((Message) packet);
			}
		}
	}

	private boolean isUserAvailable(String username) {
		boolean result = false;
		try {
			User user = userManager.getUser(username);
			result = presenceManager.isAvailable(user);
		} catch (Exception e) {
		}
		return result;
	}

	private void deleteMessageFromOfflineTable(Message receivedMessage) {
		if (!Utils.getOfflineHandle()) {
			return;
		}
		long start = System.currentTimeMillis();

		Element received = receivedMessage.getChildElement(RECEIVED_TAG,
				RECEIVED_NAMASPACE);
		if (received == null)
			return;
		String username = receivedMessage.getFrom().getNode();
		String receivedId = received.attributeValue("id");

		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			con = DbConnectionManager.getConnection();
			String sql = String
					.format("select messageID from ofOffline where username='%s' and stanza like '%s'",
							username, "%id=\"" + receivedId + "\"%");
			pstmt = con.prepareStatement(sql);

			resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				String msgId = resultSet.getString(1);
				pstmt.close();
				sql = String
						.format("delete from ofOffline where username='%s' and messageID=%s",
								username, msgId);
				pstmt = con.prepareStatement(sql);
				pstmt.execute();
				pstmt.close();
			}
			sql = String
					.format("delete from ofOffline where username='%s' or username='%s'",
							getTranslator(), getVolunteer());
			pstmt = con.prepareStatement(sql);
			pstmt.execute();
			pstmt.close();

		} catch (SQLException e) {
			log.info(e.getMessage(), e);
		} finally {
			DbConnectionManager.closeConnection(resultSet, pstmt, con);
		}
		log.info("delete offline: " + (System.currentTimeMillis() - start));
	}

	private void submitOldVersionJob(String packetId, String toTTTalkId,
			Element oldVersion) throws Exception {
		String function = "push_message";

		String file_type = oldVersion.attributeValue("file_type");
		String from_content = oldVersion.attributeValue("from_content");
		String to_content = oldVersion.attributeValue("to_content");
		String userid = oldVersion.attributeValue("userid");
		String to_lang = oldVersion.attributeValue("to_lang");
		String to_userid = toTTTalkId;

		JSONObject jo = new JSONObject();
		jo.put("app_name", APP_NAME);
		jo.put("userid", userid);
		jo.put("title", TAG_OLD_VERSION_TRANSLATED);
		jo.put("file_type", file_type);
		jo.put("from_content", from_content);
		jo.put("to_content", to_content);
		jo.put("to_lang", to_lang);
		jo.put("user_id", to_userid);

		byte[] data = ByteUtils.toUTF8Bytes(jo.toString());
		String uniqueId = null;
		createBackgroundJob(function, data, uniqueId);
	}

	private void createBackgroundJob(String function, byte[] data,
			String uniqueId) throws IOException {
		GearmanJob job = GearmanJobImpl.createBackgroundJob(function, data,
				uniqueId);
		try {
			gearmanClient.submit(job);
		} catch (Exception e) {
			log.error(e.getMessage());
			gearmanClient = genGearmanClient();
			gearmanClient.submit(job);
		}
	}

	private void submitTTTalkJob(String packetId, String fromTTTalkId,
			String toTTTalkId, String body, Element tttalk) throws Exception {
		String function = "push_message";

		String type = tttalk.attributeValue("type");
		String to_lang = tttalk.attributeValue("to_lang");

		JSONObject jo = new JSONObject();
		jo.put("app_name", APP_NAME);
		jo.put("title", TAG_TTTALK);
		jo.put("type", type);
		jo.put("userid", fromTTTalkId);
		jo.put("user_id", toTTTalkId);
		jo.put("body", body);
		jo.put("to_lang", to_lang);
		jo.put("pid", packetId);
		log.info("submitTTTalkJob: " + jo.toString());

		byte[] data = ByteUtils.toUTF8Bytes(jo.toString());
		String uniqueId = null;
		createBackgroundJob(function, data, uniqueId);
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

	private String getUsernameFromJID(JID jid) {
		String temp = jid.toString();
		return temp.substring(0, temp.indexOf("@"));
	}

	public void requestBaiduTranslate(JID from, JID to, String message_id,
			String from_lang, String to_lang, String text) {

		// new Thread(new BaiduTranslateRunnable(from, to, message_id,
		// from_lang,
		// to_lang, text), "Baidu").start();
		_baiduTranslate(from, to, message_id, from_lang, to_lang, text);
	}

	public class BaiduTranslateRunnable implements Runnable {
		JID from;
		JID to;
		String message_id;
		String from_lang;
		String to_lang;
		String text;

		public BaiduTranslateRunnable(JID from, JID to, String message_id,
				String from_lang, String to_lang, String text) {
			this.from = from;
			this.to = to;
			this.message_id = message_id;
			this.from_lang = from_lang;
			this.to_lang = to_lang;
			this.text = text;
		}

		@Override
		public void run() {
			_baiduTranslate(from, to, message_id, from_lang, to_lang, text);
		}
	}

	private void _baiduTranslate(JID from, JID to, String message_id,
			String from_lang, String to_lang, String text) {
		String userid = getTTTalkId(to);

		final Map<String, String> postParams = new HashMap<String, String>();
		postParams.put("userid", userid);
		postParams.put("loginid", userid);
		postParams.put("from_lang", from_lang);
		postParams.put("to_lang", to_lang);
		postParams.put("text", text);
		try {
			String response = Utils.post(Utils.getBaiduTranslateUrl(),
					postParams);
			String to_content = parseBaiduResponse(response);

			translated(message_id, to.toString(), to_content, "0", "1");
			translated(message_id, from.toString(), to_content, "0", "1");
		} catch (Exception e) {
			log.error("Baidu Exception=" + e.getMessage());
		}
	}

	public class ManualTranslateRunnable implements Runnable {
		JID from;
		JID to;
		String message_id;
		String from_lang;
		String to_lang;
		String text;
		String filePath;
		String filetype;
		String content_length;

		public ManualTranslateRunnable(JID from, JID to, String message_id,
				String from_lang, String to_lang, String text, String filetype,
				String filePath, String content_length) {
			this.from = from;
			this.to = to;
			this.message_id = message_id;
			this.from_lang = from_lang;
			this.to_lang = to_lang;
			this.text = text;
			this.filetype = filetype;
			this.filePath = filePath;
			this.content_length = content_length;
		}

		@Override
		public void run() {
			_manualTranslate(from, to, message_id, from_lang, to_lang, text,
					filetype, filePath, content_length);

		}
	}

	private void increaseChatBadgeCount(JID to) {
		String userid = getTTTalkId(to);

		Map<String, String> postParams = new HashMap<String, String>();

		postParams.put("userid", userid);
		postParams.put("loginid", userid);
		postParams.put("badge_key", "message");

		try {
			log.info("increase badge count=" + userid);
			Utils.post(Utils.getBadgeCountIncreaseUrl(), postParams);
		} catch (Exception e) {
			log.error("increase badge Exception=" + e.getMessage());
		}
	}

	private void _manualTranslate(JID from, JID to, String message_id,
			String from_lang, String to_lang, String text, String filetype,
			String filePath, String content_length) {
		String userid = getTTTalkId(to);

		Map<String, String> postParams = new HashMap<String, String>();

		postParams.put("userid", userid);
		postParams.put("loginid", userid);
		postParams.put("from_lang", from_lang);
		postParams.put("to_lang", to_lang);
		postParams.put("text", text);
		postParams.put("filetype", filetype);
		postParams.put("local_id", message_id);
		postParams.put("content_length", content_length);
		postParams.put("to_userid", getTTTalkId(from));
		if (!CHAT_TYPE_TEXT.equalsIgnoreCase(filetype))
			postParams.put("file_path", filePath);

		try {
			Utils.post(Utils.getManualTranslateUrl(), postParams);
		} catch (Exception e) {
			log.error("Manual Exception=" + e.getMessage());
		}
	}

	public void requestManualTranslate(JID from, JID to, String message_id,
			String from_lang, String to_lang, String text, String filetype,
			String filePath, String content_length) {
		// new Thread(new ManualTranslateRunnable(from, to, message_id,
		// from_lang,
		// to_lang, text, filetype, content_length), "Manual").start();
		_manualTranslate(from, to, message_id, from_lang, to_lang, text,
				filetype, filePath, content_length);
	}
	
	public void requestAddQa(JID from, String question){
    	  String userid = getTTTalkId(from);
          Map<String, String> postParams = new HashMap<String, String>();
          postParams.put("loginid", userid);
          postParams.put("question", question);
          postParams = Utils.genParams(userid, postParams);
          try {
              Utils.get(Utils.getAddQaUrl(), postParams);
          } catch (Exception e) {
              log.error("Exception=" + e.getMessage());
          }
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
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public String getBaiduTranslateUrl() {
		return Utils.getBaiduTranslateUrl();
	}

	public void setBaiduTranslateUrl(String url) {
		Utils.setBaiduTranslateUrl(url);
	}

	public String getManualTranslateUrl() {
		return Utils.getManualTranslateUrl();
	}

	public void setManualTranslateUrl(String url) {
		Utils.setManualTranslateUrl(url);
	}

	public String getProperty(String username, String key, String def) {
		try {
			User user = userManager.getUser(username);
			if (key == null)
				return user.getProperties().toString();
			String value = user.getProperties().get(key);
			if (value == null) {
				value = def;
			}
			return value;
		} catch (UserNotFoundException e) {
			log.error(username, e);
			return def;
		}
	}

	public void updateProperty(String username, String key, String value)
			throws UserNotFoundException {
		User user = userManager.getUser(username);
		user.getProperties().put(key, value);
	}
}
