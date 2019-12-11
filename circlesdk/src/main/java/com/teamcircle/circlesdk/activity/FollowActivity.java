package com.teamcircle.circlesdk.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.ApiHelper;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.MessageEvent;
import com.teamcircle.circlesdk.model.UserData;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class FollowActivity extends Activity {
    private Handler mHandler;
    private boolean mIsFollowers;
    private ListView mListView;
    private FollowAdapter mAdapter;
    private LinkedHashMap<Integer, UserData> mUsers;
    private UserData mUser;
    private String mCurrentTab = "Followers";
    private boolean haveMoreToLoad = true;
    private boolean followingHashtag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);
        EventBus.getDefault().register(this);
        final int userId = getIntent().getIntExtra("userId", -1);
        mUser = AppSocialGlobal.getInstance().getUserById(userId);
        mIsFollowers = getIntent().getBooleanExtra("follower", false);
        mHandler = new Handler();
        mListView = findViewById(R.id.list_view);
        mUsers = new LinkedHashMap<>();
        setupUsers();

        ImageView backImage = findViewById(R.id.back_image);
        backImage.setImageResource(AppSocialGlobal.backResourceId);
        FrameLayout back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        String[] titles = new String[]{ "Followers", "Following" };
        for (String title : titles) {
            TabLayout.Tab tab = tabLayout.newTab();
            tab.setText(title);
            tab.setTag(title);
            tabLayout.addTab(tab);
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String tabString = (String) tab.getTag();
                if (!mCurrentTab.equals(tabString)) {
                    mCurrentTab = tabString;
                    if (tabString.equals("Followers")) {
                        mIsFollowers = true;
                        getFollowers();
                    } else {
                        mIsFollowers = false;
                        getFollowings();
                    }
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        if (mIsFollowers) {
            getFollowers();
            tabLayout.getTabAt(0).select();
        } else {
            getFollowings();
            tabLayout.getTabAt(1).select();
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Subscribe(sticky = true)
    public void onEvent(MessageEvent event) {
        switch (event.type) {
            case FOLLOW:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setupUsers();
                    }
                });
                break;
        }
    }

    private class FollowAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (!mIsFollowers && followingHashtag) {
                return mUsers.size() + 1;
            }
            return mUsers.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int pos = position;
            if (!mIsFollowers && followingHashtag) {
                pos = position - 1;
            }

            View view = convertView;
            if (view == null) {
                LayoutInflater layoutInflater = LayoutInflater.from(FollowActivity.this);
                view = layoutInflater.inflate(R.layout.follow_row, parent, false);
            }
            ImageView imageView = view.findViewById(R.id.image);
            TextView nameText = view.findViewById(R.id.name);
            final Button followButton = view.findViewById(R.id.follow_button);

            if (!mIsFollowers && followingHashtag && position == 0) {
                imageView.setImageResource(R.drawable.hashtag);
                nameText.setText("Hashtags");
                followButton.setVisibility(View.GONE);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(FollowActivity.this, FollowingHashtagsActivity.class);
                        intent.putExtra("userId", mUser.userId);
                        startActivity(intent);
                    }
                });
            } else {
                final UserData userData = new ArrayList<UserData>(mUsers.values()).get(pos);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AppSocialGlobal.getInstance().gotoAccount(userData.userId, FollowActivity.this);
                    }
                });
                String photoUrl = userData.photoUrl;
                AppSocialGlobal.loadImage(photoUrl, imageView);
                nameText.setText(userData.username);

                UserData me = AppSocialGlobal.getInstance().me;
                if (userData.followed || userData.equals(me) || !mIsFollowers) {
                    followButton.setVisibility(View.GONE);
                }
                followButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AppSocialGlobal.getInstance().follow(userData);
                    }
                });
            }

            if (pos == mUsers.size() - 1 && haveMoreToLoad) {
                if (mIsFollowers) {
                    getFollowersNext();
                } else {
                    getFollowingsNext();
                }
            }

            return view;
        }
    }

    private void getFollowers() {
        mUsers.clear();
        ApiHelper.getFollowers(mUser.userId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APIGetFollowers success", response.toString());
                haveMoreToLoad = AppSocialGlobal.getInstance().updateUsers(response.optJSONArray("resultData"), mUsers);
                setupUsers();
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APIGetFollowers fail", errorMsg);
            }
        });
    }

    private void getFollowersNext() {
        ApiHelper.getFollowersNext(mUser.userId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APIGetFollowers success", response.toString());
                haveMoreToLoad = AppSocialGlobal.getInstance().updateUsers(response.optJSONArray("resultData"), mUsers);
                setupUsers();
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APIGetFollowers fail", errorMsg);
            }
        });
    }

    private void getFollowings() {
        mUsers.clear();
        ApiHelper.getFollowings(mUser.userId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APIGetFollowing success", response.toString());
                haveMoreToLoad = AppSocialGlobal.getInstance().updateUsers(response.optJSONObject("resultData").optJSONArray("users"), mUsers);
                followingHashtag = response.optJSONObject("resultData").optBoolean("followHashtagStatus");
                setupUsers();
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APIGetFollowings fail", errorMsg);
            }
        });
    }

    private void getFollowingsNext() {
        ApiHelper.getFollowingsNext(mUser.userId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APIGetFollowing success", response.toString());
                haveMoreToLoad = AppSocialGlobal.getInstance().updateUsers(response.optJSONArray("resultData"), mUsers);
                setupUsers();
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APIGetFollowings fail", errorMsg);
            }
        });
    }

    private void setupUsers() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mAdapter == null) {
                    mAdapter = new FollowAdapter();
                    mListView.setAdapter(mAdapter);
                } else {
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
    }
}
