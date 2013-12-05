package org.news.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONObject;

import org.news.utils.DisplayUtils;
import org.news.model.Category;
import org.news.utils.StringUtil;

import org.news.service.SyncHttp;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;

import android.view.View;
import android.view.View.OnClickListener;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import android.widget.GridView;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
    private int mcid;
    private final int SUCCESS = 0;
	private final int NONEWS = 1;
	private final int NOMORENEWS = 2;
	private final int LOADERROR = 3;
    private ArrayList<HashMap<String,Object>> numNewsData;
    private SimpleAdapter mNewsListAdapter;
    private LayoutInflater mInflater;
    private Button mTitlebarRefresh;
	private ProgressBar mLoadnewsProgress;
	private Button mLoadMoreBtn;
	private LoadNewsAsyncTask loadNewsAsyncTask;
	private String mCatName;
	
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mInflater = getLayoutInflater();
        numNewsData=new ArrayList<HashMap<String,Object>>();  //分类获取新闻信息
        mTitlebarRefresh = (Button)findViewById(R.id.titlebar_refresh);
		mLoadnewsProgress = (ProgressBar)findViewById(R.id.loadnews_progress);
		mTitlebarRefresh.setOnClickListener(loadMoreListener);
		
        String[] categoryArray = getResources().getStringArray(R.array.categories);
		//把新闻分类保存到List中
		final List<HashMap<String, Category>> categories = new ArrayList<HashMap<String, Category>>();
		//分割新闻类型字符串
		for(int i=0;i<categoryArray.length;i++)
		{
			String[] temp = categoryArray[i].split("[、]");
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
		mcid = 1;
		mCatName="焦点";
        GridView category = new GridView(this);
       
        int widthDip = new DisplayUtils().pxToDip(this, 55);
        category.setColumnWidth(widthDip);
        
        category.setNumColumns(GridView.AUTO_FIT);
       
        category.setGravity(Gravity.CENTER);//对齐方式
        
        category.setSelector(new ColorDrawable(Color.TRANSPARENT));
        
        int width = widthDip * categoryArray.length;
		LayoutParams params = new LayoutParams(width, LayoutParams.WRAP_CONTENT);
        category.setLayoutParams(params);
        
        category.setAdapter(new Customapply(this, categories, R.layout.category_title, new String[]{"category_title"}, new int[]{R.id.category_title}));
     
        LinearLayout layout = (LinearLayout) findViewById(R.id.category_layout);
        layout.addView(category);
      
        category.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) 
			{
				TextView text;
				for (int i = 0; i < parent.getCount(); i++) 
				{
					text = (TextView) parent.getChildAt(i);
					text.setTextColor(0XFFADB2AD);
					text.setBackgroundDrawable(null);
				}
				text = (TextView) view;
				text.setTextColor(Color.WHITE);
				text.setBackgroundResource(R.drawable.image_categorybar_item_selected_background);
				mcid = categories.get(position).get("category_title").getCid();
				mCatName = categories.get(position).get("category_title").getTitle();
				//getSpeCateNews(mcid,numNewsData,0,true);
				//mNewsListAdapter.notifyDataSetChanged();
				loadNewsAsyncTask = new LoadNewsAsyncTask();
				loadNewsAsyncTask.execute(mcid,0,true);
			}
		});
        
        getSpeCateNews(mcid,numNewsData,0,true);
        ListView news_list = (ListView) findViewById(R.id.news_list);
        mNewsListAdapter=new SimpleAdapter(this,numNewsData,R.layout.news_list_item,
        										new String[]{"newslist_item_title","newslist_item_summery","newslist_item_author","newslist_item_datatime"},
        										new int[]{R.id.newslist_item_title,R.id.newslist_item_summery,R.id.newslist_item_author,R.id.newslist_item_datatime});
        
        View footerView = mInflater.inflate(R.layout.loadmore, null);
        news_list.addFooterView(footerView);
        news_list.setAdapter(mNewsListAdapter);
        
        news_list.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				Intent intent = new Intent(MainActivity.this, NewsDetailsActivity.class);
				intent.putExtra("newsDate", numNewsData);
				intent.putExtra("position", position);
				intent.putExtra("categoryName", mCatName);
				startActivity(intent);
			}
		});
        mLoadMoreBtn = (Button)findViewById(R.id.loadmore_btn);
        mLoadMoreBtn.setOnClickListener(loadMoreListener);
	}

	private int getSpeCateNews(int cid,List<HashMap<String, Object>> newsList,int start,boolean first)
	{
		
		if(first==true)
		{
			newsList.clear();
		}
		String url = "http://10.0.2.2:8080/web/getSpecifyCategoryNews";
		String params = "startnid="+start+"&count=5&cid="+cid;
		SyncHttp syncHttp = new SyncHttp();
		try
		{
			//以Get方式请求，并获得返回结果
			String retStr = syncHttp.httpGet(url, params);
			JSONObject jsonObject = new JSONObject(retStr);
			//获取返回码，0表示成功
			int retCode = jsonObject.getInt("ret");
			if (0==retCode)
			{
				JSONObject dataObject = jsonObject.getJSONObject("data");
				//获取返回数目
				int totalnum = dataObject.getInt("totalnum");
				if (totalnum>0)
				{
					//获取返回新闻集合
					JSONArray newslist = dataObject.getJSONArray("newslist");
					for(int i=0;i<newslist.length();i++)
					{
						JSONObject newsObject = (JSONObject)newslist.opt(i); 
						HashMap<String, Object> hashMap = new HashMap<String, Object>();
						hashMap.put("nid", newsObject.getInt("nid"));
						hashMap.put("newslist_item_title", newsObject.getString("title"));
						hashMap.put("newslist_item_summery", newsObject.getString("digest"));
						hashMap.put("newslist_item_author", newsObject.getString("source"));
						hashMap.put("newslist_item_datatime", newsObject.getString("ptime"));
						newsList.add(hashMap);
					}
					return SUCCESS;
				}
				else
				{
					if(first==true)
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
				loadNewsAsyncTask.execute(mcid,numNewsData.size(),false);
				break;
			case R.id.titlebar_refresh:
				loadNewsAsyncTask.execute(mcid,0,true);
				break;
			}
		}
	};
	private class LoadNewsAsyncTask extends AsyncTask<Object,Integer,Integer>
	{
		
		@Override
		protected void onPreExecute() {
			mTitlebarRefresh.setVisibility(View.GONE);
			mLoadnewsProgress.setVisibility(View.VISIBLE);
			mLoadMoreBtn.setText(R.string.loadmore_txt);	
		}

		@Override
		protected Integer doInBackground(Object... params) {
			// TODO Auto-generated method stub
			return getSpeCateNews((Integer)params[0],numNewsData,(Integer)params[1],(Boolean)params[2]);
			
		}

		@Override
		protected void onPostExecute(Integer result) {
			// TODO Auto-generated method stub
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