package com.xebia.xcoss.axcv.util;



public class StringUtil {

	public static final int LEN = 80;
	
	public static boolean isEmpty(String value) {
		return value == null || value.trim().length() == 0;
	}

	public static int getFirstInteger(String value) {
		StringBuilder sb = new StringBuilder();
		char[] charArray = value.toCharArray();
		for (int i = 0; i < charArray.length; i++) {
			if ( charArray[i] >= '0' && charArray[i] <= '9' ) {
				sb.append(charArray[i]);
			} else {
				break;
			}
		}
		return ( sb.length() == 0 ? 0 : Integer.parseInt(sb.toString()));
	}

	public static String getExceptionMessage(Throwable e) {
		String message = e.getMessage();
		if ( StringUtil.isEmpty(message)) {
			message = e.getClass().getSimpleName().replaceFirst("Exception", "");
		}
		if ( message.length() > LEN ) {
			message = message.substring(0, LEN) + "...";
		}
		return message;
	}
}
