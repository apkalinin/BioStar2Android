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
package com.supremainc.biostar2.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.supremainc.biostar2.R;
import com.supremainc.biostar2.meta.Setting;
import com.supremainc.biostar2.adapter.PhotoUserAdapter;
import com.supremainc.biostar2.adapter.base.BaseListAdapter.OnItemsListener;
import com.supremainc.biostar2.sdk.datatype.v2.Card.ListCard;
import com.supremainc.biostar2.sdk.datatype.v2.Common.ResponseStatus;
import com.supremainc.biostar2.sdk.datatype.v2.Permission.PermissionModule;
import com.supremainc.biostar2.sdk.datatype.v2.User.ListUser;
import com.supremainc.biostar2.sdk.datatype.v2.User.User;
import com.supremainc.biostar2.sdk.datatype.v2.User.UserGroup;
import com.supremainc.biostar2.sdk.volley.Response;
import com.supremainc.biostar2.sdk.volley.Response.Listener;
import com.supremainc.biostar2.sdk.volley.VolleyError;
import com.supremainc.biostar2.view.SubToolbar;
import com.supremainc.biostar2.widget.ScreenControl;
import com.supremainc.biostar2.widget.ScreenControl.ScreenType;
import com.supremainc.biostar2.widget.popup.Popup.OnPopupClickListener;
import com.supremainc.biostar2.widget.popup.Popup.PopupType;
import com.supremainc.biostar2.widget.popup.SelectPopup;
import com.supremainc.biostar2.widget.popup.SelectPopup.OnSelectResultListener;
import com.supremainc.biostar2.widget.popup.SelectPopup.SelectType;

import java.util.ArrayList;

