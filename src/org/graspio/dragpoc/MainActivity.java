
package org.graspio.dragpoc;

import java.util.ArrayList;

import org.graspio.dragdrop.adapter.ItemAdapter;
import org.graspio.dragdrop.adapter.ReceverAdapter;
import org.graspio.dragdrop.data.OneItem;
import org.graspio.dragdrop.view.DynamicListView;
import org.graspio.dragdrop.view.ListViewDragDrop;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

public class MainActivity extends Activity{

	private RelativeLayout mainRelativeLayout;
	private ListViewDragDrop lvPicture;
	private DynamicListView lvRecever;
	private ItemAdapter itemAdapter;
	private ReceverAdapter receverAdapter;
	private ImageView imageDrag;
	private OneItem oneItemSelected;
	private Context context;

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		context = this;
		init();
		process();
	}

	private void init() 
	{
		mainRelativeLayout = (RelativeLayout)RelativeLayout.inflate(context, R.layout.main_list, null);
		lvPicture = (ListViewDragDrop) mainRelativeLayout.findViewById(R.id.listView1);
		lvRecever = (DynamicListView) mainRelativeLayout.findViewById(R.id.listView2);
		imageDrag = (ImageView) mainRelativeLayout.findViewById(R.id.imageView1);
		setContentView(mainRelativeLayout);
	}

	private void process() 
	{
		itemAdapter = new ItemAdapter(context, constructList());
		receverAdapter = new ReceverAdapter(context, new ArrayList<OneItem>());

		lvPicture.setAdapter(itemAdapter);
		lvRecever.setAdapter(receverAdapter);

		//Set selected Listener to know what item must be drag
		lvPicture.setOnItemSelectedListener(mOnItemSelectedListener);

		//Set an touch listener to know what is the position when item are move out of the listview		
		lvPicture.setOnItemMoveListener(mOnItemMoveListener);

		// Listener to know if the item is droped out of this origin ListView
		lvPicture.setOnItemUpOutListener(mOnItemUpOutListener);

		//Listener to know on what position the new item must be insert
		lvRecever.setOnItemReceiverListener(listenerReceivePicture);
		lvRecever.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		//An listemer to remove an item
		//lvRecever.setOnItemLongClickListener(listenerRemoveItem);
	}

	//Save selected item
	private AdapterView.OnItemSelectedListener mOnItemSelectedListener = new AdapterView.OnItemSelectedListener() 
	{
		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,long arg3) 
		{
			//retrieve selected item from adapterview
			oneItemSelected = (OneItem) arg0.getItemAtPosition(arg2);
			imageDrag.setImageDrawable(context.getResources().getDrawable(oneItemSelected.getId()));
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
		}
	};

	private OnTouchListener mOnItemUpOutListener = new  OnTouchListener() 
	{
		@Override
		public boolean onTouch(View v, MotionEvent event) 
		{
			return lvRecever.onUpReceive(event);
		}
	};


	private OnItemClickListener listenerReceivePicture = new OnItemClickListener() 
	{
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			if(oneItemSelected != null)
			{				
				receverAdapter.addPicture(oneItemSelected, arg2);		
				lvRecever.setCheeseList(receverAdapter.getPicture());
			}
		}
	};

	private OnItemLongClickListener listenerRemoveItem = new OnItemLongClickListener() 
	{
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			//receverAdapter.removeItem(arg2);
			return false;
		}
	};

	private OnTouchListener mOnItemMoveListener = new  OnTouchListener() 
	{
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			RelativeLayout.LayoutParams layout = (RelativeLayout.LayoutParams) imageDrag.getLayoutParams();
			imageDrag.setVisibility(ImageView.VISIBLE);

			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				imageDrag.bringToFront();
				return true;
			}

			if (event.getAction()==MotionEvent.ACTION_MOVE) {
				layout.leftMargin = (int) event.getX() - imageDrag.getWidth()/2;    		
				layout.topMargin = (int) event.getY() - imageDrag.getHeight()/2;
			}

			if (event.getAction()==MotionEvent.ACTION_UP) {
				imageDrag.setVisibility(View.GONE);
			}

			imageDrag.setLayoutParams(layout);
			return true;
		}
	};

	private ArrayList<OneItem> constructList()
	{
		ArrayList<OneItem> al = new ArrayList<OneItem>();

		OneItem op = new OneItem(R.drawable.img1, "One",1);
		al.add(op);

		OneItem op2 = new OneItem(R.drawable.img2, "Two",1);
		al.add(op2);

		OneItem op3 = new OneItem(R.drawable.img3, "Three",1);
		al.add(op3);

		OneItem op4 = new OneItem(R.drawable.img4, "Four",1);
		al.add(op4);		

		return al;
	}
}
