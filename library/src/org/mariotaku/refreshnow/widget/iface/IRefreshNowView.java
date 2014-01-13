package org.mariotaku.refreshnow.widget.iface;

import org.mariotaku.refreshnow.widget.OnRefreshListener;
import org.mariotaku.refreshnow.widget.RefreshMode;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;

public interface IRefreshNowView {

	public static final String LOGTAG = "RefreshNow";

	public boolean canOverScroll();

	public RefreshMode getRefreshMode();

	public boolean isRefreshing(RefreshMode mode);

	public void setOnRefreshListener(OnRefreshListener listener);

	public void setRefreshComplete();

	public void setRefreshIndicatorView(View view);

	public void setRefreshing(RefreshMode mode, boolean refresh);

	public void setRefreshMode(RefreshMode mode);

	public class Helper implements IRefreshNowView, OnGestureListener {

		private final View mView;
		private OnRefreshListener mRefreshListener;
		private RefreshMode mRefreshMode;
		private static final int MAX_Y_OVERSCROLL_DISTANCE = 48;

		private final int mMaxYOverscrollDistance;

		private int mOverScrollY;

		private final SparseBooleanArray mRefreshState = new SparseBooleanArray();
		private View mIndicatorView;
		private final GestureDetector mGestureDetector;
		private final OverScroller mScroller;
		private boolean mIsDown;

		public Helper(final View view, final Context context, final AttributeSet attrs, final int defStyle) {
			if (!(view instanceof IRefreshNowView))
				throw new IllegalArgumentException("this view instance must implement IRefreshNowView");
			mView = view;
			mGestureDetector = new GestureDetector(context, this);
			final DisplayMetrics metrics = view.getResources().getDisplayMetrics();
			final float density = metrics.density;
			mMaxYOverscrollDistance = (int) (density * MAX_Y_OVERSCROLL_DISTANCE);
			mScroller = new OverScroller(context);
		}

		public void afterOnOverScrolled(final int scrollX, final int scrollY, final boolean clampedX,
				final boolean clampedY) {
			mOverScrollY = scrollY;
		}

		public void beforeOnTouchEvent(final MotionEvent ev) {
			final int action = ev.getAction();
			switch (action) {
				case MotionEvent.ACTION_UP: {
					if (mOverScrollY != 0) {
						cancelTouchEvent();
					}
					if (mIndicatorView != null) {
						((IRefreshNowIndicatorView) mIndicatorView).setIsPulling(false);
					}
					mIsDown = false;
					break;
				}
				case MotionEvent.ACTION_CANCEL: {
					mIsDown = false;
					break;
				}
				case MotionEvent.ACTION_DOWN: {
					if (mIndicatorView != null) {
						((IRefreshNowIndicatorView) mIndicatorView).setIsPulling(true);
					}
					break;
				}
			}
			mGestureDetector.onTouchEvent(ev);
			final int scrollY = mView.getScrollY();
			if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && scrollY != 0) {
				mScroller.springBack(0, scrollY, 0, 0, 0, 0);
				mView.postDelayed(new SpringBackRunnable(mView, mScroller), 16);
			}
		}

		public void beforeOverScrollBy(final int deltaX, final int deltaY, final int scrollX, final int scrollY,
				final int scrollRangeX, final int scrollRangeY, final int maxOverScrollX, final int maxOverScrollY,
				final boolean isTouchEvent) {
			if (Math.abs(scrollY) < mMaxYOverscrollDistance) {
				dispatchPulled(scrollY);
			} else if (scrollY != 0 && isTouchEvent) {
				mOverScrollY = 0;
				cancelTouchEvent();
				dispatchRefreshStart(scrollY);
			}
		}

		public void cancelTouchEvent() {
			final long time = SystemClock.uptimeMillis();
			mView.dispatchTouchEvent(MotionEvent.obtain(time, time, MotionEvent.ACTION_CANCEL, 0, 0, 0));
		}

		@Override
		public boolean canOverScroll() {
			return false;
		}

		public int computeDeltaY(final int deltaY, final int scrollY, final boolean isTouchEvent) {
			if (isTouchEvent && (mRefreshState.get(RefreshMode.FLAG_START) || mRefreshState.get(RefreshMode.FLAG_END)))
				return 0;
			final float pullPercent = Math.abs((float) scrollY) / mMaxYOverscrollDistance;
			final int factor = 2 + Math.round(pullPercent * 3);
			return isTouchEvent ? deltaY / factor : deltaY;
		}

