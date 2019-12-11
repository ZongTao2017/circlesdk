package com.teamcircle.circlesdk.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.tabs.TabLayout;
import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.ApiHelper;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.HashtagData;
import com.teamcircle.circlesdk.model.UserData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class SearchActivity extends Activity {
    private ListView mListView;
    private SearchResultAdapter mAdapter;
    private EditText mSearchText;
    private ImageView mClearImage;
    private LinkedHashMap<Integer, UserData> mAccounts;
    private ArrayList<HashtagData> mHashtags;
    private boolean isSearchingAccounts = false;
    private boolean isSearching = false;
    private String mCurrentTab = "TAGS";
    private boolean haveMoreAccountsToLoad = true;
    private boolean haveMoreTagsToLoad = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        ImageView backImage = findViewById(R.id.back_image);
        backImage.setImageResource(AppSocialGlobal.backResourceId);
        FrameLayout back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        String[] titles = new String[]{ "TAGS", "ACCOUNTS" };
        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
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
                    if (tabString.equals("ACCOUNTS")) {
                        isSearchingAccounts = true;
                        mSearchText.setHint("Search account");
                    } else {
                        isSearchingAccounts = false;
                        mSearchText.setHint("Search hashtag");
                    }
                    mAdapter.notifyDataSetChanged();
                    mListView.setSelection(0);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mSearchText = findViewById(R.id.search_text);
        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mSearchText.getText().toString().isEmpty()) {
                    mClearImage.setVisibility(View.GONE);
                    isSearching = false;
                } else {
                    mClearImage.setVisibility(View.VISIBLE);
                    isSearching = true;
                }
                searchForPeopleAndTags();
                mAdapter.notifyDataSetChanged();
            }
        });

        mClearImage = findViewById(R.id.clear);
        mClearImage.setVisibility(View.GONE);
        mClearImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchText.getText().clear();
                mClearImage.setVisibility(View.GONE);
                isSearching = false;
                mAdapter.notifyDataSetChanged();
            }
        });

        mAccounts = new LinkedHashMap<>();
        mHashtags = new ArrayList<>();
        searchForPeopleAndTags();

        mListView = findViewById(R.id.list_view);
        mAdapter = new SearchResultAdapter();
        mListView.setAdapter(mAdapter);
    }

    private class SearchResultAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (isSearchingAccounts) {
                if (isSearching) {
                    return mAccounts.size();
                } else {
                    return mAccounts.size() + 1;
                }
            } else {
                if (isSearching) {
                    return mHashtags.size();
                } else {
                    return mHashtags.size() + 1;
                }
            }
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
            View view = convertView;
            LayoutInflater layoutInflater = LayoutInflater.from(SearchActivity.this);
            if (isSearching) {
                view = layoutInflater.inflate(R.layout.search_row, parent, false);
                ImageView imageView = view.findViewById(R.id.image);
                ImageView hashtagImage = view.findViewById(R.id.hashtag_image);
                TextView nameText = view.findViewById(R.id.name);
                TextView numberText = view.findViewById(R.id.number);
                if (isSearchingAccounts) {
                    final UserData userData = new ArrayList<>(mAccounts.values()).get(position);
                    hashtagImage.setVisibility(View.GONE);
                    numberText.setText(String.format("%d followers", userData.followerNumber));
                    AppSocialGlobal.loadImage(userData.photoUrl, imageView);
                    nameText.setText(userData.username);
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AppSocialGlobal.getInstance().gotoAccount(userData.userId, SearchActivity.this);
                        }
                    });
//                    if (position == mAccounts.values().size() - 1 && haveMoreAccountsToLoad) {
//                        searchForPeopleAndTagsNext();
//                    }
                } else {
                    final HashtagData hashtagData = mHashtags.get(position);
                    nameText.setText(hashtagData.hashtag);
                    numberText.setText(String.format("%d posts", hashtagData.number));
                    AppSocialGlobal.loadImage(hashtagData.photoUrl, imageView);
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(SearchActivity.this, HashtagActivity.class);
                            intent.putExtra("hashtag", hashtagData.hashtag);
                            startActivity(intent);
                        }
                    });
//                    if (position == mHashtags.size() - 1 && haveMoreTagsToLoad) {
//                        searchForPeopleAndTagsNext();
//                    }
                }
            } else {
                if (isSearchingAccounts) {
                    if (position == 0) {
                        view = layoutInflater.inflate(R.layout.search_section_header, parent, false);
                        TextView title = view.findViewById(R.id.header_title);
                        title.setText("Suggested");
                    } else {
                        view = layoutInflater.inflate(R.layout.search_row, parent, false);
                        ImageView imageView = view.findViewById(R.id.image);
                        ImageView hashtagImage = view.findViewById(R.id.hashtag_image);
                        TextView nameText = view.findViewById(R.id.name);
                        TextView numberText = view.findViewById(R.id.number);
                        final UserData userData = new ArrayList<>(mAccounts.values()).get(position - 1);
                        hashtagImage.setVisibility(View.GONE);
                        numberText.setText(String.format("%d followers", userData.followerNumber));
                        AppSocialGlobal.loadImage(userData.photoUrl, imageView);
                        nameText.setText(userData.username);
                        view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                AppSocialGlobal.getInstance().gotoAccount(userData.userId, SearchActivity.this);
                            }
                        });
                    }
