package kr.go.mobile.iff.sample.nativz;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import kr.go.mobile.common.v3.CommonBasedAPI;
import kr.go.mobile.common.v3.broker.Response;
import kr.go.mobile.iff.sample.R;
import kr.go.mobile.iff.sample.util.TimeStamp;

class ServiceBrokerListAdapter extends BaseAdapter {

    List<ServiceVO> serviceVO = new ArrayList<>();

    @Override
    public int getCount() {
        return serviceVO.size();
    }

    @Override
    public Object getItem(int position) {
        return serviceVO.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final Context context = parent.getContext();

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.serviceid_list, parent, false);
        }

        TextView txtServiceID = view.findViewById(R.id.txtServiceId);
        TextView txtServiceParmas = view.findViewById(R.id.txtServiceParams);

        final ServiceVO serviceVO = (ServiceVO) getItem(position);
        txtServiceID.setText(serviceVO.getServiceID());
        txtServiceParmas.setText(serviceVO.getParams());

        view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                TimeStamp.startTime("broker");
                // Request 생성시 serviceId, serviceParameters 값은 필수이다.

                try {
                    CommonBasedAPI.call(serviceVO.getServiceID(), serviceVO.getParams(), serviceVO.getListener());
                } catch (CommonBasedAPI.CommonBaseAPIException e) {
                    Log.e("@@@", e.getMessage(), e);
                    Toast.makeText(context, "요청 실패 : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    protected void addServiceVO(String serviceID, String serviceParams, Response.Listener listener) {
        ServiceVO vo = new ServiceVO();
        vo.serviceID = serviceID;
        vo.params = serviceParams;
        vo.listener = listener;
        serviceVO.add(vo);
    }
}
