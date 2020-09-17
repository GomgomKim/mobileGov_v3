package kr.go.mobile.iff.sample;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sds.mobile.servicebrokerLib.ServiceBrokerLib;
import com.sds.mobile.servicebrokerLib.event.ResponseListener;

import java.util.ArrayList;
import java.util.List;

class ServiceBrokerListAdapter extends BaseAdapter {

    List<ServiceVO> serviceVO = new ArrayList<ServiceVO>();

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

        TextView txtServiceID = (TextView) view.findViewById(R.id.txtServiceId);
        TextView txtParmas = (TextView) view.findViewById(R.id.txtServiceParams);

        final ServiceVO serviceVO = (ServiceVO) getItem(position);
        txtServiceID.setText(serviceVO.getServiceID());
        txtParmas.setText(serviceVO.getParams());

        view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                ServiceBrokerLib brokerLib = new ServiceBrokerLib(view.getContext(), serviceVO.getResponseListener());
                Intent intent = new Intent();
                intent.putExtra(ServiceBrokerLib.KEY_SERVICE_ID, serviceVO.getServiceID());
                intent.putExtra(ServiceBrokerLib.KEY_PARAMETER, serviceVO.getParams());
                brokerLib.request(intent);
            }
        });

        return view;
    }

    protected void addServiceVO(String serviceID, String serviceParams, ResponseListener listener) {
        ServiceVO vo = new ServiceVO();
        vo.setServiceID(serviceID);
        vo.setParams(serviceParams);
        vo.setResponseListener(listener);

        serviceVO.add(vo);
    }
}
