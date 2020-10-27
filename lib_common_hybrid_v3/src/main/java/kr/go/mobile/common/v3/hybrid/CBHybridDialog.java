package kr.go.mobile.common.v3.hybrid;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import kr.go.mobile.common.hybrid.R;


/**
 * Created by ChangBum Hong on 2020-07-27.
 * cloverm@infrawareglobal.com
 */
public class CBHybridDialog extends AlertDialog {

    private View progressView;

    public CBHybridDialog(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        progressView = inflater.inflate(R.layout.hybrid_dialog_content_progress, null);
        setView(progressView);
    }

    @Override
    public void setMessage(CharSequence message) {
        TextView msgView = progressView.findViewById(R.id.message);
        msgView.setText(message);
        super.setMessage(null);
    }
}
