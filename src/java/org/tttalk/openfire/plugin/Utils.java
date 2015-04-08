package org.tttalk.openfire.plugin;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kevinsawicki.http.HttpRequest;

public class Utils {
	private static final Logger log = LoggerFactory.getLogger(Utils.class);

	private static final String BAIDU_TRANSLATE_URL = "tttalk.baidu.url";
	private static final String MANUAL_TRANSLATE_URL = "tttalk.manual.url";
	private static final String TTTALK_APP_SECRET = "tttalk.app.secret";
	private static final String TTTALK_GEARMAN_HOST = "tttalk.gearman.host";
	private static final String TTTALK_GEARMAN_PORT = "tttalk.gearman.prt";

	private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	private static String getSource() {
		return "iOS2.6.0";
	}

	public static String genSign(Map<String, String> params, String appkey) {
		// sign
		StringBuilder sb = new StringBuilder();
		sb.append(appkey);

		String[] keyArray = params.keySet().toArray(new String[params.size()]);
		Arrays.sort(keyArray);

		for (String key : keyArray) {
			String value = params.get(key);
			if (!Utils.isEmpty(value)) {
				sb.append(key).append(value);
			}
		}
		sb.append(getAppSecret());

		String sign = Utils.sha1(sb.toString());

		return sign;
	}

	private static String getFormattedText(byte[] bytes) {
		int len = bytes.length;
		StringBuilder buf = new StringBuilder(len * 2);

		for (int j = 0; j < len; j++) {
			buf.append(HEX_DIGITS[(bytes[j] >> 4) & 0x0f]);
			buf.append(HEX_DIGITS[bytes[j] & 0x0f]);
		}
		return buf.toString();
	}

	public static String sha1(String str) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
			messageDigest.update(str.getBytes());
			return getFormattedText(messageDigest.digest());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isEmpty(String s) {
		return s == null || s.length() == 0 || s.equals("null");
	}

	public static Map<String, String> genParams(String loginid,
			Map<String, String> params) {
		if (params == null) {
			params = new HashMap<String, String>();
		}

		params.put("source", getSource());
		params.put("loginid", loginid);
		String sign = Utils.genSign(params, loginid);
		params.put("sign", sign);

		return params;
	}

	public static String encodeParameters(Map<String, String> params) {
		StringBuffer buf = new StringBuffer();
		String[] keyArray = params.keySet().toArray(new String[0]);
		Arrays.sort(keyArray);
		int j = 0;
		for (String key : keyArray) {
			String value = params.get(key);
			if (j++ != 0) {
				buf.append("&");
			}
			if (!Utils.isEmpty(value)) {
				try {
					buf.append(URLEncoder.encode(key, "UTF-8")).append("=")
							.append(URLEncoder.encode(value, "UTF-8"));
				} catch (java.io.UnsupportedEncodingException neverHappen) {
					// throw new RuntimeException(neverHappen.getMessage(),
					// neverHappen);
				}
			}
		}

		return buf.toString();
	}

	public static String get(String url, Map<String, String> params) {
		url += "?" + Utils.encodeParameters(params);

		log.info("get request=" + url);
		String body = HttpRequest.get(url).body();
		log.info("get response=" + body);
		return body;
	}

	public static String post(String url, Map<String, String> params) {
		log.info("get request=" + url);
		params = Utils.genParams(params.get("loginid"), params);

		logParameters(params);
		String body = HttpRequest.post(url).form(params).body();
		log.info("get response=" + body);
		return body;
	}

	public static String getAppSecret() {
		return JiveGlobals.getProperty(TTTALK_APP_SECRET,
				"2a9304125e25edaa5aff574153eafc95c97672c6");
	}

	public static String getGearmanHost() {
		return JiveGlobals.getProperty(TTTALK_GEARMAN_HOST, "115.68.24.72");
	}

	public static int getGearmanPort() {
		return JiveGlobals.getIntProperty(TTTALK_GEARMAN_PORT, 4730);
	}

	public static void setBaiduTranslateUrl(String url) {
		JiveGlobals.setProperty(BAIDU_TRANSLATE_URL, url);
	}

	public static String getBaiduTranslateUrl() {
		return JiveGlobals
				.getProperty(
						BAIDU_TRANSLATE_URL,
						"http://app.tttalk.org/tttalk150214/message/message_request_baidu_translate.php");
	}

	public static void setManualTranslateUrl(String url) {
		JiveGlobals.setProperty(MANUAL_TRANSLATE_URL, url);
	}

	public static String getManualTranslateUrl() {
		return JiveGlobals
				.getProperty(MANUAL_TRANSLATE_URL,
						"http://app.tttalk.org/tttalk150214/message/message_request_translate.php");
	}

	public static String getClientId(String address) {
		return address
				.substring(address.indexOf("_") + 1, address.indexOf("@"));
	}

	public static String getAppName(String address) {
		return address.substring(0, address.indexOf("_"));
	}

	public static void logParameters(Map<String, String> map) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			String value = entry.getValue();
			log.info(String.format("[%s] = {%s}", entry.getKey(), value));
		}
	}
}
