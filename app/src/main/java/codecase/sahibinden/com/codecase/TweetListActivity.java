package codecase.sahibinden.com.codecase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.tweetcomposer.ComposerActivity;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;
import com.twitter.sdk.android.tweetui.Timeline;
import com.twitter.sdk.android.tweetui.TimelineResult;
import com.twitter.sdk.android.tweetui.TweetTimelineListAdapter;
import com.twitter.sdk.android.tweetui.UserTimeline;

import java.lang.ref.WeakReference;

import io.fabric.sdk.android.Fabric;

/**
 * An activity representing a list of Tweets. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link TweetDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class TweetListActivity extends AppCompatActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private SwipeRefreshLayout swipeLayout;
    private SwipeRefreshLayout.OnRefreshListener swipeRefreshListener;
    private static final int TWEETER_REQ_CODE = 1;
    final WeakReference<Activity> activityRef = new WeakReference<Activity>(TweetListActivity.this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tweet_list);

        TwitterAuthConfig authConfig = new TwitterAuthConfig("ACbtwPwrlbtPvf6o2uiR3NThl", "iLHe6ncZUZHO6nFQOJXgA0ptRSiDcrdi7CTVDCb0sESS7ow07A");
        Fabric.with(this, new TwitterCore(authConfig), new TweetComposer());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getResources().getString(R.string.user_timeline));
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();
                    final Intent intent = new ComposerActivity.Builder(TweetListActivity.this).session(session).createIntent();
                    startActivityForResult(intent, TWEETER_REQ_CODE);
                }
            });
        }

//        String userName = Twitter.getInstance().core.getSessionManager().getActiveSession().getUserName();
        final UserTimeline userTimeline = new UserTimeline.Builder().screenName("fabric").build();
        final CustomTweetTimelineListAdapter adapter = new CustomTweetTimelineListAdapter(this, userTimeline);
        final ListView listView = (ListView) findViewById(R.id.list);
        assert listView != null;
        listView.setAdapter(adapter);

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);
        final View emptyView = findViewById(android.R.id.empty);
        listView.setEmptyView(emptyView);

        assert swipeLayout != null;
        swipeLayout.setColorSchemeResources(R.color.twitter_blue, R.color.twitter_dark);

        // set custom scroll listener to enable swipe refresh layout only when at list top
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            boolean enableRefresh = false;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listView.getChildCount() > 0) {
                    // check that the first item is visible and that its top matches the parent
                    enableRefresh = listView.getFirstVisiblePosition() == 0 && listView.getChildAt(0).getTop() >= 0;
                } else {
                    enableRefresh = false;
                }
                swipeLayout.setEnabled(enableRefresh);
            }
        });

        // specify action to take on swipe refresh
        swipeRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeLayout.setRefreshing(true);
                adapter.refresh(new Callback<TimelineResult<Tweet>>() {
                    @Override
                    public void success(Result<TimelineResult<Tweet>> result) {
                        swipeLayout.setRefreshing(false);
                    }

                    @Override
                    public void failure(TwitterException exception) {
                        swipeLayout.setRefreshing(false);
                        final Activity activity = activityRef.get();
                        if (activity != null && !activity.isFinishing()) {
                            Toast.makeText(activity, exception.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        };
        swipeLayout.setOnRefreshListener(swipeRefreshListener);

        if (findViewById(R.id.tweet_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }


    /**
     * Custom Adapter to overrides TweetView onClickListener
     */

    class CustomTweetTimelineListAdapter extends TweetTimelineListAdapter {

        public CustomTweetTimelineListAdapter(Context context, Timeline<Tweet> timeline) {
            super(context, timeline);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            //disable subviews
            if (view instanceof ViewGroup) {
                disableViewAndSubViews((ViewGroup) view);
            }

            //enable root view and attach custom listener
            view.setEnabled(true);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String tweetId = String.valueOf(getItemId(position));

                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putString(TweetDetailFragment.ARG_ITEM_ID, tweetId);
                        TweetDetailFragment fragment = new TweetDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction().replace(R.id.tweet_detail_container, fragment).commit();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, TweetDetailActivity.class);
                        intent.putExtra(TweetDetailFragment.ARG_ITEM_ID, tweetId);
                        context.startActivity(intent);
                    }

                }
            });
            return view;
        }

        private void disableViewAndSubViews(ViewGroup layout) {
            layout.setEnabled(false);
            for (int i = 0; i < layout.getChildCount(); i++) {
                View child = layout.getChildAt(i);
                if (child instanceof ViewGroup) {
                    disableViewAndSubViews((ViewGroup) child);
                } else {
                    child.setEnabled(false);
                    child.setClickable(false);
                    child.setLongClickable(false);
                }
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TWEETER_REQ_CODE) {
            swipeLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeLayout.setRefreshing(true);
                    swipeRefreshListener.onRefresh();
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
