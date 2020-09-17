package com.infrawaretech.docviewer.ui;

import com.infrawaretech.docviewer.DocConvertException;
import com.infrawaretech.docviewer.DocConvertManager;
import com.infrawaretech.docviewer.utils.PageUtils;

import androidx.viewpager.widget.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.github.chrisbanes.photoview.PhotoView;

import java.util.HashMap;
import java.util.Map;

public class DocPagerAdapter extends PagerAdapter {

  Map<Integer, PhotoView> mDocViews = new HashMap<>();

  private int mTotalCount = 1;

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
    docView.setBackgroundResource(R.color.it_dv_color_pagedefault_bg);
    docView.setMaximumScale(3.0f * 4);
    try {
      int page = PageUtils.positionToPage(position);
      DocConvertManager.getInstance().requestConvertedData(page);
    } catch (DocConvertException e) {
      Log.d("@@@@", e.toString());
    }
    container.addView(docView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    synchronized (mDocViews) {
      mDocViews.put(position, docView);
    }
    return docView;
  }


  public PhotoView getDocView(int position) {
    synchronized (mDocViews) {
      return mDocViews.get(position);
    }
  }

  @Override
  public void destroyItem(ViewGroup container, int position, Object object) {
    synchronized (mDocViews) {
      mDocViews.remove(position);
    }
    container.removeView((View) object);
  }



  @Override
  public boolean isViewFromObject(View view, Object object) {
      return view == object;
  }

}
