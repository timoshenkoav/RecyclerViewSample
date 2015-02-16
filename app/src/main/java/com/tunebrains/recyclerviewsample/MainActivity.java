package com.tunebrains.recyclerviewsample;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;
import com.tunebrains.recyclertwowaygrid.TwoWayGridLayoutManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends ActionBarActivity {
    @InjectView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    private MultiSelector mMultiSelector = new MultiSelector();

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

        mMultiSelector.setSelectable(true);
        // specify an adapter (see also next example)
        DataSetAdapter mAdapter = new DataSetAdapter(getLayoutInflater(), createDataset(), mMultiSelector);
        mRecyclerView.setAdapter(mAdapter);
    }

    private List<Data> createDataset() {
        List<Data> lDatas = new ArrayList<>(100);
        for (int i = 0; i < 100; ++i) {
            lDatas.add(new Data(String.format("Title %d", i), String.format("Subtitle %d", i)));
        }
        return lDatas;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.custom:
                initCustomRecyclerView();
                return true;
            case R.id.simple:
                initSimpleRecyclerView();
                return true;
        }
        return false;
    }

    private void initSimpleRecyclerView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initCustomRecyclerView() {
        mRecyclerView.setLayoutManager(new TwoWayGridLayoutManager());
    }

    private static class Data {
        private final String mTitle;
        private final String mSubTitle;
        private boolean mChecked;

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

        public boolean isChecked() {
            return mChecked;
        }

        public void setChecked(boolean pChecked) {
            mChecked = pChecked;
        }
    }

    static class DataViewHolder extends SwappingHolder implements View.OnClickListener, View.OnLongClickListener {

        private final MultiSelector mMultiSelector;
        @InjectView(R.id.title)
        TextView mTitle;

        @InjectView(R.id.sub_title)
        TextView mSubTitle;


        public DataViewHolder(View itemView, MultiSelector pMultiSelector) {
            super(itemView, pMultiSelector);
            mMultiSelector = pMultiSelector;
            ButterKnife.inject(this, itemView);
            bindClick();
        }

        private void bindClick() {
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            itemView.setLongClickable(true);
        }

        public void bind(Data pD) {
            mTitle.setText(pD.getTitle());
            mSubTitle.setText(pD.getSubTitle());
        }

        @Override
        public void onClick(View v) {
            if (mMultiSelector.tapSelection(this)) {
                // Selection is on, so tapSelection() toggled item selection.
            } else {
                // Selection is off; handle normal item click here.
            }

        }

        @Override
        public boolean onLongClick(View v) {
            mMultiSelector.setSelected(this, true);
            return true;
        }
    }

    static class DataSetAdapter extends RecyclerView.Adapter<DataViewHolder> {
        private final List<Data> mDataList;
        private MultiSelector mMultiSelector;
        private LayoutInflater mLayoutInflater;

        private DataSetAdapter(LayoutInflater pLayoutInflater, List<Data> pDataList, MultiSelector pMultiSelector) {
            mLayoutInflater = pLayoutInflater;
            mDataList = pDataList;

            mMultiSelector = pMultiSelector;
        }

        @Override
        public DataViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View content = mLayoutInflater.inflate(R.layout.list_item, parent, false);

            return new DataViewHolder(content, mMultiSelector);
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
