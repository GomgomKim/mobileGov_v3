package com.infrawaretech.docviewer.utils;

import android.content.Context;
import android.graphics.Bitmap;

public class PageUtils {

	public static final int positionToPage(int position) {
		return position + 1;
	}
	
	public static final int pageToPosition(int page) {
		return page - 1;
	}
	
	public static Bitmap createBlankBitmap(Context context){
		Bitmap blank = Bitmap.createBitmap(128, 128, Bitmap.Config.RGB_565);
		int blankColor = context.getResources().getColor(LibraryTool.getColorId(context, "it_dv_color_pagedefault_bg"));
		blank.eraseColor(blankColor);
		return blank;
	}
}
