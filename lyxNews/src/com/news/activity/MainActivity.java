package com.news.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.news.custom.Category;
import com.news.custom.CustomSimpleAdapter;
import com.news.service.SyncHttp;
import com.news.util.DensityUtil;
import com.news.util.StringUtil;
import com.szy.news.activity.R;

public class MainActivity extends Activity
{
	private final int COLUMNWIDTHPX = 55;
	private final int FLINGVELOCITYPX = 800; // 滚动距离
	private final int NEWSCOUNT = 5; //返回新闻数目
	private final int SUCCESS = 0;//加载成功
	private final int NONEWS = 1;//该栏目下没有新闻
	private final int NOMORENEWS = 2;//该栏目下没有更多新闻
	private final int LOADERROR = 3;//加载失败
	
	private int mColumnWidthDip;
	private int mFlingVelocityDip;
	private int mCid;
	private String mCatName;
	private ArrayList<HashMap<String, Object>> mNewsData;
	private ListView mNewsList;
	private SimpleAdapter mNewsListAdapter;
	private LayoutInflater mInflater;
	private Button mTitlebarRefresh;
	private ProgressBar mLoadnewsProgress;
	private Button mLoadMoreBtn;
	
	private LoadNewsAsyncTask loadNewsAsyncTask;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mInflater = getLayoutInflater();
		mNewsData = new ArrayList<HashMap<String,Object>>();
		mNewsList = (ListView)findViewById(R.id.newslist);
		mTitlebarRefresh = (Button)findViewById(R.id.titlebar_refresh);
		mLoadnewsProgress = (ProgressBar)findViewById(R.id.loadnews_progress);
		mTitlebarRefresh.setOnClickListener(loadMoreListener);
		
		
		mColumnWidthDip = DensityUtil.px2dip(this, COLUMNWIDTHPX);
		mFlingVelocityDip = DensityUtil.px2dip(this, FLINGVELOCITYPX);
		
		
		String[] categoryArray = getResources().getStringArray(R.array.categories);
		
		final List<HashMap<String, Category>> categories = new ArrayList<HashMap<String, Category>>();
		
		for(int i=0;i<categoryArray.length;i++)
		{
			String[] temp = categoryArray[i].split("[|]");
			if (temp.length==2)
			{
				int cid = StringUtil.String2Int(temp[0]);
				String title = temp[1];
				Category type = new Category(cid, title);
				HashMap<String, Category> hashMap = new HashMap<String, Category>();
				hashMap.put("category_title", type);
				categories.add(hashMap);
			}
		}
		
		mCid = 1;
		mCatName ="焦点";
		
		CustomSimpleAdapter categoryAdapter = new CustomSimpleAdapter(this, categories, R.layout.category_title, new String[]{"category_title"}, new int[]{R.id.category_title});
		
		GridView category = new GridView(this);
		category.setColumnWidth(mColumnWidthDip);
		category.setNumColumns(GridView.AUTO_FIT);
		category.setGravity(Gravity.CENTER);
		
		category.setSelector(new ColorDrawable(Color.TRANSPARENT));
		
		int width = mColumnWidthDip * categories.size();
		LayoutParams params = new LayoutParams(width, LayoutParams.FILL_PARENT);
		
		category.setLayoutParams(params);
		
		category.setAdapter(categoryAdapter);
		
		LinearLayout categoryList = (LinearLayout) findViewById(R.id.category_layout);
		categoryList.addView(category);
		
		category.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				TextView categoryTitle;
				
				for (int i = 0; i < parent.getChildCount(); i++)
				{
					categoryTitle = (TextView) (parent.getChildAt(i));
					categoryTitle.setBackgroundDrawable(null);
					categoryTitle.setTextColor(0XFFADB2AD);
				}
				
				categoryTitle = (TextView) (parent.getChildAt(position));
				categoryTitle.setBackgroundResource(R.drawable.categorybar_item_background);
				categoryTitle.setTextColor(0XFFFFFFFF);
				
				mCid = categories.get(position).get("category_title").getCid();
				mCatName = categories.get(position).get("category_title").getTitle();
				
