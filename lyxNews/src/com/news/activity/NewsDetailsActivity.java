package com.news.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.news.service.SyncHttp;
import com.szy.news.activity.R;


public class NewsDetailsActivity extends Activity
{
	private final int FINISH = 0;

	private ViewFlipper mNewsBodyFlipper;
	private LayoutInflater mNewsBodyInflater;
	private float mStartX;
	private ArrayList<HashMap<String, Object>> mNewsData;
	private int mPosition = 0;
	private int mCursor;
	private int mNid;
	private TextView mNewsDetails;
	
	private Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.arg1)
			{
			case FINISH:
				
				mNewsDetails.setText(Html.fromHtml(msg.obj.toString()));
				break;
			}
		}
	};

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.newsdetails);

		NewsDetailsTitleBarOnClickListener newsDetailsTitleBarOnClickListener = new NewsDetailsTitleBarOnClickListener();
		
		Button newsDetailsTitlebarPref = (Button) findViewById(R.id.newsdetails_titlebar_previous);
		newsDetailsTitlebarPref.setOnClickListener(newsDetailsTitleBarOnClickListener);
		
		Button newsDetailsTitlebarNext = (Button) findViewById(R.id.newsdetails_titlebar_next);
		newsDetailsTitlebarNext.setOnClickListener(newsDetailsTitleBarOnClickListener);
		
		
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		
		String categoryName = bundle.getString("categoryName");
		TextView titleBarTitle = (TextView) findViewById(R.id.newsdetails_titlebar_title);
		titleBarTitle.setText(categoryName);
		
		Serializable s = bundle.getSerializable("newsDate");
		mNewsData = (ArrayList<HashMap<String, Object>>) s;
		
		mCursor = mPosition = bundle.getInt("position");

		
		mNewsBodyInflater = getLayoutInflater();
		inflateView(0);
	}

	/**
	 * 处理NewsDetailsTitleBar点击事件
	 */
	class NewsDetailsTitleBarOnClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			switch (v.getId())
			{
			
			case R.id.newsdetails_titlebar_previous:
				showPrevious();
				break;
			
			case R.id.newsdetails_titlebar_next:
				showNext();
				break;
			}

		}
	}

	/**
	 * 处理新闻NewsBody触摸事件
	 */
	class NewsBodyOnTouchListener implements OnTouchListener
	{
		@Override
		public boolean onTouch(View v, MotionEvent event)
		{
			switch (event.getAction())
			{
			
			case MotionEvent.ACTION_DOWN:
				
				mStartX = event.getX();
				break;
			
			case MotionEvent.ACTION_UP:
				
				if (event.getX() < mStartX)
				{
					showPrevious();
				}
				
				else if (event.getX() > mStartX)
				{
					showNext();
				}
				break;
			}
			return true;
		}
	}

	/**
	 * 显示下一条新闻
	 */
	private void showNext()
	{
		
		if (mPosition<mNewsData.size()-1)
		{
			
			mNewsBodyFlipper.setInAnimation(this, R.anim.push_left_in);
			mNewsBodyFlipper.setOutAnimation(this, R.anim.push_left_out);
			mPosition++;
			
			if (mPosition >= mNewsBodyFlipper.getChildCount())
			{
				inflateView(mNewsBodyFlipper.getChildCount());
			}
			
			mNewsBodyFlipper.showNext();
		}
		else
		{
			Toast.makeText(this, R.string.no_next_news, Toast.LENGTH_SHORT).show();
		}
		System.out.println(mCursor +";"+mPosition);
	}

	private void showPrevious()
	{
		if (mPosition>0)
		{
			mPosition--;
			
			HashMap<String, Object> hashMap = mNewsData.get(mPosition);
			mNid = (Integer) hashMap.get("nid");
			if (mCursor>mPosition)
			{
				mCursor = mPosition;
				inflateView(0);
				System.out.println(mNewsBodyFlipper.getChildCount());
				mNewsBodyFlipper.showNext();
			}
			mNewsBodyFlipper.setInAnimation(this, R.anim.push_right_in);
			mNewsBodyFlipper.setOutAnimation(this, R.anim.push_right_out);
			mNewsBodyFlipper.showPrevious();
		}
		else
		{
			Toast.makeText(this, R.string.no_pre_news, Toast.LENGTH_SHORT).show();
		}
		System.out.println(mCursor +";"+mPosition);
	}

	private void inflateView(int index)
	{
		
		View newsBodyLayout = mNewsBodyInflater.inflate(R.layout.news_body, null);
		
		HashMap<String, Object> hashMap = mNewsData.get(mPosition);
		
		TextView newsTitle = (TextView) newsBodyLayout.findViewById(R.id.news_body_title);
		newsTitle.setText(hashMap.get("newslist_item_title").toString());
		
		TextView newsPtimeAndSource = (TextView) newsBodyLayout.findViewById(R.id.news_body_ptime_source);
		newsPtimeAndSource.setText(hashMap.get("newslist_item_ptime").toString() + "    " + hashMap.get("newslist_item_source").toString());
		
		mNid = (Integer) hashMap.get("nid");
		
		mNewsBodyFlipper = (ViewFlipper) findViewById(R.id.news_body_flipper);
		mNewsBodyFlipper.addView(newsBodyLayout,index);
	
		
		mNewsDetails = (TextView) newsBodyLayout.findViewById(R.id.news_body_details);
		mNewsDetails.setOnTouchListener(new NewsBodyOnTouchListener());
	
		
		new UpdateNewsThread().start();
	}

	private class UpdateNewsThread extends Thread
	{
		@Override
		public void run()
		{
			
			String newsBody = getNewsBody();
			Message msg = mHandler.obtainMessage();
			msg.arg1 = FINISH;
			msg.obj = newsBody;
			mHandler.sendMessage(msg);
		}
	}

	/**
	 * 获取新闻详细信息
	 * 
	 * @return
	 */
	private String getNewsBody()
	{
		String retStr = "网络连接失败，请稍后再试";
		SyncHttp syncHttp = new SyncHttp();
		String url = "http://192.168.191.1:8080/web/getNews";
		String params = "nid=" + mNid;
		try
		{
			String retString = syncHttp.httpGet(url, params);
			JSONObject jsonObject = new JSONObject(retString);
			
			int retCode = jsonObject.getInt("ret");
			if (0 == retCode)
			{
				JSONObject dataObject = jsonObject.getJSONObject("data");
				JSONObject newsObject = dataObject.getJSONObject("news");
				retStr = newsObject.getString("body");
			}

		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return retStr;
	}
}
