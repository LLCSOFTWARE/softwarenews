package org.news.activity;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONObject;
import org.news.service.SyncHttp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class NewsDetailsActivity extends Activity{
	
	private ViewFlipper mNewsBodyFlipper;
	private LayoutInflater mNewsBodyInflater;
	private int mPosition = 0;
	private ArrayList<HashMap<String, Object>> mNewsData;
	private int mNid;
	private TextView mNewsDetails;
	private final int FINISH = 0;
	private int mCursor;
	
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
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.news_details);
		
		Button newsDetailsTitlebarPre = (Button)findViewById(R.id.newsdetails_titlebar_previous);
		NewsDetailsTitleBarOnClickListener newsDetailsTitleBarOnClickListener = new NewsDetailsTitleBarOnClickListener();
		newsDetailsTitlebarPre.setOnClickListener(newsDetailsTitleBarOnClickListener);
		Button newsDetailsTitlebarNext = (Button)findViewById(R.id.newsdetails_titlebar_next);
		newsDetailsTitlebarNext.setOnClickListener(newsDetailsTitleBarOnClickListener);
		
		//获取信息
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		//新闻标题
		String categoryName = bundle.getString("categoryName");
		TextView titleBarTitle = (TextView) findViewById(R.id.newsdetails_titlebar_title);
		titleBarTitle.setText(categoryName);
		//获取位置
		mCursor = mPosition = bundle.getInt("position");
		//获取新闻信息
		Serializable s  = bundle.getSerializable("newsDate");
		mNewsData = ((ArrayList<HashMap<String, Object>>) s);
		//新闻编号
		mNewsBodyInflater = getLayoutInflater();
		inflateView(0);
	}
	
	class NewsDetailsTitleBarOnClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			switch (v.getId())
			{
			//上一条新闻
			case R.id.newsdetails_titlebar_previous:
				showPrevious();
				break;
			//下一条新闻
			case R.id.newsdetails_titlebar_next:
				showNext();
			default:
				break;
			}
			
		}
	}
	
	
	private void showNext()
	{
		if (mPosition<mNewsData.size()-1)
		{
			// 设置下一屏动画
			mNewsBodyFlipper.setInAnimation(this, R.anim.push_left_in);
			mNewsBodyFlipper.setOutAnimation(this, R.anim.push_left_out);
			mPosition++;
			//判断下一屏是否已经创建
			if (mPosition >= mNewsBodyFlipper.getChildCount())
			{
				inflateView(mNewsBodyFlipper.getChildCount());
			}
			// 显示下一屏
			mNewsBodyFlipper.showNext();
		}
		else
		{
			Toast.makeText(this, R.string.no_next_news, Toast.LENGTH_SHORT).show();
		}
	}

	private void showPrevious()
	{
		if (mPosition>0)
		{
			mPosition--;
			//记录当前新闻编号
			HashMap<String, Object> hashMap = mNewsData.get(mPosition);
			mNid = (Integer) hashMap.get("nid");
			if (mCursor>mPosition)
			{
				mCursor = mPosition;
				inflateView(0);
				mNewsBodyFlipper.showNext();// 显示下一页
			}
			mNewsBodyFlipper.setInAnimation(this, R.anim.push_right_in);// 定义下一页进来时的动画
			mNewsBodyFlipper.setOutAnimation(this, R.anim.push_right_out);// 定义当前页出去的动画
			mNewsBodyFlipper.showPrevious();// 显示上一页
		}
		else
		{
			Toast.makeText(this, R.string.no_pre_news, Toast.LENGTH_SHORT).show();
		}
	}
	
	private void inflateView(int index)
	{
		View newsBodyLayout = mNewsBodyInflater.inflate(R.layout.newsbody, null);
		// 获取点击新闻基本信息
		HashMap<String, Object> hashMap = mNewsData.get(mPosition);
		// 新闻标题
		TextView newsTitle = (TextView) newsBodyLayout.findViewById(R.id.news_body_title);
		newsTitle.setText(hashMap.get("newslist_item_title").toString());
		// 发布时间和出处
		TextView newsPtimeAndSource = (TextView) newsBodyLayout.findViewById(R.id.news_body_ptime_source);
		newsPtimeAndSource.setText(hashMap.get("newslist_item_datatime").toString() + "    " + hashMap.get("newslist_item_author").toString());
		// 新闻编号
		mNid = (Integer) hashMap.get("nid");
	
		// 把新闻视图添加到Flipper中
		mNewsBodyFlipper = (ViewFlipper) findViewById(R.id.news_body_flipper);
		mNewsBodyFlipper.addView(newsBodyLayout,index);
	
		// 给新闻Body添加触摸事件
		mNewsDetails = (TextView) newsBodyLayout.findViewById(R.id.news_body_details);
		
	
		// 启动线程
		new UpdateNewsThread().start();
	}
	
	private class UpdateNewsThread extends Thread
	{
		@Override
		public void run()
		{
			// 从网络上获取新闻
			String newsBody = getNewsBody();
			Message msg = mHandler.obtainMessage();
			msg.arg1 = FINISH;
			msg.obj = newsBody;
			mHandler.sendMessage(msg);
		}
	}
	
	private String getNewsBody()
	{
		String retStr = "网络连接失败，请稍后再试";
		SyncHttp syncHttp = new SyncHttp();
		String url = "http://10.0.2.2:8080/web/getNews";
		String params = "nid=" + mNid;
		try
		{
			String retString = syncHttp.httpGet(url, params);
			JSONObject jsonObject = new JSONObject(retString);
			//获取返回码，0表示成功
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

