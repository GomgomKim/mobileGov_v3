package kr.go.mobile.mobp.iff.util;

import java.text.NumberFormat;

import kr.go.mobile.mobp.iff.R;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

@Deprecated
public class SingleProgressDialog{

	private Context context;

	private Dialog dialog;

	private Handler mViewUpdateHandler;

	private ProgressBar mProgress;
	private volatile boolean mProgressIsNull;

	private TextView mProgressPercent;

	private TextView mProgressNumber;

	private String mProgressNumberFormat;

	private NumberFormat mProgressPercentFormat;

	private int mMax;
	private int mProgressVal;
	private int mIncrementBy;
	private Drawable mProgressDrawable;
	private Drawable mIndeterminateDrawable;
	private CharSequence mMessage;
	private boolean mIndeterminate;

	private boolean mHasStarted;

	public SingleProgressDialog(Context c) {
		context = c;
		mProgressIsNull = true;
		setContentView();
	}

	public void setContentView() {

		LayoutInflater inflater = LayoutInflater.from(context);

		mViewUpdateHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);

				/* Update the number and percent */
				int progress = mProgress.getProgress();
				int max = mProgress.getMax();
				double percent = (double) progress / (double) max;
				String format = mProgressNumberFormat;
				mProgressNumber.setText(String.format(format, readByteToString(progress, true), readByteToStringMax(max, false)));
				SpannableString tmp = new SpannableString(mProgressPercentFormat.format(percent));
				tmp.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, tmp.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				mProgressPercent.setText(tmp);

			}
		};
		View view = inflater.inflate(R.layout.alert_dialog_single_progress, null);
		dialog = new Dialog(context, R.style.popupStyle);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		dialog.setContentView(view);
		dialog.setCancelable(false);
		
		mProgress = (ProgressBar) dialog.findViewById(R.id.progress);
		mProgressIsNull = false;

		mProgressNumber = (TextView) dialog.findViewById(R.id.progress_number);
		mProgressNumberFormat = "%s / %s";
		mProgressPercent = (TextView) dialog.findViewById(R.id.progress_percent);
		mProgressPercentFormat = NumberFormat.getPercentInstance();
		mProgressPercentFormat.setMaximumFractionDigits(0);

		if (mMax > 0) {
			setMax(mMax);
		}
		if (mProgressVal > 0) {
			setProgress(mProgressVal);
		}
		if (mIncrementBy > 0) {
			incrementProgressBy(mIncrementBy);
		}
		if (mProgressDrawable != null) {
			setProgressDrawable(mProgressDrawable);
		}
		if (mIndeterminateDrawable != null) {
			setIndeterminateDrawable(mIndeterminateDrawable);
		}

		setIndeterminate(mIndeterminate);
		onProgressChanged();
		
		setProgress(0);
	}
	public void setProgress(int value) {
		mProgress.setProgress(value);
		onProgressChanged();
		mProgressVal = value;
	}



	public boolean isShowing() {
		if (dialog != null && dialog.isShowing())
			return true;
		else
			return false;
	}
	
	public void show(){
		if (dialog != null) {
			if(dialog.isShowing()){
				dialog.dismiss();
			}
			dialog.show();
		}	
	}

	public void cancel() {
		if (dialog != null && dialog.isShowing()) {
			dialog.cancel();
		}
	}

	public void dismiss() {
		if (dialog != null && dialog.isShowing()) {
			dialog.dismiss();
		}
	}

	public int getProgress() {
		if (mProgress != null) {
			return mProgress.getProgress();
		}
		return mProgressVal;
	}

	public int getMax() {
		if (mProgressIsNull) return mMax;
		return mProgress.getMax();
	}

	public void setMax(int max) {
		
		//현재 신규내부의 경우 앱스토어 상세정보의 앱크기와 약간 달라서 계산을 따로..
		float a = (float)1000 / (float)1024;
		float nMaxSize = (float)max * (float)a * (float)a;
		String contentLength = String.format("%.0f", Math.ceil(nMaxSize));	
		
		max = Integer.valueOf(contentLength);

		if (mProgressIsNull) {
			mMax = max;
		}
		else {
			mProgress.setMax(max);
			onProgressChanged();
		}
	}

	public void incrementProgressBy(int diff) {
		if (mProgressIsNull) {
			mIncrementBy += diff;
		}
		else {
			mProgress.incrementProgressBy(diff);
			onProgressChanged();
		}
	}

	public void setProgressDrawable(Drawable d) {
		if (mProgressIsNull) {
			mProgressDrawable = d;
		}
		else {
			mProgress.setProgressDrawable(d);
		}
	}

	public void setIndeterminateDrawable(Drawable d) {
		if (mProgressIsNull) {
			mIndeterminateDrawable = d;
		}
		else {
			mProgress.setIndeterminateDrawable(d);
		}
	}

	public void setIndeterminate(boolean indeterminate) {
		if (mProgressIsNull) {
			mIndeterminate = indeterminate;
		}
		else {
			mProgress.setIndeterminate(indeterminate);
		}
	}

	public boolean isIndeterminate() {
		if (mProgressIsNull) return mIndeterminate;
		return mProgress.isIndeterminate();
	}

	/**
	 * Change the format of Progress Number. The default is "current/max".
	 * Should not be called during the number is progressing.
	 * 
	 * @param format
	 *            Should contain two "%d". The first is used for current number
	 *            and the second is used for the maximum.
	 * @hide
	 */
	public void setProgressNumberFormat(String format) {
		mProgressNumberFormat = format;
	}

	private void onProgressChanged() {
		mViewUpdateHandler.sendEmptyMessage(0);
	}
	
	public String readByteToStringMax(long bytes, boolean si) {

		int unit = si ? 1000 : 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "");

		float d = (float)bytes / (float)1000000;
		String str = String.valueOf(d);
		str = str.substring(0, 4);
		return String.format("%s %sB", str , pre);
	}

	public String readByteToString(long bytes, boolean si) {

		int unit = si ? 1000 : 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
}
