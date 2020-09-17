package kr.go.mobile.mobp.mff.lib.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.ProgressBar;

public class Progress extends Dialog
{
	public static Progress dialog = null;
	
	public static Progress show(Context context, CharSequence title, CharSequence message){
		return show(context, title, message, false);
	}

	public static Progress show(Context context, CharSequence title, CharSequence message, boolean indeterminate){
		return show(context, title, message, indeterminate, false);
	}

	public static Progress show(Context context, CharSequence title, CharSequence message,
			boolean indeterminate, boolean cancelable){
		return show(context, title, message, indeterminate, cancelable, null);
	}

	public static Progress show(Context context, CharSequence title, CharSequence message,
			boolean indeterminate, boolean cancelable, OnCancelListener cancelListener){
		if(dialog != null && dialog.isShowing()){
			dialog.dismiss();
		}
		dialog = new Progress(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		//dialog.setTitle(title);
		dialog.setCancelable(cancelable);
		dialog.setOnCancelListener(cancelListener);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


		dialog.addContentView(new ProgressBar(context), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		dialog.show();
		return dialog;
	}
	
	public static void progressDismiss(){
		if(dialog != null){
			dialog.dismiss();
		}
	}

	public Progress(Context context){
		super(context);
	}
}
