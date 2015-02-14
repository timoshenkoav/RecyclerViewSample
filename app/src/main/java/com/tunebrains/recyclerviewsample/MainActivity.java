package com.tunebrains.recyclerviewsample;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends ActionBarActivity {
    @InjectView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        initRecyclerView();
    }

    private void initRecyclerView() {
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        DataSetAdapter mAdapter = new DataSetAdapter(getLayoutInflater(), createDataset());
        mRecyclerView.setAdapter(mAdapter);
    }

    private List<Data> createDataset() {
        List<Data> lDatas = new ArrayList<>(100);
        for (int i = 0; i < 100; ++i) {
            lDatas.add(new Data(String.format("Title %d", i), String.format("Subtitle %d", i)));
        }
        return lDatas;
    }

    private static class Data {
        private final String mTitle;
        private final String mSubTitle;

        private Data(String pTitle, String pSubTitle) {
            mTitle = pTitle;
            mSubTitle = pSubTitle;
        }

        public String getSubTitle() {
            return mSubTitle;
        }

        public String getTitle() {
            return mTitle;
        }
    }

    static class DataViewHolder extends RecyclerView.ViewHolder {

        @InjectView(R.id.title)
        TextView mTitle;

        @InjectView(R.id.sub_title)
        TextView mSubTitle;

        public DataViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }

        public void bind(Data pD) {
            mTitle.setText(pD.getTitle());
            mSubTitle.setText(pD.getSubTitle());
        }
    }

    static class DataSetAdapter extends RecyclerView.Adapter<DataViewHolder> {
        private final List<Data> mDataList;
        private LayoutInflater mLayoutInflater;

        private DataSetAdapter(LayoutInflater pLayoutInflater, List<Data> pDataList) {
            mLayoutInflater = pLayoutInflater;
            mDataList = pDataList;

        }

        @Override
        public DataViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View content = mLayoutInflater.inflate(R.layout.list_item, parent, false);

            return new DataViewHolder(content);
        }

        @Override
        public void onBindViewHolder(DataViewHolder holder, int position) {
            Data d = mDataList.get(position);
            holder.bind(d);

        }

        @Override
        public int getItemCount() {
            return mDataList.size();
        }
    }


}
