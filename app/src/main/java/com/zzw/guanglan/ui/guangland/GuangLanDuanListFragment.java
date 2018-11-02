package com.zzw.guanglan.ui.guangland;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.dl7.tag.TagLayout;
import com.dl7.tag.TagView;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.zzw.guanglan.R;
import com.zzw.guanglan.base.BaseFragment;
import com.zzw.guanglan.bean.GradeBean;
import com.zzw.guanglan.bean.GuangLanDItemBean;
import com.zzw.guanglan.bean.ListDataBean;
import com.zzw.guanglan.bean.SingleChooseBean;
import com.zzw.guanglan.http.Api;
import com.zzw.guanglan.http.retrofit.RetrofitHttpEngine;
import com.zzw.guanglan.manager.LocationManager;
import com.zzw.guanglan.rx.ErrorObserver;
import com.zzw.guanglan.rx.LifeObservableTransformer;
import com.zzw.guanglan.ui.guangland.add.GuangLanDAddActivitty;
import com.zzw.guanglan.ui.qianxin.QianXinListActivity;
import com.zzw.guanglan.utils.RequestBodyUtils;
import com.zzw.guanglan.utils.ToastUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.functions.Consumer;

public class GuangLanDuanListFragment extends BaseFragment implements BaseQuickAdapter.OnItemClickListener,
        BaseQuickAdapter.RequestLoadMoreListener, SwipeRefreshLayout.OnRefreshListener,
        TextView.OnEditorActionListener, LocationManager.OnLocationListener {

    @BindView(R.id.recy)
    RecyclerView recy;
    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout refreshLayout;
    @BindView(R.id.et_param)
    EditText etParam;
    @BindView(R.id.tv_sel)
    TextView tvSel;
    @BindView(R.id.hide)
    TextView hide;
    @BindView(R.id.loca)
    TextView loca;
    @BindView(R.id.location)
    TextView location;
    @BindView(R.id.loca_content)
    View locaContent;
    @BindView(R.id.juli)
    TagLayout juli;
    @BindView(R.id.jibie)
    TagLayout jibie;

    private GuangLanDListAdapter adapter;
    private String searchKey;
    private String searchJuli;
    private String searchJibie;
    private String searchLontude;
    private String searchLatude;

    private int searchFlog = 0;
    private int tempFlog = 0;

    private int pageNo = 1;

    private boolean userLoca = false;
    private LocationManager locationManager;


    public static GuangLanDuanListFragment newInstance() {
        return new GuangLanDuanListFragment();
    }


    @Override
    protected int getLayoutId() {
        return R.layout.fragment_guang_lan_d_list;
    }


    @Override
    protected void initData() {
        super.initData();

        etParam.setOnEditorActionListener(this);
        initLoca();
        startLocation();
        recy.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GuangLanDListAdapter(new ArrayList<GuangLanDItemBean>());
        adapter.setOnItemClickListener(this);
        adapter.setEnableLoadMore(true);
        adapter.setOnLoadMoreListener(this, recy);
        recy.setAdapter(adapter);

        refreshLayout.setOnRefreshListener(this);

        onRefresh();
    }

    private String juliStr;
    private String jibieStr;

    private void initLoca() {
        final List<SingleChooseBean> juliS = new ArrayList<>();
        juliS.add(new SingleChooseBean(0, "300m", 0.3f));
        juliS.add(new SingleChooseBean(1, "1km", 1.0f));
        juliS.add(new SingleChooseBean(2, "2km", 2.0f));
        juliS.add(new SingleChooseBean(3, "5km", 5.0f));
//        juliS.add(new SingleChooseBean(3, "10km", 10000));
//        juliS.add(new SingleChooseBean(4, "30km", 30000));
//        juliS.add(new SingleChooseBean(5, "60km", 60000));
//        juliS.add(new SingleChooseBean(6, "100km", 100000));
//        juliS.add(new SingleChooseBean(7, "180km", 180000));

        juli.cleanTags();
        for (SingleChooseBean singleChooseBean : juliS) {
            juli.addTags(singleChooseBean.getName());
        }
        juli.setTagCheckListener(new TagView.OnTagCheckListener() {
            @Override
            public void onTagCheck(int i, String s, boolean b) {
                if (b) {
                    juliStr = String.valueOf(juliS.get(i).getFloatValue());
                    if (userLoca) {
                        onRefresh();
                    }
                }
            }
        });
        juli.setCheckTag(0);
        juliStr = String.valueOf(juliS.get(0).getFloatValue());

        RetrofitHttpEngine.obtainRetrofitService(Api.class)
                .quertListInfo()
                .compose(LifeObservableTransformer.<List<GradeBean>>create(this))
                .subscribe(new ErrorObserver<List<GradeBean>>(this) {
                    @Override
                    public void onNext(final List<GradeBean> data) {
                        if (data == null) {
                            return;
                        }
                        jibie.cleanTags();
                        for (GradeBean datum : data) {
                            jibie.addTags(datum.getDescChina());
                        }
                        jibie.setTagCheckListener(new TagView.OnTagCheckListener() {
                            @Override
                            public void onTagCheck(int i, String s, boolean b) {
                                if (b) {
                                    jibieStr = String.valueOf(data.get(i).getDescChina());
                                    if (userLoca) {
                                        onRefresh();
                                    }
                                }
                            }
                        });
                        jibie.setCheckTag(0);
                        jibieStr = String.valueOf(data.get(0).getDescChina());
                    }
                });

    }


    void setData(List<GuangLanDItemBean> datas) {
        if (pageNo == 1) {
            adapter.replaceData(datas);
            refreshLayout.setRefreshing(false);
        } else {
            adapter.addData(datas);
        }
    }


    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        QianXinListActivity.open(getContext(), (GuangLanDItemBean) adapter.getData().get(position));
    }


    @OnClick({R.id.add, R.id.search, R.id.tv_sel, R.id.hide, R.id.loca, R.id.location})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.add:
                GuangLanDAddActivitty.open(getContext());
                break;

            case R.id.search:
                hideKeyWordSearch();
                break;
            case R.id.tv_sel:
                showListPopupWindow(tvSel);
                break;
            case R.id.hide:
                locaContent.setVisibility(locaContent.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                if (locaContent.getVisibility() == View.VISIBLE) {
                    hide.setText("收起");
                } else {
                    hide.setText("展开");
                }
                break;
            case R.id.loca:
                loca.setSelected(!loca.isSelected());

                if (loca.isSelected()) {
                    userLoca = true;
                    locaContent.setVisibility(View.VISIBLE);
                    hide.setVisibility(View.VISIBLE);
                    hide.setText("收起");
                } else {
                    userLoca = false;
                    locaContent.setVisibility(View.GONE);
                    hide.setVisibility(View.GONE);
                }

                onRefresh();

                break;

            case R.id.location:
                startLocation();
                break;
        }
    }

    public void showListPopupWindow(View view) {
        final String items[] = {"光缆端编码", "光缆段名称"};
        final ListPopupWindow listPopupWindow = new ListPopupWindow(getContext());

        // ListView适配器
        listPopupWindow.setAdapter(
                new ArrayAdapter<String>(getContext().getApplicationContext(), android.R.layout.simple_list_item_1, items));

        // 选择item的监听事件
        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                tempFlog = pos;
                tvSel.setText(items[pos]);
                listPopupWindow.dismiss();
            }
        });

        // 对话框的宽高
        listPopupWindow.setWidth(500);
        listPopupWindow.setHeight(400);

        // ListPopupWindow的锚,弹出框的位置是相对当前View的位置
        listPopupWindow.setAnchorView(view);

        // ListPopupWindow 距锚view的距离
