package com.xiaochao.lcrapiddevelop.UI;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xiaochao.lcrapiddevelop.Adapter.ListViewAdapter;
import com.xiaochao.lcrapiddevelop.Data.Constant;
import com.xiaochao.lcrapiddevelop.Data.JsonData;
import com.xiaochao.lcrapiddevelop.R;
import com.xiaochao.lcrapiddevelop.Volley.VolleyInterface;
import com.xiaochao.lcrapiddevelop.Volley.VolleyReQuest;
import com.xiaochao.lcrapiddevelop.entity.DataDto;
import com.xiaochao.lcrapiddevelop.entity.IsError;
import com.xiaochao.lcrapiddevelop.entity.UniversityListDto;
import com.xiaochao.lcrapiddeveloplibrary.BaseQuickAdapter;
import com.xiaochao.lcrapiddeveloplibrary.viewtype.ProgressActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListvViewActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener,BaseQuickAdapter.RequestLoadMoreListener {

    RecyclerView mRecyclerView;
    SwipeRefreshLayout swipeLayout;
    ProgressActivity progress;
    private Toolbar toolbar;
    private BaseQuickAdapter mQuickAdapter;
    private int PageIndex=1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listv_view);
        toolBar();
        initView();
        initListener();
    }

    private void toolBar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.rv_list);
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipeLayout);
        progress = (ProgressActivity) findViewById(R.id.progress);
        //设置RecyclerView的显示模式  当前List模式
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        //如果Item高度固定  增加该属性能够提高效率
        mRecyclerView.setHasFixedSize(true);
        //设置页面为加载中..
        progress.showLoading();
        //请求网络数据
        initdate(PageIndex,false);
        //设置适配器
        mQuickAdapter = new ListViewAdapter(R.layout.list_view_item_layout,null);
        //设置加载动画
        mQuickAdapter.openLoadAnimation(BaseQuickAdapter.SCALEIN);
        //设置是否自动加载以及加载个数
        mQuickAdapter.openLoadMore(6,true);
        //将适配器添加到RecyclerView
        mRecyclerView.setAdapter(mQuickAdapter);
    }
    private void initListener() {
        //设置下拉刷新监听
        swipeLayout.setOnRefreshListener(this);
        //设置自动加载监听
        mQuickAdapter.setOnLoadMoreListener(this);

        mQuickAdapter.setOnRecyclerViewItemClickListener(new BaseQuickAdapter.OnRecyclerViewItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(ListvViewActivity.this, "点击了"+position, Toast.LENGTH_SHORT).show();
            }
        });
    }
    //下拉刷新
    @Override
    public void onRefresh() {
        PageIndex=1;
        initdate(PageIndex,false);
    }
   //自动加载
    @Override
    public void onLoadMoreRequested() {
        PageIndex++;
        initdate(PageIndex,true);
    }
    public void initdate(int PageIndex,final Boolean isJz){
        Map<String,String> map=new HashMap<String,String>();
        map.put("ProvinceIds","");
        map.put("Classify","");
        map.put("CollegeLevel","");
        map.put("IsBen","");
        map.put("PageIndex",PageIndex+"");
        map.put("PageSize","10");
        JSONObject json=new JSONObject(map);
        VolleyReQuest.ReQuestPost_null(this, Constant.DATA_URL, "school_list_post", json, new VolleyInterface(VolleyInterface.mLisener, VolleyInterface.mErrorLisener) {
            @Override
            public JSONObject onMySuccess(JSONObject response) {
                DataDto<UniversityListDto> data=JsonData.httpDate(response,isJz);
                switch (data.getType()){
                    case JsonData.DATA_LOAD_OK:
                        //新增自动加载的的数据
                        mQuickAdapter.notifyDataChangedAfterLoadMore(data.getT(), true);
                        break;
                    case JsonData.DATA_LOAD_NULL:
                        //所有数据加载完成后显示
                        mQuickAdapter.notifyDataChangedAfterLoadMore(false);
                        View view = getLayoutInflater().inflate(R.layout.not_loading, (ViewGroup) mRecyclerView.getParent(), false);
                        mQuickAdapter.addFooterView(view);
                        break;
                    case JsonData.DATA_REFRESH_OK:
                        //进入显示的初始数据或者下拉刷新显示的数据
                        mQuickAdapter.setNewData(data.getT());//新增数据
                        mQuickAdapter.openLoadMore(10,true);//设置是否可以下拉加载  以及加载条数
                        swipeLayout.setRefreshing(false);//刷新成功
                        progress.showContent();
                        break;
                    case JsonData.DATA_REFRESH_NULL:
                        //设置页面为无数据
                        toEmpty();
                        break;
                    case JsonData.DATA_ERROR:
                        //设置页面为加载错误
                        toError();
                        break;
                }
                return response;
            }

            @Override
            public String onMyError(VolleyError error) {
                toError();
                return null;
            }
        });
    }
    public void toError(){
        try {
            mQuickAdapter.notifyDataChangedAfterLoadMore(false);
            View view = getLayoutInflater().inflate(R.layout.not_loading, (ViewGroup) mRecyclerView.getParent(), false);
            mQuickAdapter.addFooterView(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
        progress.showError(getResources().getDrawable(R.mipmap.monkey_cry), Constant.ERROR_TITLE, Constant.ERROR_CONTEXT, Constant.ERROR_BUTTON, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progress.showLoading();
                initdate(1,false);
            }
        });
    }
    public void toEmpty(){
        progress.showEmpty(getResources().getDrawable(R.mipmap.monkey_cry),Constant.EMPTY_TITLE,Constant.EMPTY_CONTEXT);
    }
}
