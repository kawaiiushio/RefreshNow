package org.mariotaku.refreshnow.widget;

import org.mariotaku.refreshnow.widget.iface.IRefreshNowView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

public class RefreshNowListView extends ListView implements IRefreshNowView {

	private final Helper mHelper;

	public RefreshNowListView(final Context context) {
		this(context, null);
	}

	public RefreshNowListView(final Context context, final AttributeSet attrs) {
		this(context, attrs, android.R.attr.listViewStyle);
	}

	public RefreshNowListView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		mHelper = new Helper(this, context, attrs, defStyle);
	}

	@Override
	public boolean canOverScroll() {
		final int childCount = getChildCount(), count = getCount();
		if (childCount == 0) return false;
		if (count > childCount)
			return true;
		else {
			final View firstVisibleChild = getChildAt(0);
			final View lastVisibleChild = getChildAt(childCount - 1);
			return firstVisibleChild.getTop() < 0 || lastVisibleChild.getBottom() > getBottom();
		}
	}

	@Override
	public RefreshMode getRefreshMode() {
		return mHelper.getRefreshMode();
	}

	@Override
	public boolean isOverScrolling() {
		if (!canOverScroll()) return false;
		return getScrollY() != 0;
	}

	@Override
	public boolean isRefreshing() {
		return mHelper.isRefreshing();
	}

	@Override
	public boolean onTouchEvent(final MotionEvent ev) {
		mHelper.beforeOnTouchEvent(ev);
		return super.onTouchEvent(ev);
	}

	@Override
	public void scrollBy(final int x, final int y) {
		if (canOverScroll()) return;
		super.scrollBy(x, y);
	}

	@Override
	public void scrollTo(final int x, final int y) {
		if (canOverScroll()) return;
		super.scrollTo(x, y);
	}

	@Override
	public void setFriction(final float friction) {
		super.setFriction(friction);
		mHelper.setFriction(friction);
	}

	@Override
	public void setOnRefreshListener(final OnRefreshListener listener) {
		mHelper.setOnRefreshListener(listener);
	}

	@Override
	public void setRefreshComplete() {
		mHelper.setRefreshComplete();
	}

	@Override
	public void setRefreshIndicatorView(final View view) {
		mHelper.setRefreshIndicatorView(view);
	}

	@Override
	public void setRefreshing(final boolean refreshing) {
		mHelper.setRefreshing(refreshing);
	}

	@Override
	public void setRefreshMode(final RefreshMode mode) {
		mHelper.setRefreshMode(mode);
	}

	@Override
	protected void onOverScrolled(final int scrollX, final int scrollY, final boolean clampedX, final boolean clampedY) {
		super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
		mHelper.dispatchOnOverScrolled(scrollX, scrollY, clampedX, clampedY);
	}

	@Override
	protected void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		mHelper.dispatchOnScrollChanged(l, t, oldl, oldt);
	}

	@Override
	protected boolean overScrollBy(final int deltaX, final int deltaY, final int scrollX, final int scrollY,
			final int scrollRangeX, final int scrollRangeY, final int maxOverScrollX, final int maxOverScrollY,
			final boolean isTouchEvent) {
		if (!canOverScroll()) return true;
		mHelper.beforeOverScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX,
				maxOverScrollY, isTouchEvent);
		final int computedDy = mHelper.computeDeltaY(deltaY, scrollY, isTouchEvent);
		return super.overScrollBy(deltaX, computedDy, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX,
				mHelper.getMaxYOverscrollDistance(), isTouchEvent);
	}

}