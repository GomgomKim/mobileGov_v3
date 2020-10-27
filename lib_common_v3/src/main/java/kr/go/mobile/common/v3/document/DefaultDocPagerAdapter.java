package kr.go.mobile.common.v3.document;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;

import com.github.chrisbanes.photoview.PhotoView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import kr.go.mobile.common.R;

public class DefaultDocPagerAdapter extends PagerAdapter {

    interface DocPagerEventListener {
        void destroyItem(int page);
    }

    Map<Integer, PhotoView> mDocViews = Collections.synchronizedMap(new HashMap<Integer, PhotoView>());
    DocConvertManager docConvertManager;
    private DocPagerEventListener eventListener;
    private int mTotalCount = 1;

    public DefaultDocPagerAdapter(DocConvertManager docConvertManager, DocPagerEventListener listener) {
        this.docConvertManager = docConvertManager;
        this.eventListener = listener;
    }

    public void totalCount(int count) {
        this.mTotalCount = count;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mTotalCount;
    }

    @Override
    public View instantiateItem(ViewGroup container, int position) {
        PhotoView docView = new PhotoView(container.getContext());
        docView.setBackgroundResource(R.color.cb_dv_color_page_default_bg);
        docView.setMaximumScale(3.0f * 4);
        docView.setAdjustViewBounds(true);
        try {
            int page = position + 1;
            docConvertManager.requestConvertedData(page);
        } catch (DocConvertManager.DocConvertException e) {
            docConvertManager.failedRequest(e);
        }
        container.addView(docView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mDocViews.put(position, docView);
        return docView;
    }


    public PhotoView getDocView(int position) {
        return mDocViews.get(position);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        mDocViews.remove(position);
        container.removeView((View) object);
        eventListener.destroyItem(position + 1);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

}