public class UserListFragment extends BaseFragment {
    protected static final int MODE_DELETE = 1;
    private String mSearchText = null;
    private SelectPopup<UserGroup> mSelectUserGroupsPopup;
    private SubToolbar mSubToolbar;
    private String mTitle;
    private PhotoUserAdapter mUserAdapter;
    private int mTotal = 0;
    private UserGroup mUserGroup;
    private Response.Listener<User> mItemClickListener = new Response.Listener<User>() {
        @Override
        public void onResponse(User response, Object deliverParam) {
            if (isInValidCheck(null)) {
                return;
            }
            mPopup.dismissWiat();
            if (response == null) {
                mErrorItemClickListener.onErrorResponse(new VolleyError(getString(R.string.server_null)), deliverParam);
                return;
            }
            ScreenControl screenControl = ScreenControl.getInstance();
            User arg = null;
            try {
                arg = response.clone();
            } catch (Exception e) {
                Log.e(TAG, "selected user clone fail");
                e.printStackTrace();
            }
            Bundle bundle = new Bundle();
            bundle.putSerializable(User.TAG, arg);
            screenControl.addScreen(ScreenType.USER_INQURIY, bundle);
        }
    };
    private Response.ErrorListener mErrorItemClickListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error, final Object deliverParam) {
            if (isInValidCheck(error)) {
                return;
            }
            mPopup.dismissWiat();
            mPopup.show(PopupType.ALERT, mContext.getString(R.string.fail_retry), Setting.getErrorMessage(error, mContext), new OnPopupClickListener() {
                @Override
                public void OnNegative() {

                }

                @Override
                public void OnPositive() {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mPopup.showWait(true);
                            mUserDataProvider.getUser(TAG, (String) deliverParam, mItemClickListener, mErrorItemClickListener, (String) deliverParam);
                        }
                    });
                }
            }, mContext.getString(R.string.ok), mContext.getString(R.string.cancel));
        }
    };
    private OnItemsListener mOnUsersListener = new OnItemsListener() {
        @Override
        public void onSuccessNull() {
            mIsDataReceived = true;
            if (mTotal == 0 ) {
                mToastPopup.show(getString(R.string.none_data), null);
            }
        }

        @Override
        public void onTotalReceive(int total) {
            mIsDataReceived = true;
            if (mTotal != total) {
                mSubToolbar.setTotal(total);
                mTotal = total;
                if (mSearchText == null && mUserAdapter != null && mUserAdapter.getuserGroupId().equals("1")) {
                    sendLocalBroadcast(Setting.BROADCAST_USER_COUNT, total);
                }
            }
        }
    };
    private Listener<ResponseStatus> mDeleteListener = new Response.Listener<ResponseStatus>() {
        @Override
        public void onResponse(ResponseStatus response, Object deliverParam) {
            if (mIsDestroy) {
                return;
            }
            mUserAdapter.clearChoices();
            mUserAdapter.getItems(mSearchText);
            if (mSubToolbar != null) {
                mSubToolbar.setSelectedCount(0);
            }
            mToastPopup.show(getString(R.string.deleted_user) + " " + (Integer) deliverParam, null);
        }
    };
    private Response.ErrorListener mDeleteErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error, Object deliverParam) {
            if (isInValidCheck(error)) {
                return;
            }
            mPopup.dismiss();
            mPopup.show(PopupType.ALERT, mContext.getString(R.string.fail_retry), Setting.getErrorMessage(error, mContext), new OnPopupClickListener() {
                @Override
                public void OnPositive() {
                    deleteDo();
                }

                @Override
                public void OnNegative() {

                }
            }, mContext.getString(R.string.ok), mContext.getString(R.string.cancel), false);
        }
    };

    private SubToolbar.SubToolBarListener mSubToolBarEvent = new SubToolbar.SubToolBarListener() {
        @Override
        public void onClickSelectAll() {
            if (mSubToolbar.showReverseSelectAll()) {
                if (mUserAdapter != null) {
                    mUserAdapter.selectChoices();
                    mSubToolbar.setSelectedCount(mUserAdapter.getCheckedItemCount());
                }
            } else {
                if (mUserAdapter != null) {
                    mUserAdapter.clearChoices();
                    mSubToolbar.setSelectedCount(0);
                }
            }
        }
    };

    public UserListFragment() {
        super();
        setType(ScreenType.USER);
        TAG = getClass().getSimpleName() + String.valueOf(System.currentTimeMillis());
    }

    private void applyPermission() {
        ActivityCompat.invalidateOptionsMenu(this.getActivity());
    }

    private void deleteConfirm(int selectedCount) {
        mPopup.show(PopupType.ALERT, getString(R.string.delete_confirm_question), getString(R.string.selected_count) + " " + selectedCount, new OnPopupClickListener() {
            @Override
            public void OnNegative() {
            }

            @Override
            public void OnPositive() {
                deleteDo();
            }


        }, getString(R.string.ok), getString(R.string.cancel));
    }

    private void deleteDo() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mPopup.showWait(mCancelExitListener);
                ArrayList<ListUser> users = mUserAdapter.getCheckedItems();
                mUserDataProvider.deleteUsers(TAG, users, mDeleteListener, mDeleteErrorListener, null);
            }
        });
    }

    private void initValue() {
        mSelectUserGroupsPopup = new SelectPopup<UserGroup>(mContext, mPopup);
        if (mSubToolbar == null) {
            mSubToolbar = (SubToolbar) mRootView.findViewById(R.id.subtoolbar);
            mSubToolbar.init(getActivity());
            mSubToolbar.setVisibleSearch(true, new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    if (mSearchText == null) {
                        return false;
                    }
                    onSearch(null);
                    return false;
                }
            });
            if (!Setting.IS_DELETE_ALL_USER) {
                mSubToolbar.setSelectAllViewGone(true);
            }
            mSubToolbar.showMultipleSelectInfo(false, 0);
        }

        if (mUserAdapter == null) {
            mUserAdapter = new PhotoUserAdapter(mContext, null, getListView(), new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (mSubToolbar == null) {
                        return;
                    }
                    if (mSubMode == MODE_DELETE) {
                        mSubToolbar.setSelectAllViewOff();
                        mSubToolbar.setSelectedCount(mUserAdapter.getCheckedItemCount());
                    } else {
                        ListUser user = (ListUser) mUserAdapter.getItem(position);
                        if (user == null) {
                            return;
                        }
                        mPopup.showWait(true);
                        mUserDataProvider.getUser(TAG, user.user_id, mItemClickListener, mErrorItemClickListener, user.user_id);
                    }
                }
            }, mPopup, mOnUsersListener);
            mUserAdapter.setSwipyRefreshLayout(getSwipeyLayout(), getFab());
        }
    }

    @Override
    public boolean onBack() {
        if (mSubToolbar != null) {
            if (mSubToolbar.isExpandSearch()) {
                mSubToolbar.setSearchIconfied();
                return true;
            }
        }
        if (super.onBack()) {
            return true;
        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mIsDataReceived && mUserAdapter != null) {
            mUserAdapter.getItems(mSearchText);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSubToolbar != null) {
            mSubToolbar.hideIme();
        }
    }

    @Override
    public void onDestroy() {
        if (mUserAdapter != null) {
            mUserAdapter.clearItems();
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }
        if (mSubToolbar != null) {
            mSubToolbar.hideIme();
        }
        switch (item.getItemId()) {
            case R.id.action_delete_confirm:
                int selectedCount = mUserAdapter.getCheckedItemCount();
                if (selectedCount < 1) {
                    mToastPopup.show(getString(R.string.selected_none), null);
                    return true;
                }
                deleteConfirm(selectedCount);
                break;
            case R.id.action_delete:
                setSubMode(MODE_DELETE);
                break;
            case R.id.action_add:
                if (mUserGroup == null) {
                    mScreenControl.addScreen(ScreenType.USER_MODIFY, null);
                } else {
                    UserGroup userGroup = null;
                    try {
                        userGroup = mUserGroup.clone();
                    } catch (CloneNotSupportedException e) {
                        Log.e(TAG, "selected user clone fail");
                        e.printStackTrace();
                    }
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(UserGroup.TAG, userGroup);
                    mScreenControl.addScreen(ScreenType.USER_MODIFY, bundle);
                }
                return true;
            case R.id.action_usergroups:
                mSelectUserGroupsPopup.show(SelectType.USER_GROUPS, new OnSelectResultListener<UserGroup>() {
                    @Override
                    public void OnResult(ArrayList<UserGroup> selectedItem,boolean isPositive) {
                        if (isInValidCheck(null)) {
                            return;
                        }
                        if (selectedItem == null) {
                            return;
                        }
                        mUserGroup = selectedItem.get(0);
                        if (mUserAdapter != null) {
                            mUserAdapter.setUserGroupId(mUserGroup.id);
                        }
                        mTitle = mUserGroup.name;
                        initActionbar(mUserGroup.name);
                    }
                }, null, getString(R.string.select_user_group), false);
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public boolean onSearch(String query) {
        if (super.onSearch(query)) {
            return true;
        }
        mSearchText = query;
        if (mUserAdapter == null && mSubToolbar == null) {
            return true;
        }
        mUserAdapter.clearChoices();
        mSubToolbar.setSelectedCount(0);
        mUserAdapter.getItems(mSearchText);
        return true;
    }

    protected void registerBroadcast() {
        if (mReceiver == null) {
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (mIsDestroy) {
                        return;
                    }
                    if (action.equals(Setting.BROADCAST_USER)) {
                        User user = (User) getExtraData(Setting.BROADCAST_USER, intent);
                        if (user != null) {
                            if (mUserAdapter.modifyItem(user)) {
                                return;
                            }
                        }
                        if (isResumed()) {
                            mUserAdapter.getItems(mSearchText);
                        } else {
                            mIsDataReceived = false;
                        }
                    } else if (action.equals(Setting.BROADCAST_REROGIN)) {
                        applyPermission();
                    } else if (action.equals(Setting.BROADCAST_UPDATE_CARD)) {
                        User user = getExtraData(Setting.BROADCAST_UPDATE_CARD, intent);
                        if (user == null) {
                            return;
                        }
                        mUserAdapter.modifyCardItem(user.user_id,user.card_count);
                        return;
                    } else if (action.equals(Setting.BROADCAST_UPDATE_FINGER)) {
                        User user = getExtraData(Setting.BROADCAST_UPDATE_FINGER, intent);
                        if (user == null) {
                            return;
                        }
                        mUserAdapter.modifyFingerPrintItem(user.user_id,user.fingerprint_count);
                        return;
                    }

                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Setting.BROADCAST_USER);
            intentFilter.addAction(Setting.BROADCAST_REROGIN);
            intentFilter.addAction(Setting.BROADCAST_UPDATE_CARD);
            intentFilter.addAction(Setting.BROADCAST_UPDATE_FINGER);

            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, intentFilter);
        }
    }

    @Override
    protected void setSubMode(int mode) {
        mSubMode = mode;
        switch (mode) {
            case MODE_NORMAL:
                mUserAdapter.setChoiceMode(ListView.CHOICE_MODE_NONE);
                mSubToolbar.showMultipleSelectInfo(false, 0);
                break;
            case MODE_DELETE:
                mUserAdapter.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                mSubToolbar.showMultipleSelectInfo(true, 0);
                break;
        }
        ActivityCompat.invalidateOptionsMenu(this.getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setResID(R.layout.fragment_list_swpiy);
        super.onCreateView(inflater, container, savedInstanceState);
        if (!mIsReUsed) {
            initValue();
            mTitle = getString(R.string.all_users);
            initActionbar(mTitle);
            mRootView.invalidate();
        }
        return mRootView;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = mContext.getMenuInflater();
        if (mPermissionDataProvider.getPermission(PermissionModule.USER, true) || mPermissionDataProvider.getPermission(PermissionModule.USER_GROUP, true)) {
            switch (mSubMode) {
                default:
                case MODE_NORMAL:
                    initActionbar(mTitle);
                    inflater.inflate(R.menu.user_list_admin, menu);
                    break;
                case MODE_DELETE:
                    initActionbar(getString(R.string.delete) + " " + getString(R.string.user));
                    inflater.inflate(R.menu.delete_confirm, menu);
                    break;
            }
        } else {
            inflater.inflate(R.menu.menu, menu);
        }
        super.onPrepareOptionsMenu(menu);
    }
}