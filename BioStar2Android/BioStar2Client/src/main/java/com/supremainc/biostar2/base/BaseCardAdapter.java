/*
 * Copyright 2015 Suprema(biostar2@suprema.co.kr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.supremainc.biostar2.base;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.supremainc.biostar2.R;
import com.supremainc.biostar2.Setting;
import com.supremainc.biostar2.popup.Popup;
import com.supremainc.biostar2.popup.Popup.OnPopupClickListener;
import com.supremainc.biostar2.popup.Popup.PopupType;
import com.supremainc.biostar2.sdk.datatype.CardData.Cards;
import com.supremainc.biostar2.sdk.datatype.CardData.ListCard;
import com.supremainc.biostar2.sdk.provider.DeviceDataProvider;
import com.supremainc.biostar2.sdk.volley.Response;
import com.supremainc.biostar2.sdk.volley.Response.Listener;
import com.supremainc.biostar2.sdk.volley.VolleyError;

import java.util.ArrayList;

public class BaseCardAdapter extends BaseListAdapter<ListCard> {
    protected static final int FIRST_LIMIT = 50;
    protected DeviceDataProvider mDeviceDataProvider;
    protected boolean mIsLastItemVisible = false;
    protected int mLimit = FIRST_LIMIT;
    protected int mOffset = 0;
    protected OnItemsListener mOnItemsListener;
    protected String mQuery;
    protected int mTotal = 0;
    Listener<Cards> mItemListener = new Response.Listener<Cards>() {
        @Override
        public void onResponse(Cards response, Object deliverParam) {
            if (isDestroy()) {
                return;
            }
            mPopup.dismiss();
            if (response == null || response.records == null) {
                if (mOnItemsListener != null) {
                    mOnItemsListener.onSuccessNull();
                }
                return;
            }
            if (mItems == null) {
                mItems = new ArrayList<ListCard>();
            }
            if (mOnItemsListener != null) {
                mOnItemsListener.onTotalReceive(response.total);
            }
            if (response.records.size() < 1) {
                return;
            }
            for (ListCard ListCard : response.records) {
                mItems.add(ListCard);
            }
            setData(mItems);
            mOffset = mItems.size() - 1;
            mTotal = response.total;
        }
    };
    private int mGroupId = 0;
    Runnable mRunGetItems = new Runnable() {
        @Override
        public void run() {
            if (isMemoryPoor()) {
                mPopup.dismiss();
                if (mSwipyRefreshLayout != null) {
                    mSwipyRefreshLayout.setRefreshing(false);
                }
                mToastPopup.show(mContext.getString(R.string.memory_poor), null);
                return;
            }
            mDeviceDataProvider.getCards(TAG, mItemListener, mItemErrorListener, mOffset, mLimit, mGroupId, mQuery, null);
        }
    };
    Response.ErrorListener mItemErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error, Object deliverParam) {
            if (isDestroy(error)) {
                return;
            }
            mPopup.dismiss();
            mPopup.show(PopupType.ALERT, mContext.getString(R.string.fail_retry), Setting.getErrorMessage(error, mContext), new OnPopupClickListener() {
                @Override
                public void OnNegative() {
                    mCancelExitListener.onCancel(null);
                }

                @Override
                public void OnPositive() {
                    mListView.removeCallbacks(mRunGetItems);
                    mListView.post(mRunGetItems);
                }
            }, mContext.getString(R.string.ok), mContext.getString(R.string.cancel), false);
        }
    };

    public BaseCardAdapter(Activity context, ArrayList<ListCard> items, ListView listView, OnItemClickListener itemClickListener, Popup popup, OnItemsListener onItemsListener) {
        super(context, items, listView, popup);
        listView.setAdapter(this);
        setOnItemClickListener(itemClickListener);
        mDeviceDataProvider = DeviceDataProvider.getInstance(context);
        mOnItemsListener = onItemsListener;
        setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == OnScrollListener.SCROLL_STATE_IDLE && mIsLastItemVisible && mTotal - 1 > mOffset) {
                    mPopup.showWait(true);
                    mListView.removeCallbacks(mRunGetItems);
                    mListView.postDelayed(mRunGetItems, 100);
                } else {
                    mPopup.dismissWiat();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                mIsLastItemVisible = (totalItemCount > 0) && (firstVisibleItem + visibleItemCount >= totalItemCount);
            }
        });
    }

    @Override
    public void getItems(String query) {
        mQuery = query;
        mOffset = 0;
        mTotal = 0;
        mLimit = FIRST_LIMIT;
        mListView.removeCallbacks(mRunGetItems);
        mDeviceDataProvider.cancelAll(TAG);
        mPopup.showWait(mCancelExitListener);
        if (mItems != null) {
            mItems.clear();
            notifyDataSetChanged();
        }
        mListView.postDelayed(mRunGetItems, 500);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        super.onItemClick(parent, view, position, id);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemViewHolder viewHolder = getViewHolder(position, convertView, parent, R.layout.list_item);
        if (viewHolder == null) {
            return null;
        }

        ListCard item = mItems.get(position);
        if (item != null) {
            viewHolder.mName.setText(item.card_id);
        }
        return viewHolder.mRoot;
    }
}