//                    if (position == mAccounts.values().size() && haveMoreAccountsToLoad) {
//                        searchForPeopleAndTagsNext();
//                    }
                } else {
                    if (position == 0) {
                        view = layoutInflater.inflate(R.layout.search_section_header, parent, false);
                        TextView title = view.findViewById(R.id.header_title);
                        title.setText("Suggested");
                    } else {
                        view = layoutInflater.inflate(R.layout.search_row, parent, false);
                        ImageView imageView = view.findViewById(R.id.image);
                        ImageView hashtagImage = view.findViewById(R.id.hashtag_image);
                        TextView nameText = view.findViewById(R.id.name);
                        TextView numberText = view.findViewById(R.id.number);
                        final HashtagData hashtagData = mHashtags.get(position - 1);
                        AppSocialGlobal.loadImage(hashtagData.photoUrl, imageView);
                        nameText.setText(hashtagData.hashtag);
                        numberText.setText(String.format("%d posts", hashtagData.number));
                        view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(SearchActivity.this, HashtagActivity.class);
                                intent.putExtra("hashtag", hashtagData.hashtag);
                                startActivity(intent);
                            }
                        });
                    }
//                    if (position == mHashtags.size() && haveMoreTagsToLoad) {
//                        searchForPeopleAndTagsNext();
//                    }
                }
            }
            return view;
        }
    }

    private void searchForPeopleAndTags() {
        String text = mSearchText.getText().toString();
        ApiHelper.searchPeople(text, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APISearchUsers success", response.toString());
                mAccounts.clear();
                haveMoreAccountsToLoad = AppSocialGlobal.getInstance().updateUsers(response.optJSONArray("resultData"), mAccounts);
                if (isSearchingAccounts) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APISearchUsers fail", errorMsg);
            }
        });
        ApiHelper.searchHashtag(text, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APISearchHashtags", response.toString());
                mHashtags.clear();
                JSONArray jsonArray = response.optJSONArray("resultData");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.optJSONObject(i);
                    String hashtag = jsonObject.optString("hashtag");
                    int number = jsonObject.optInt("tagNumber");
                    JSONObject photo = jsonObject.optJSONObject("post").optJSONArray("photos").optJSONObject(0);
                    String photoUrl = null;
                    if (!photo.isNull("imageUrl")) {
                        photoUrl = photo.optString("imageUrl");
                    } else if (!photo.isNull("videoUrl")) {
                        photoUrl = photo.optString("videoUrl").split("&photoUrl=")[1];
                    }
                    HashtagData hashtagData = new HashtagData(hashtag, number, photoUrl);
                    mHashtags.add(hashtagData);
                }
                if (jsonArray.length() == 0)
                    haveMoreTagsToLoad = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APISearchHashtags", errorMsg);
            }
        });
    }

    private void searchForPeopleAndTagsNext() {
        String text = mSearchText.getText().toString();
        ApiHelper.searchPeopleNext(text, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APISearchUsers success", response.toString());
                haveMoreAccountsToLoad = AppSocialGlobal.getInstance().updateUsers(response.optJSONArray("resultData"), mAccounts);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APISearchUsers fail", errorMsg);
            }
        });
        ApiHelper.searchHashtagNext(text, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APISearchHashtags", response.toString());
                JSONArray jsonArray = response.optJSONArray("resultData");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.optJSONObject(i);
                    String hashtag = jsonObject.optString("hashtag");
                    int number = jsonObject.optInt("tagNumber");
                    JSONObject photo = jsonObject.optJSONObject("post").optJSONArray("photos").optJSONObject(0);
                    String photoUrl = null;
                    if (!photo.isNull("imageUrl")) {
                        photoUrl = photo.optString("imageUrl");
                    } else if (!photo.isNull("videoUrl")) {
                        photoUrl = photo.optString("videoUrl").split("&photoUrl=")[1];
                    }
                    HashtagData hashtagData = new HashtagData(hashtag, number, photoUrl);
                    mHashtags.add(hashtagData);
                }
                if (jsonArray.length() == 0)
                    haveMoreTagsToLoad = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APISearchHashtags", errorMsg);
            }
        });

    }
}
