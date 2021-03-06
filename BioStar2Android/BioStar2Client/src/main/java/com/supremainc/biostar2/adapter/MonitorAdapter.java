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
package com.supremainc.biostar2.adapter;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;

import com.supremainc.biostar2.BuildConfig;
import com.supremainc.biostar2.R;
import com.supremainc.biostar2.adapter.base.BaseListAdapter;
import com.supremainc.biostar2.adapter.base.BaseMonitorAdapter;
import com.supremainc.biostar2.sdk.models.v2.eventlog.ListEventLog;
import com.supremainc.biostar2.sdk.models.v2.eventlog.LogLevel;
import com.supremainc.biostar2.sdk.models.v2.eventlog.LogType;
import com.supremainc.biostar2.sdk.provider.DateTimeDataProvider;
import com.supremainc.biostar2.view.StyledTextView;
import com.supremainc.biostar2.widget.popup.Popup;

import java.util.ArrayList;

public class MonitorAdapter extends BaseMonitorAdapter {

    public MonitorAdapter(Activity context, ArrayList<ListEventLog> items, ListView listView, OnItemClickListener itemClickListener, boolean clickEnable, Popup popup,
                          BaseListAdapter.OnItemsListener onItemsListener) {
        super(context, items, listView, itemClickListener, popup, onItemsListener);
        setClickEnable(clickEnable);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (!mIsClickEnable) {
            return;
        }
        ItemViewHolder viewHolder = (ItemViewHolder) view.getTag();
        ListEventLog item = mItems.get(position);
        StringBuilder sb = new StringBuilder();
        sb.append(item.getDescription());
        sb.append("\n");
        sb.append(getDate(item));
        sb.append("\n");
        if (getUserName(item) != null) {
            sb.append(mActivity.getString(R.string.user));
            sb.append("\n");
            sb.append(getUserName(item));
            sb.append("\n");
        }
        if (getDeviceName(item) != null) {
            sb.append(mActivity.getString(R.string.device));
            sb.append("\n");
            sb.append(getDeviceName(item));
        }
        mPopup.show(Popup.PopupType.NONE, mActivity.getString(R.string.view_log), sb.toString(), null, null, null);
        super.onItemClick(parent, view, position, id);

    }

    private String getDeviceName(ListEventLog item) {
        if (item.device != null) {
            if (item.device.name == null) {
                return item.device.id + " / " + item.device.id;
            } else {
                return item.device.id + " / " + item.device.name;
            }
        }
        return null;
    }

    private String getUserName(ListEventLog item) {
        if (item.user != null) {
            return item.user.user_id + " / " + item.user.getName();
        }
        return null;
    }

    private String getDate(ListEventLog item) {
        String date = item.getTimeFormmat(mDateTimeDataProvider, ListEventLog.ListEventLogTimeType.datetime, DateTimeDataProvider.DATE_TYPE.FORMAT_DATE_HOUR_MIN_SEC);
        if (date != null) {
            String[] test = date.split(" ");
            if (test.length > 2) {
                String reFormat = test[0] + " ";
                for (int i = 1; i < test.length; i++) {
                    reFormat = reFormat + test[i];
                }
                date = reFormat;
            }
        }
        return date;
    }

    private void setDisplay(StyledTextView view, String content) {
        if (content == null) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
            view.setText(content);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mItems == null || mItems.size() < 1) {
            return null;
        }
        if (null == convertView) {
            convertView = mInflater.inflate(R.layout.list_item_monitor, parent, false);
            ItemViewHolder viewHolder = new ItemViewHolder(convertView);
            convertView.setTag(viewHolder);
        }
        ItemViewHolder vh = (ItemViewHolder) convertView.getTag();
        if (vh == null) {
            vh = new ItemViewHolder(convertView);
            convertView.setTag(vh);
        }
        if (!mIsClickEnable) {
            vh.mLink.setVisibility(View.GONE);
        }
        ListEventLog item = mItems.get(position);
        if (item != null) {
            vh.mTitle.setText(item.getDescription());
            vh.mDate.setText(getDate(item));
            setDisplay(vh.mUser, getUserName(item));
            setDisplay(vh.mDevice, getDeviceName(item));
            if (mIsClickEnable) {
                setSelector(vh.mRoot, vh.mLink, position, true);
            } else {
                setSelector(vh.mRoot, vh.mLink, position, false);
            }
            setIcon(item, vh.mIcon);
        }
        return vh.mRoot;
    }

    private void setGreen(LogType type, ImageView view) {
        switch (type) {
            case DEFAULT:
                view.setImageResource(R.drawable.monitoring_ic3);
                break;
            case DEVICE:
                view.setImageResource(R.drawable.ic_event_device_01);
                break;
            case DOOR:
                view.setImageResource(R.drawable.ic_event_door_01);
                break;
            case USER:
                view.setImageResource(R.drawable.ic_event_user_01);
                break;
            case ZONE:
                view.setImageResource(R.drawable.ic_event_zone_01);
                break;
            case AUTHENTICATION:
                view.setImageResource(R.drawable.ic_event_auth_01);
                break;
        }
    }

    private void setIcon(ListEventLog item, ImageView view) {
        LogType type = LogType.DEFAULT;

        if (LogType.DEVICE.mName.equals(item.type)) {
            type = LogType.DEVICE;
        } else if (LogType.DOOR.mName.equals(item.type)) {
            type = LogType.DOOR;
        } else if (LogType.USER.mName.equals(item.type)) {
            type = LogType.USER;
        } else if (LogType.ZONE.mName.equals(item.type)) {
            type = LogType.ZONE;
        } else if (LogType.AUTHENTICATION.mName.equals(item.type)) {
            type = LogType.AUTHENTICATION;
        }
        if (type == LogType.DEFAULT) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "type is NULL:" + item.id + " code:" + item.event_type.code + " name:" + item.event_type.name);
            }
        }

        if (LogLevel.GREEN.mName.equals(item.level)) {
            setGreen(type, view);
        } else if (LogLevel.YELLOW.mName.equals(item.level)) {
            setYellow(type, view);
        } else if (LogLevel.RED.mName.equals(item.level)) {
            setRed(type, view);
        } else {
            setGreen(type, view);
        }