//        listPopupWindow.setHorizontalOffset(50);
//        listPopupWindow.setVerticalOffset(100);

        listPopupWindow.setModal(true);

        listPopupWindow.show();
    }


    @Override
    public void onLoadMoreRequested() {
        pageNo++;
        search(searchKey, searchFlog, pageNo);
    }

    @Override
    public void onRefresh() {
        pageNo = 1;
        search(searchKey, searchFlog, pageNo);
    }

    void hideKeyWordSearch() {
        // 当按了搜索之后关闭软键盘
        ((InputMethodManager) etParam.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(getActivity().getCurrentFocus()
                        .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        String searchKey = etParam.getText().toString().trim();
        search(searchKey, tempFlog, pageNo);
    }

    void search(final String key, final int flog, final int page) {
        searchKey = key;
        searchFlog = flog;

        String searchJuli = null;
        String searchJibie = null;
        String searchLontude = null;
        String searchLatude = null;
        if (userLoca) {
            searchJuli = juliStr;
            searchJibie = jibieStr;
            if (locationBean != null) {
                searchLontude = String.valueOf(locationBean.longitude);
                searchLatude = String.valueOf(locationBean.latitude);
            }
        }

        this.searchJuli = searchJuli;
        this.searchJibie = searchJibie;
        this.searchLontude = searchLontude;
        this.searchLatude = searchLatude;

        RetrofitHttpEngine.obtainRetrofitService(Api.class)
                .getAppListDuanByPage(RequestBodyUtils.generateRequestBody(new HashMap<String, String>() {
                    {
                        put("model.cabelOpCode", GuangLanDuanListFragment.this.searchFlog == 0 ? GuangLanDuanListFragment.this.searchKey : "");
                        put("model.cabelOpName", GuangLanDuanListFragment.this.searchFlog == 0 ? "" : GuangLanDuanListFragment.this.searchKey);
                        put("pageNum", String.valueOf(page));

                        put("model.dd", GuangLanDuanListFragment.this.searchJuli);
                        put("model.descChina", GuangLanDuanListFragment.this.searchJibie);
                        put("model.lontude", GuangLanDuanListFragment.this.searchLontude);
                        put("model.latude", GuangLanDuanListFragment.this.searchLatude);
                    }
                }))
                .compose(LifeObservableTransformer.<ListDataBean<GuangLanDItemBean>>create(this))
                .subscribe(new ErrorObserver<ListDataBean<GuangLanDItemBean>>(this) {
                    @Override
                    public void onNext(ListDataBean<GuangLanDItemBean> guanLanItemBeans) {
                        if (guanLanItemBeans != null && guanLanItemBeans.getList() != null) {
                            setData(guanLanItemBeans.getList());
                            if (adapter.getData().size() >= guanLanItemBeans.getTotal()) {
                                adapter.loadMoreEnd();
                            } else {
                                adapter.loadMoreComplete();
                            }
                        }
                    }
                });
    }


    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            hideKeyWordSearch();
            return true;
        }
        return false;
    }

    @SuppressLint("CheckResult")
    private void startLocation() {
        location.setText("定位中...");
        new RxPermissions(getActivity())
                .request(Manifest.permission.ACCESS_COARSE_LOCATION)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            stopLocation();
                            locationManager = new LocationManager(getContext(),
                                    GuangLanDuanListFragment.this);
                            locationManager.start();
                        } else {
                            ToastUtils.showToast("请开启定位权限");
                            location.setText("定位失败，点击重新定位");
                        }
                    }
                });
    }

    private void stopLocation() {
        if (locationManager != null) {
            locationManager.stop();
            locationManager = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopLocation();
    }

    private LocationManager.LocationBean locationBean;

    @Override
    public void onSuccess(LocationManager.LocationBean bean) {
        location.setText("定位地址: " + bean.addrss);
        this.locationBean = bean;
    }

    @Override
    public void onError(int code, String msg) {
        location.setText("定位地址: 定位失败，点击重新定位");
    }

}