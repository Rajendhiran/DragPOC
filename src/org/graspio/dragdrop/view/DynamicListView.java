package org.graspio.dragdrop.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;

import org.graspio.dragdrop.adapter.ReceverAdapter;
import org.graspio.dragdrop.data.OneItem;

/**
 * The dynamic listview is an extension of listview that supports cell dragging
 * and swapping.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
@SuppressLint("InlinedApi")
public class DynamicListView extends ListView {

	
	private OnTouchListener mOnItemOutUpListener;
	private OnTouchListener mOnItemMoveListener; //It is an hacked touchLister, in fact it is OnMoveListener
	private OnItemClickListener mOnItemReceiver;
	private boolean isMove = false;
	private View childSelected;
	private float xInit;
	private float yInit;
	
	private final int SMOOTH_SCROLL_AMOUNT_AT_EDGE = 15;
	private final int MOVE_DURATION = 150;
	private final int LINE_THICKNESS = 15;

	public ArrayList<OneItem> mCheeseList;

	private int mLastEventY = -1;

	private int mDownY = -1;
	private int mDownX = -1;

	private int mTotalOffset = 0;

	private boolean mCellIsMobile = false;
	private boolean mIsMobileScrolling = false;
	private int mSmoothScrollAmountAtEdge = 0;

	private final int INVALID_ID = -1;
	private long mAboveItemId = INVALID_ID;
	private long mMobileItemId = INVALID_ID;
	private long mBelowItemId = INVALID_ID;

	private BitmapDrawable mHoverCell;
	private Rect mHoverCellCurrentBounds;
	private Rect mHoverCellOriginalBounds;

	private final int INVALID_POINTER_ID = -1;
	private int mActivePointerId = INVALID_POINTER_ID;

	private boolean mIsWaitingForScrollFinish = false;
	private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

	public DynamicListView(Context context) {
		super(context);
		init(context);
	}

	public DynamicListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public DynamicListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public void init(Context context) {
		setOnItemLongClickListener(mOnItemLongClickListener);
		setOnScrollListener(mScrollListener);
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		mSmoothScrollAmountAtEdge = (int) (SMOOTH_SCROLL_AMOUNT_AT_EDGE / metrics.density);
	}

	private AdapterView.OnItemLongClickListener mOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos,
				long id) {
			mTotalOffset = 0;			
			int position = pointToPosition(mDownX, mDownY);
			int itemNum = position ;//- getFirstVisiblePosition();

			View selectedView = getChildAt(itemNum);
			mMobileItemId = getAdapter().getItemId(position);
			mHoverCell = getAndAddHoverView(selectedView);
			selectedView.setVisibility(INVISIBLE);
			mCellIsMobile = true;
			updateNeighborViewsForID(mMobileItemId);
			return true;
		}
	};
	
	
	 /**
	    * When an item is moved horizontally out of this position, this listener is called. Before this OnItemSelectedListener is called.
	    * @param listener
	    */
		public void setOnItemMoveListener(AdapterView.OnTouchListener listener){
			mOnItemMoveListener = listener;
		}

		/**
		 * When a gesture on a item is terminated out of the the listView
		 * @param listener
		 */
		public void setOnItemUpOutListener(AdapterView.OnTouchListener listener){
			this.mOnItemOutUpListener = listener;
		}

		/**
		 * When an outsider item is moved and up on this listview. Return position where to add this item.
		 * @param listener
		 */
		public void setOnItemReceiverListener(AdapterView.OnItemClickListener listener){
			this.mOnItemReceiver = listener;
		}

	private BitmapDrawable getAndAddHoverView(View v) {

		int w = v.getWidth();
		int h = v.getHeight();
		int top = v.getTop();
		int left = v.getLeft();

		Bitmap b = getBitmapWithBorder(v);

		BitmapDrawable drawable = new BitmapDrawable(getResources(), b);

		mHoverCellOriginalBounds = new Rect(left, top, left + w, top + h);
		mHoverCellCurrentBounds = new Rect(mHoverCellOriginalBounds);

		drawable.setBounds(mHoverCellCurrentBounds);

		return drawable;
	}

	private Bitmap getBitmapWithBorder(View v) {
		Bitmap bitmap = getBitmapFromView(v);
		Canvas can = new Canvas(bitmap);

		Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(LINE_THICKNESS);
		paint.setColor(Color.BLACK);

		can.drawBitmap(bitmap, 0, 0, null);
		can.drawRect(rect, paint);

		return bitmap;
	}

	private Bitmap getBitmapFromView(View v) {
		Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(),
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		v.draw(canvas);
		return bitmap;
	}

	private void updateNeighborViewsForID(long itemID) {
		int position = getPositionForID(itemID);
		//StableArrayAdapter adapter = ((StableArrayAdapter) getAdapter());
		ReceverAdapter adapter = (ReceverAdapter) getAdapter();
		mAboveItemId = adapter.getItemId(position - 1);
		mBelowItemId = adapter.getItemId(position + 1);
		adapter.notifyDataSetChanged();
	}

	public View getViewForID(long itemID) {
		int firstVisiblePosition = getFirstVisiblePosition();
		//StableArrayAdapter adapter = ((StableArrayAdapter) getAdapter());
		ReceverAdapter adapter = (ReceverAdapter) getAdapter();
		for (int i = 0; i < getChildCount(); i++) {
			View v = getChildAt(i);
			int position = firstVisiblePosition + i;
			long id = adapter.getItemId(position);
			if (id == itemID) {
				return v;
			}
		}
		return null;
	}

	public int getPositionForID(long itemID) {
		View v = getViewForID(itemID);
		if (v == null) {
			return -1;
		} else {
			return getPositionForView(v);
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if (mHoverCell != null) {
			mHoverCell.draw(canvas);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) 
	{
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			mDownX = (int) event.getX();
			mDownY = (int) event.getY();
			mActivePointerId = event.getPointerId(0);
			break;
		case MotionEvent.ACTION_MOVE:
			if (mActivePointerId == INVALID_POINTER_ID) {
				break;
			}

			int pointerIndex = event.findPointerIndex(mActivePointerId);

			mLastEventY = (int) event.getY(pointerIndex);
			int deltaY = mLastEventY - mDownY;

			if (mCellIsMobile) {
				mHoverCellCurrentBounds.offsetTo(mHoverCellOriginalBounds.left,
						mHoverCellOriginalBounds.top + deltaY + mTotalOffset);
				mHoverCell.setBounds(mHoverCellCurrentBounds);
				invalidate();

				handleCellSwitch();

				mIsMobileScrolling = false;
				handleMobileCellScroll();

				return false;
			}
			break;
		case MotionEvent.ACTION_UP:
			touchEventsEnded();
			break;
		case MotionEvent.ACTION_CANCEL:
			touchEventsCancelled();
			break;
		case MotionEvent.ACTION_POINTER_UP:
			pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			final int pointerId = event.getPointerId(pointerIndex);
			if (pointerId == mActivePointerId) {
				touchEventsEnded();
			}
			break;
		default:
			break;
		}

		return super.onTouchEvent(event);
	}

	
	 /**
	    * 
	    * @param e
	    * @return
	    */
	   public boolean onUpReceive(MotionEvent e) {

			if(e.getAction() == MotionEvent.ACTION_UP){
				
				int x = (int)e.getX();
				int y = (int)e.getY();

				for(int i=0;i<getChildCount();i++){
					Rect viewRect = new Rect();
					View child = getChildAt(i);
					int left = child.getLeft() + this.getLeft();
					int right = child.getRight() + this.getLeft();
					int top = child.getTop() + this.getTop();
					int bottom = child.getTop() + child.getHeight()/2 + this.getTop();
					viewRect.set(left, top, right, bottom);
					
					if(viewRect.contains(x,y)){
						if(getOnItemSelectedListener() != null){
							getOnItemSelectedListener().onItemSelected(DynamicListView.this, child, i, getItemIdAtPosition(i));
						}
						if(mOnItemReceiver != null){
							mOnItemReceiver.onItemClick(DynamicListView.this, child, i, getItemIdAtPosition(i));
						}
						return true;
					}
					
					Rect viewRect2 = new Rect();
					left = child.getLeft() + this.getLeft();
					right = child.getRight() + this.getLeft();
					top = child.getTop()  + child.getHeight()/2 + this.getTop();
					bottom = child.getBottom() + this.getTop();
					viewRect2.set(left, top, right, bottom);
					
					if(viewRect2.contains(x,y)){
						if(getOnItemSelectedListener() != null){
							getOnItemSelectedListener().onItemSelected(DynamicListView.this, child, i, getItemIdAtPosition(i));
						}
						if(mOnItemReceiver != null){
							mOnItemReceiver.onItemClick(DynamicListView.this, child, i + 1, getItemIdAtPosition(i));
						}
						return true;
					}
				}

				int left = this.getLeft();
				int right = this.getRight();
				int top = this.getTop();
				int bottom = this.getBottom();
				Rect rect = new Rect(left, top, right, bottom);

				if(rect.contains(x,y)){

					if(this.getChildCount() > 0){
						int  minY = this.getChildAt(this.getChildCount() - 1).getBottom();
						int maxY = this.getChildAt(0).getTop();

						if(y < minY){
							mOnItemReceiver.onItemClick(DynamicListView.this, null, 0, 0);
						}else{
							if(y > maxY){
								mOnItemReceiver.onItemClick(DynamicListView.this, null, this.getChildCount() , 0);
							}
						}
						return true;

					}else{

						if(mOnItemReceiver != null){
							mOnItemReceiver.onItemClick(DynamicListView.this, null, 0, 0);
						}
						return true;
					}
				}
			}
			return false;
		}
	
	private void handleCellSwitch() {
		final int deltaY = mLastEventY - mDownY;
		int deltaYTotal = mHoverCellOriginalBounds.top + mTotalOffset + deltaY;

		View belowView = getViewForID(mBelowItemId);
		View mobileView = getViewForID(mMobileItemId);
		View aboveView = getViewForID(mAboveItemId);

		boolean isBelow = (belowView != null)
				&& (deltaYTotal > belowView.getTop());
		boolean isAbove = (aboveView != null)
				&& (deltaYTotal < aboveView.getTop());

		if (isBelow || isAbove) {

			final long switchItemID = isBelow ? mBelowItemId : mAboveItemId;
			View switchView = isBelow ? belowView : aboveView;
			final int originalItem = getPositionForView(mobileView);

			if (switchView == null) {
				updateNeighborViewsForID(mMobileItemId);
				return;
			}

			swapElements(mCheeseList, originalItem,
					getPositionForView(switchView));

			//deferNotifyDataSetChanged();
			((BaseAdapter) getAdapter()).notifyDataSetChanged();

			mDownY = mLastEventY;

			final int switchViewStartTop = switchView.getTop();

			mobileView.setVisibility(View.VISIBLE);
			switchView.setVisibility(View.INVISIBLE);

			updateNeighborViewsForID(mMobileItemId);

			final ViewTreeObserver observer = getViewTreeObserver();
			observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
				@SuppressLint("NewApi")
				public boolean onPreDraw() {
					observer.removeOnPreDrawListener(this);

					View switchView = getViewForID(switchItemID);

					mTotalOffset += deltaY;

					int switchViewNewTop = switchView.getTop();
					int delta = switchViewStartTop - switchViewNewTop;

					switchView.setTranslationY(delta);

					ObjectAnimator animator = ObjectAnimator.ofFloat(
							switchView, View.TRANSLATION_Y, 0);
					animator.setDuration(MOVE_DURATION);
					animator.start();

					return true;
				}
			});
		}
	}

	private void swapElements(ArrayList arrayList, int indexOne, int indexTwo) {
		OneItem temp = (OneItem)arrayList.get(indexOne);
		
		arrayList.set(indexOne, arrayList.get(indexTwo));
		arrayList.set(indexTwo, temp);
		
		Log.d("---","----");
		for(int i=0;i<arrayList.size();i++)
		{
			
			Log.d("Item Name : ",""+((OneItem)arrayList.get(i)).getName());
			Log.d("Item ID   : ",""+((OneItem)arrayList.get(i)).getId());
			Log.d("Item Type : ",""+((OneItem)arrayList.get(i)).getType());
			
		}
		Log.d("---","----");
	}

	private void touchEventsEnded() {
		final View mobileView = getViewForID(mMobileItemId);
		if (mCellIsMobile || mIsWaitingForScrollFinish) {
			mCellIsMobile = false;
			mIsWaitingForScrollFinish = false;
			mIsMobileScrolling = false;
			mActivePointerId = INVALID_POINTER_ID;

			if (mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
				mIsWaitingForScrollFinish = true;
				return;
			}

			mHoverCellCurrentBounds.offsetTo(mHoverCellOriginalBounds.left,
					mobileView.getTop());

			ObjectAnimator hoverViewAnimator = ObjectAnimator.ofObject(
					mHoverCell, "bounds", sBoundEvaluator,
					mHoverCellCurrentBounds);
			hoverViewAnimator
					.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
						@Override
						public void onAnimationUpdate(
								ValueAnimator valueAnimator) {
							invalidate();
						}
					});
			hoverViewAnimator.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationStart(Animator animation) {
					setEnabled(false);
				}

				@Override
				public void onAnimationEnd(Animator animation) {
					mAboveItemId = INVALID_ID;
					mMobileItemId = INVALID_ID;
					mBelowItemId = INVALID_ID;
					mobileView.setVisibility(VISIBLE);
					mHoverCell = null;
					setEnabled(true);
					invalidate();
				}
			});
			hoverViewAnimator.start();
		} else {
			touchEventsCancelled();
		}
	}

	private void touchEventsCancelled() {
		View mobileView = getViewForID(mMobileItemId);
		if (mCellIsMobile) {
			mAboveItemId = INVALID_ID;
			mMobileItemId = INVALID_ID;
			mBelowItemId = INVALID_ID;
			mobileView.setVisibility(VISIBLE);
			mHoverCell = null;
			invalidate();
		}
		mCellIsMobile = false;
		mIsMobileScrolling = false;
		mActivePointerId = INVALID_POINTER_ID;
	}

	private final static TypeEvaluator sBoundEvaluator = new TypeEvaluator() {
		public Rect evaluate(float fraction, Rect startValue, Rect endValue) {
			return new Rect(interpolate(startValue.left, endValue.left,
					fraction), interpolate(startValue.top, endValue.top,
					fraction), interpolate(startValue.right, endValue.right,
					fraction), interpolate(startValue.bottom, endValue.bottom,
					fraction));
		}

		public int interpolate(int start, int end, float fraction) {
			return (int) (start + fraction * (end - start));
		}

		@Override
		public Object evaluate(float arg0, Object arg1, Object arg2) {
			// TODO Auto-generated method stub
			return null;
		}
	};

	private void handleMobileCellScroll() {
		mIsMobileScrolling = handleMobileCellScroll(mHoverCellCurrentBounds);
	}

	public boolean handleMobileCellScroll(Rect r) {
		int offset = computeVerticalScrollOffset();
		int height = getHeight();
		int extent = computeVerticalScrollExtent();
		int range = computeVerticalScrollRange();
		int hoverViewTop = r.top;
		int hoverHeight = r.height();

		if (hoverViewTop <= 0 && offset > 0) {
			smoothScrollBy(-mSmoothScrollAmountAtEdge, 0);
			return true;
		}

		if (hoverViewTop + hoverHeight >= height && (offset + extent) < range) {
			smoothScrollBy(mSmoothScrollAmountAtEdge, 0);
			return true;
		}

		return false;
	}

	public void setCheeseList(ArrayList<OneItem> cheeseList) {
		mCheeseList = cheeseList;
	}

	private AbsListView.OnScrollListener mScrollListener = new AbsListView.OnScrollListener()
	{
		private int mPreviousFirstVisibleItem = -1;
		private int mPreviousVisibleItemCount = -1;
		private int mCurrentFirstVisibleItem;
		private int mCurrentVisibleItemCount;
		private int mCurrentScrollState;

		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			mCurrentFirstVisibleItem = firstVisibleItem;
			mCurrentVisibleItemCount = visibleItemCount;

			mPreviousFirstVisibleItem = (mPreviousFirstVisibleItem == -1) ? mCurrentFirstVisibleItem
					: mPreviousFirstVisibleItem;
			mPreviousVisibleItemCount = (mPreviousVisibleItemCount == -1) ? mCurrentVisibleItemCount
					: mPreviousVisibleItemCount;

			checkAndHandleFirstVisibleCellChange();
			checkAndHandleLastVisibleCellChange();

			mPreviousFirstVisibleItem = mCurrentFirstVisibleItem;
			mPreviousVisibleItemCount = mCurrentVisibleItemCount;
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			mCurrentScrollState = scrollState;
			mScrollState = scrollState;
			isScrollCompleted();
		}

		private void isScrollCompleted() {
			if (mCurrentVisibleItemCount > 0
					&& mCurrentScrollState == SCROLL_STATE_IDLE) {
				if (mCellIsMobile && mIsMobileScrolling) {
					handleMobileCellScroll();
				} else if (mIsWaitingForScrollFinish) {
					touchEventsEnded();
				}
			}
		}

		public void checkAndHandleFirstVisibleCellChange() {
			if (mCurrentFirstVisibleItem != mPreviousFirstVisibleItem) {
				if (mCellIsMobile && mMobileItemId != INVALID_ID) {
					updateNeighborViewsForID(mMobileItemId);
					handleCellSwitch();
				}
			}
		}

		public void checkAndHandleLastVisibleCellChange() {
			int currentLastVisibleItem = mCurrentFirstVisibleItem
					+ mCurrentVisibleItemCount;
			int previousLastVisibleItem = mPreviousFirstVisibleItem
					+ mPreviousVisibleItemCount;
			if (currentLastVisibleItem != previousLastVisibleItem) {
				if (mCellIsMobile && mMobileItemId != INVALID_ID) {
					updateNeighborViewsForID(mMobileItemId);
					handleCellSwitch();
				}
			}
		}
	};
}