//		int code = item.event_type.code;
//		// VERIFY_SUCCESS
//		if (isCondiction(4096, 4111, code, view, R.drawable.monitoring_ic3)) {
//			return;
//		}
//		// VERIFY_FAIL
//		if (isCondiction(4352, 4359, code, view, R.drawable.monitoring_ic3)) {
//			return;
//		}
//		// VERIFY_DURESS
//		if (isCondiction(4608, 4623, code, view, R.drawable.monitoring_ic2)) {
//			return;
//		}
//		// IDENTIFY_SUCCESS
//		if (isCondiction(4864, 4868, code, view, R.drawable.monitoring_ic3)) {
//			return;
//		}
//		// IDENTIFY_FAIL
//		if (isCondiction(5120, 5127, code, view, R.drawable.monitoring_ic3)) {
//			return;
//		}
//		// IDENTIFY_DURESS
//		if (isCondiction(5376, 5380, code, view, R.drawable.monitoring_ic2)) {
//			return;
//		}
//		// AUTH_FAIL
//		if (isCondiction(5888, 6147, code, view, R.drawable.monitoring_ic1)) {
//			return;
//		}
//		// ACCESS_DENIED
//		if (isCondiction(6400, 6407, code, view, R.drawable.monitoring_ic8)) {
//			return;
//		}
//		// SYSTEM
//		if (isCondiction(12288, 17664, code, view, R.drawable.monitoring_ic4)) {
//			return;
//		}
//		// DOOR //TODO icon change
//		if (isCondiction(20480, 21248, code, view, R.drawable.door_ic_3)) {
//			return;
//		}
//		// DOOR //TODO icon change
//		if (isCondiction(21504, 27648, code, view, R.drawable.monitoring_ic6)) {
//			return;
//		}
//		view.setImageResource(R.drawable.monitoring_ic3);
    }

    private void setRed(LogType type, ImageView view) {
        switch (type) {
            case DEFAULT:
                view.setImageResource(R.drawable.monitoring_ic7);
                break;
            case DEVICE:
                view.setImageResource(R.drawable.ic_event_device_02);
                break;
            case DOOR:
                view.setImageResource(R.drawable.ic_event_door_02);
                break;
            case USER:
                view.setImageResource(R.drawable.ic_event_user_02);
                break;
            case ZONE:
                view.setImageResource(R.drawable.ic_event_zone_02);
                break;
            case AUTHENTICATION:
                view.setImageResource(R.drawable.ic_event_auth_02);
                break;
        }
    }


    private void setYellow(LogType type, ImageView view) {
        switch (type) {
            case DEFAULT:
                view.setImageResource(R.drawable.monitoring_ic1);
                break;
            case DEVICE:
                view.setImageResource(R.drawable.ic_event_device_03);
                break;
            case DOOR:
                view.setImageResource(R.drawable.ic_event_door_03);
                break;
            case USER:
                view.setImageResource(R.drawable.ic_event_user_03);
                break;
            case ZONE:
                view.setImageResource(R.drawable.ic_event_zone_03);
                break;
            case AUTHENTICATION:
                view.setImageResource(R.drawable.ic_event_auth_03);
                break;
        }
    }

    private class ItemViewHolder {
        public View mRoot;
        public StyledTextView mDate;
        public StyledTextView mDevice;
        public ImageView mIcon;
        public StyledTextView mTitle;
        public StyledTextView mUser;
        public ImageView mLink;

        public ItemViewHolder(View root) {
            mRoot = root;
            mIcon = (ImageView) root.findViewById(R.id.picture);
            mTitle = (StyledTextView) root.findViewById(R.id.title);
            mDate = (StyledTextView) root.findViewById(R.id.date);
            mUser = (StyledTextView) root.findViewById(R.id.user);
            mDevice = (StyledTextView) root.findViewById(R.id.device);
            mLink = (ImageView) root.findViewById(R.id.info);
        }
    }

}