				loadNewsAsyncTask = new LoadNewsAsyncTask();
				loadNewsAsyncTask.execute(mCid,0,true);
			}
		});
		
		
		final HorizontalScrollView categoryScrollview = (HorizontalScrollView) findViewById(R.id.category_scrollview);
		Button categoryArrowRight = (Button) findViewById(R.id.category_arrow_right);
		categoryArrowRight.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				categoryScrollview.fling(DensityUtil.px2dip(MainActivity.this, mFlingVelocityDip));
			}
		});
		
		
		mNewsListAdapter = new SimpleAdapter(this, mNewsData, R.layout.newslist_item, 
										new String[]{"newslist_item_title","newslist_item_digest","newslist_item_source","newslist_item_ptime"}, 
										new int[]{R.id.newslist_item_title,R.id.newslist_item_digest,R.id.newslist_item_source,R.id.newslist_item_ptime});
		View loadMoreLayout = mInflater.inflate(R.layout.loadmore, null);
		mNewsList.addFooterView(loadMoreLayout);
		mNewsList.setAdapter(mNewsListAdapter);
		mNewsList.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				Intent intent = new Intent(MainActivity.this, NewsDetailsActivity.class);
				
				intent.putExtra("newsDate", mNewsData);
				intent.putExtra("position", position);
				intent.putExtra("categoryName", mCatName);
				startActivity(intent);
			}
		});
		
		mLoadMoreBtn = (Button)findViewById(R.id.loadmore_btn);
		mLoadMoreBtn.setOnClickListener(loadMoreListener);
		
		loadNewsAsyncTask = new LoadNewsAsyncTask();
		loadNewsAsyncTask.execute(mCid,0,true);
	}
	
	
	private int getSpeCateNews(int cid,List<HashMap<String, Object>> newsList,int startnid,Boolean firstTimes)
	{
		if (firstTimes)
		{
			
			newsList.clear();
		}
		
		String url = "http://192.168.191.1:8080/web/getSpecifyCategoryNews";
		String params = "startnid="+startnid+"&count="+NEWSCOUNT+"&cid="+cid;
		SyncHttp syncHttp = new SyncHttp();
		try
		{
			
			String retStr = syncHttp.httpGet(url, params);
			JSONObject jsonObject = new JSONObject(retStr);
			
			int retCode = jsonObject.getInt("ret");
			if (0==retCode)
			{
				JSONObject dataObject = jsonObject.getJSONObject("data");
				
				int totalnum = dataObject.getInt("totalnum");
				if (totalnum>0)
				{
					
					JSONArray newslist = dataObject.getJSONArray("newslist");
					for(int i=0;i<newslist.length();i++)
					{
						JSONObject newsObject = (JSONObject)newslist.opt(i); 
						HashMap<String, Object> hashMap = new HashMap<String, Object>();
						hashMap.put("nid", newsObject.getInt("nid"));
						hashMap.put("newslist_item_title", newsObject.getString("title"));
						hashMap.put("newslist_item_digest", newsObject.getString("digest"));
						hashMap.put("newslist_item_source", newsObject.getString("source"));
						hashMap.put("newslist_item_ptime", newsObject.getString("ptime"));
						hashMap.put("newslist_item_comments", newsObject.getString("commentcount"));
						newsList.add(hashMap);
					}
					return SUCCESS;
				}
				else
				{
					if (firstTimes)
					{
						return NONEWS;
					}
					else
					{
						return NOMORENEWS;
					}
				}
			}
			else
			{
				return LOADERROR;
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			return LOADERROR;
		}
	}
	
	private OnClickListener loadMoreListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			loadNewsAsyncTask = new LoadNewsAsyncTask();
			switch (v.getId())
			{
			case R.id.loadmore_btn:
				loadNewsAsyncTask.execute(mCid,mNewsData.size(),false);
				break;
			case R.id.titlebar_refresh:
				loadNewsAsyncTask.execute(mCid,0,true);
				break;
			}
			
		}
	};
	
	private class LoadNewsAsyncTask extends AsyncTask<Object, Integer, Integer>
	{
		
		@Override
		protected void onPreExecute()
		{
			
			mTitlebarRefresh.setVisibility(View.GONE);
			
			mLoadnewsProgress.setVisibility(View.VISIBLE); 
			
			mLoadMoreBtn.setText(R.string.loadmore_txt);
		}

		@Override
		protected Integer doInBackground(Object... params)
		{
			return getSpeCateNews((Integer)params[0],mNewsData,(Integer)params[1],(Boolean)params[2]);
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			
			switch (result)
			{
			case NONEWS:
				Toast.makeText(MainActivity.this, R.string.no_news, Toast.LENGTH_LONG).show();
			break;
			case NOMORENEWS:
				Toast.makeText(MainActivity.this, R.string.no_more_news, Toast.LENGTH_LONG).show();
				break;
			case LOADERROR:
				Toast.makeText(MainActivity.this, R.string.load_news_failure, Toast.LENGTH_LONG).show();
				break;
			}
			mNewsListAdapter.notifyDataSetChanged();
			
			mTitlebarRefresh.setVisibility(View.VISIBLE);
			
			mLoadnewsProgress.setVisibility(View.GONE); 
			
			mLoadMoreBtn.setText(R.string.loadmore_btn);
		}
	}
}