package com.infrawaretech.docviewer.utils;

import android.content.Context;

@Deprecated
public class LibraryTool {

	public static int getLayoutId(Context context, String a) {
		return z(context, a, "layout");
	}
	
	public static int getId(Context context, String a) {
		return z(context, a, "id");
	}
	
	public static int getMenuId(Context context, String a) {
		return z(context, a, "menu");
	}
	
	public static int getStringId(Context context, String a) {
		return z(context, a, "string");
	}
	
	public static int getIntegerId(Context context, String a) {
		return z(context, a, "integer");
	}
	
	public static int getColorId(Context context, String a) {
		return z(context, a, "color");
	}
	
	private static int z(Context context, String z, String t) {
		return context.getResources().getIdentifier(z, t, context.getPackageName());
	}
	
}