		public void dispatchPulled(final int scrollY) {
			final float pullPercent = Math.abs((float) scrollY) / mMaxYOverscrollDistance;
			if (mIndicatorView != null) {
				((IRefreshNowIndicatorView) mIndicatorView).onPulled(pullPercent);
			}
		}

		public void dispatchRefreshStart(final int scrollY) {
			final RefreshMode refreshMode = scrollY > 0 ? RefreshMode.END : RefreshMode.START;
			if (mRefreshListener != null) {
				mRefreshListener.onRefreshStart(refreshMode);
			}
			if (mIndicatorView != null) {
				((IRefreshNowIndicatorView) mIndicatorView).onRefreshStart();
			}
			((IRefreshNowView) mView).setRefreshing(refreshMode, true);
		}

		public int getMaxYOverscrollDistance() {
			return mMaxYOverscrollDistance;
		}

		public int getOverScrollY() {
			return mOverScrollY;
		}

		@Override
		public RefreshMode getRefreshMode() {
			return mRefreshMode;
		}

		@Override
		public boolean isRefreshing(final RefreshMode mode) {
			boolean result = false;
			if (mode.hasStart()) {
				result |= mRefreshState.get(RefreshMode.FLAG_START);
			}
			if (mode.hasEnd()) {
				result |= mRefreshState.get(RefreshMode.FLAG_END);
			}
			return result;
		}

		@Override
		public boolean onDown(final MotionEvent e) {
			mIsDown = true;
			return true;
		}

		@Override
		public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
			return true;
		}

		@Override
		public void onLongPress(final MotionEvent e) {

		}

		@Override
		public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
			if (((IRefreshNowView) mView).canOverScroll() || !mIsDown) return true;
			mOverScrollY = 0;
			final int deltaY = Math.round(distanceY);
			final int scrollY = mView.getScrollY();
			Log.d(LOGTAG, String.format("Call overScrollVertically, scrollY: %d", scrollY));
			if (Math.abs(scrollY) >= getMaxYOverscrollDistance()) {
				cancelTouchEvent();
				dispatchRefreshStart(scrollY);
			} else {
				mView.scrollBy(0, computeDeltaY(deltaY, scrollY, true));
				dispatchPulled(scrollY);
			}
			return true;
		}

		@Override
		public void onShowPress(final MotionEvent e) {

		}

		@Override
		public boolean onSingleTapUp(final MotionEvent e) {
			return true;
		}

		public void setFriction(final float friction) {
			mScroller.setFriction(friction);
		}

		@Override
		public void setOnRefreshListener(final OnRefreshListener listener) {
			mRefreshListener = listener;
		}

		@Override
		public void setRefreshComplete() {
			int flags = 0;
			if (mRefreshState.get(RefreshMode.FLAG_START)) {
				flags |= RefreshMode.FLAG_START;
			}
			if (mRefreshState.get(RefreshMode.FLAG_END)) {
				flags |= RefreshMode.FLAG_END;
			}
			setRefreshing(RefreshMode.BOTH, false);
			final RefreshMode mode = RefreshMode.valueOf(flags);
			if (mRefreshListener != null && mode != RefreshMode.NONE) {
				mRefreshListener.onRefreshComplete();
			}
			if (mIndicatorView != null) {
				((IRefreshNowIndicatorView) mIndicatorView).onRefreshComplete();
			}
		}

		@Override
		public void setRefreshIndicatorView(final View view) {
			if (!(view instanceof IRefreshNowIndicatorView))
				throw new IllegalArgumentException("this view must implement IRefreshNowIndicatorView");
			mIndicatorView = view;
		}

		@Override
		public void setRefreshing(final RefreshMode mode, final boolean refresh) {
			if (mode.hasStart()) {
				mRefreshState.put(RefreshMode.FLAG_START, refresh);
			}
			if (mode.hasEnd()) {
				mRefreshState.put(RefreshMode.FLAG_END, refresh);
			}
		}

		@Override
		public void setRefreshMode(final RefreshMode mode) {
			mRefreshMode = mode;
		}

		private static class SpringBackRunnable implements Runnable {

			private final View mView;
			private final OverScroller mScroller;

			SpringBackRunnable(final View view, final OverScroller scroller) {
				mView = view;
				mScroller = scroller;
			}

			@Override
			public void run() {
				if (mScroller.computeScrollOffset()) {
					mView.scrollTo(mView.getScrollX(), mScroller.getCurrY());
					mView.postDelayed(this, 16);
				} else {
					mView.scrollTo(mView.getScrollX(), 0);
				}
			}
		}
	}
}