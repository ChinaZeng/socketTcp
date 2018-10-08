package com.zzw.socketdemo.http;

import com.zzw.socketdemo.bean.AreaBean;
import com.zzw.socketdemo.bean.BseRoomBean;
import com.zzw.socketdemo.bean.GuanLanItemBean;
import com.zzw.socketdemo.bean.ListDataBean;
import com.zzw.socketdemo.bean.LoginResultBean;
import com.zzw.socketdemo.bean.QianXinItemBean;
import com.zzw.socketdemo.bean.ResultBean;
import com.zzw.socketdemo.bean.StationBean;
import com.zzw.socketdemo.bean.StatusInfoBean;
import com.zzw.socketdemo.bean.TeamInfoBean;

import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PartMap;
import retrofit2.http.Query;

public interface Api {

    @Multipart
    @POST("/glcs/staffmgr/appLogin")
    Observable<LoginResultBean> login(@PartMap Map<String, RequestBody> s);


    @GET("/glcs/bseRoom/getAreaTree")
    Observable<List<AreaBean>> getAreaTree();

    @GET("/glcs/bseRoom/getAllStation")
    Observable<List<StationBean>> getAllStation(@Query("areaId") String areaId );

    @GET("/glcs/bseRoom/getBseRoomListByArea")
    Observable<List<BseRoomBean>> getBseRoomListByArea(@Query("id") String id );

    @GET("/glcs/patrolScheme/getAppConstructionTeamInfo")
    Observable<ListDataBean<TeamInfoBean>> getAppConstructionTeamInfo();

    @GET("/glcs/pubrestrion/quertstatuslistinfo")
    Observable<List<StatusInfoBean>> quertstatuslistinfo();

    @Multipart
    @POST("/glcs/cblCable/getAppListDuanByPage")
    Observable<ListDataBean<GuanLanItemBean>> getAppListDuanByPage(@PartMap Map<String, RequestBody> requestBodyMap);

    @Multipart
    @POST("/glcs/cblFiber/getAppListByPage")
    Observable<ListDataBean<QianXinItemBean>> getAppListByPage(@PartMap Map<String, RequestBody> s);


    @Multipart
    @POST("/glcs/cblCable/duanAppAdd")
    Observable<ResultBean<Object>> duanAppAdd(@PartMap Map<String, RequestBody> s);

}
