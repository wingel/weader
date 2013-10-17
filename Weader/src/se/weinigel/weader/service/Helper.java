package se.weinigel.weader.service;


class Helper {
	public static String getMethodName() {
		return Thread.currentThread().getStackTrace()[3].getMethodName();
	}
}
