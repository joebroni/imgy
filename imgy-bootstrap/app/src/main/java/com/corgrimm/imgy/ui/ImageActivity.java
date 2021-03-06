package com.corgrimm.imgy.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.corgrimm.imgy.R;
import com.corgrimm.imgy.api.ImgyApi;
import com.corgrimm.imgy.core.Constants;
import com.corgrimm.imgy.dialog.CommentDialog;
import com.corgrimm.imgy.models.*;
import com.devspark.appmsg.AppMsg;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flurry.android.FlurryAgent;
import com.google.inject.Inject;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.image.SmartImageView;
import com.slidingmenu.lib.SlidingMenu;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import roboguice.inject.InjectExtra;
import roboguice.inject.InjectView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.corgrimm.imgy.core.Constants.Extra.*;
import static com.corgrimm.imgy.core.Constants.Oauth.EXPIRED_TOKEN;
import static com.corgrimm.imgy.core.Constants.Oauth.IMGUR_CLIENT_ID;
import static com.corgrimm.imgy.core.Constants.Oauth.TOKEN_VALID;
import static com.corgrimm.imgy.core.Constants.Vote.*;

public class ImageActivity extends BootstrapActivity {

    @Inject protected ObjectMapper objectMapper;

    @InjectView(R.id.imgImage) protected SmartImageView image;
    @InjectView(R.id.viewContainer) protected RelativeLayout viewContainer;

    @InjectView(R.id.caption) protected TextView caption;
    @InjectView(R.id.upvote) protected ImageButton upvote;
    @InjectView(R.id.downvote) protected ImageButton downvote;
    @InjectView(R.id.new_comment) protected ImageButton comment;
    @InjectView(R.id.info) protected LinearLayout info;
    @InjectView(R.id.downvotes) protected TextView downvotes;
    @InjectView(R.id.upvotes) protected TextView upvotes;
    @InjectView(R.id.author) protected TextView author;

    @InjectExtra(GALLERY) protected ArrayList<Object> gallery;
    @InjectExtra(INDEX) protected int index;

    protected  SlidingMenu menu;
    ListView menuList;
    protected int vote_status;
    List<Comment> comments;
    GalleryImage gImage;
    SubredditImage sImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.image);

        menu = new SlidingMenu(this);
        menu.setMode(SlidingMenu.RIGHT);
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        menu.setShadowWidthRes(R.dimen.shadow_width);
        menu.setShadowDrawable(R.drawable.shadowright);
        menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        menu.setFadeDegree(0.35f);
        menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        menu.setMenu(R.layout.slider_menu_list);
        menuList = (ListView) findViewById(R.id.menu_list);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (gallery.get(index).getClass() == GalleryImage.class) {
            gImage = (GalleryImage) gallery.get(index);
        }
        else {
            sImage = (SubredditImage) gallery.get(index);
        }

        if (gImage != null) {
            hydrateGImage(gImage);
        }
        else {
            hydrateSImage(sImage);
        }

        setListeners();
    }

    private void setListeners() {
        upvote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FlurryAgent.logEvent(Constants.Flurry.UP_VOTE);
                    if (vote_status == DOWNVOTE) {
                        downvote.setImageDrawable(getResources().getDrawable(R.drawable.down_white_256));
                    }
                    upvote.setImageDrawable(getResources().getDrawable(R.drawable.up_green_256));
                    vote_status = UPVOTE;
                    voteOnImage(UP_VOTE_STRING);
            }
        });

        downvote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FlurryAgent.logEvent(Constants.Flurry.DOWN_VOTE);
                    if (vote_status == UPVOTE) {
                        upvote.setImageDrawable(getResources().getDrawable(R.drawable.up_white_256));
                    }
                    downvote.setImageDrawable(getResources().getDrawable(R.drawable.down_red_256));
                    vote_status = DOWNVOTE;
                    voteOnImage(DOWN_VOTE_STRING);
//                }
            }
        });

        comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new CommentDialog(ImageActivity.this, gImage.getId(), null).show();
            }
        });

        menu.setOnOpenedListener(new SlidingMenu.OnOpenedListener() {
            @Override
            public void onOpened() {
                FlurryAgent.logEvent(Constants.Flurry.VIEW_COMMENTS);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.imgy_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.comments:
                menu.toggle(true);
                FlurryAgent.logEvent(Constants.Flurry.COMMENT_BUTTON_CLICK);
                return true;
            case R.id.left:
                previous();
                return true;
            case R.id.right:
                next();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void previous() {
        FlurryAgent.logEvent(Constants.Flurry.PREV_BUTTON_CLICK);
        if (index != 0 ) {
            if (gallery.get(index - 1).getClass() == GalleryImage.class || gallery.get(index + 1).getClass() == SubredditImage.class) {
                startActivity(new Intent(ImageActivity.this, ImageActivity.class).putExtra(GALLERY, gallery).putExtra(INDEX, index - 1).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
            else {
                startActivity(new Intent(ImageActivity.this, AlbumActivity.class).putExtra(GALLERY, gallery).putExtra(INDEX, index - 1).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        }
    }

    private void next() {
        FlurryAgent.logEvent(Constants.Flurry.NEXT_BUTTON_CLICK);
        if (index != gallery.size() - 1 ) {
            if (gallery.get(index + 1).getClass() == GalleryImage.class || gallery.get(index + 1).getClass() == SubredditImage.class) {
                startActivity(new Intent(ImageActivity.this, ImageActivity.class).putExtra(GALLERY, gallery).putExtra(INDEX, index + 1).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
            else {
                startActivity(new Intent(ImageActivity.this, AlbumActivity.class).putExtra(GALLERY, gallery).putExtra(INDEX, index + 1).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        }
    }

    private void voteOnImage(final String vote) {
        int tokenStatus = ImgyApi.checkForValidAuthToken(this);
        if ( tokenStatus == TOKEN_VALID) {
            ImgyApi.voteForImage(this, gImage.getId(), vote, new JsonHttpResponseHandler() {

                @Override
                public void onSuccess(JSONObject response) {
                    super.onSuccess(response);
                    if (ImageListFragment.voteMap != null) {
                        ImageListFragment.voteMap.put(gImage.getId(), vote);
                    }
                }

                @Override
                public void onFailure(Throwable e, JSONObject errorResponse) {
                    super.onFailure(e, errorResponse);
                    AppMsg.makeText(ImageActivity.this, getString(R.string.general_error), AppMsg.STYLE_ALERT).show();
                }

                @Override
                public void onFailure(Throwable e, JSONArray errorResponse) {
                    super.onFailure(e, errorResponse);
                    AppMsg.makeText(ImageActivity.this, getString(R.string.general_error), AppMsg.STYLE_ALERT).show();
                }
            });
        }
        else if (tokenStatus == EXPIRED_TOKEN) {
            FlurryAgent.logEvent(Constants.Flurry.TOKEN_REFRESH_CALLED);
            ImgyApi.getAccessTokenFromRefresh(ImageActivity.this, new JsonHttpResponseHandler() {
                @Override
                public void onFailure(Throwable e, JSONObject errorResponse) {
                    super.onFailure(e, errorResponse);
                    AppMsg.makeText(ImageActivity.this, getString(R.string.general_error), AppMsg.STYLE_ALERT).show();
                    FlurryAgent.logEvent(Constants.Flurry.TOKEN_REFRESH_FAILED);
                }

                @Override
                public void onSuccess(JSONObject response) {
                    super.onSuccess(response);
                    ImgyApi.saveAuthToken(ImageActivity.this, response);
                    voteOnImage(vote);
                }
            });
        }
        else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(
                    Uri.parse(String.format("https://api.imgur.com/oauth2/authorize?client_id=%s&response_type=%s&state=%s", IMGUR_CLIENT_ID, "code", "useless")));
            startActivity(intent);
        }
    }

    private void hydrateGImage(final GalleryImage gImage) {
        if (gImage.getAnimated()) {
            image.setVisibility(View.GONE);

            WebView gifView = new WebView(ImageActivity.this);
            gifView.setId(0X100);
            gifView.setScrollContainer(false);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, gImage.getHeight().intValue()*3);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            gifView.setLayoutParams(params);
            String html = "<body >\n <img id=\"resizeImage\" src=\"" + gImage.getLink() + "\" width=\"100%\" alt=\"\" />\n </body>";
            gifView.loadData(html, "text/html", "utf-8");

            gifView.setBackgroundColor(Color.parseColor("#333333"));
            viewContainer.addView(gifView);
            info.bringToFront();
        }
        else if (gImage.getHeight().intValue()/gImage.getWidth().intValue() > 3) {
            image.setVisibility(View.GONE);

            WebView gifView = new WebView(ImageActivity.this);
            gifView.setId(0X100);
            gifView.setScrollContainer(false);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            gifView.setLayoutParams(params);
//            gifView.loadUrl(gImage.getLink());
            String html = "<body >\n <img id=\"resizeImage\" src=\"" + gImage.getLink() + "\" width=\"100%\" alt=\"\" />\n </body>";
            gifView.loadData(html, "text/html", "utf-8");
            gifView.setBackgroundColor(Color.parseColor("#333333"));
            gifView.getSettings().setSupportZoom(true);
            gifView.getSettings().setBuiltInZoomControls(true);
            viewContainer.addView(gifView);
            info.bringToFront();
        }
        else {
            // The new size we want to scale to
            Display display = getWindowManager().getDefaultDisplay();
            final int REQUIRED_SIZE = display.getWidth();

            // Find the correct scale value. It should be the power of 2.
            int width_tmp = gImage.getWidth().intValue(), height_tmp = gImage.getHeight().intValue();
            int scale = 1;
            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE) {
                    break;
                }
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }
            image.setSampleSize(scale);
            image.setImageUrl(gImage.getLink());
        }

        if (gImage.getVote() != null) {
            if (gImage.getVote().equals(UP_VOTE_STRING)) {
                upvote.setImageDrawable(getResources().getDrawable(R.drawable.up_green_256));
                vote_status = UPVOTE;
            }
            else if (gImage.getVote().equals(DOWN_VOTE_STRING)) {
                downvote.setImageDrawable(getResources().getDrawable(R.drawable.down_red_256));
                vote_status = DOWNVOTE;
            }
        }

        caption.setText(gImage.getTitle());
        if (gImage.getUps() != null)
            upvotes.setText(gImage.getUps().toString());
        if (gImage.getDowns() != null)
            downvotes.setText(gImage.getDowns().toString());
        author.setText(gImage.getAccount_url());

        ImgyApi.getImageComments(ImageActivity.this, gImage.getId(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(JSONObject response) {
                super.onSuccess(response);
                try {
                    JSONArray jData = response.getJSONArray("data");
                    try {
                        comments = objectMapper.readValue(String.valueOf(jData), new TypeReference<List<Comment>>() { });
                        if (gImage.getAccount_url() != null) {
                            menuList.setAdapter(new CommentAdapter(ImageActivity.this, comments, gImage.getAccount_url()));
                        }
                        else {
                            menuList.setAdapter(new CommentAdapter(ImageActivity.this, comments, ""));
                        }
                        Log.d("IMGY", "Comments count: " + Integer.toString(comments.size()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        AppMsg.makeText(ImageActivity.this, getString(R.string.image_error), AppMsg.STYLE_ALERT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    AppMsg.makeText(ImageActivity.this, getString(R.string.image_error), AppMsg.STYLE_ALERT).show();
                }

            }

            @Override
            public void onFailure(Throwable e, JSONObject errorResponse) {
                super.onFailure(e, errorResponse);
                AppMsg.makeText(ImageActivity.this, getString(R.string.image_error), AppMsg.STYLE_ALERT).show();
            }
        });
    }

    private void hydrateSImage(final SubredditImage sImage) {
        if (sImage.getType().contains("gif")) {
            image.setVisibility(View.GONE);

            WebView gifView = new WebView(ImageActivity.this);
            gifView.setId(0X100);
            gifView.setScrollContainer(false);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, sImage.getHeight().intValue()*3);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            gifView.setLayoutParams(params);
            String html = "<body >\n <img id=\"resizeImage\" src=\"" + sImage.getLink() + "\" width=\"100%\" alt=\"\" />\n </body>";
            gifView.loadData(html, "text/html", "utf-8");

            gifView.setBackgroundColor(Color.parseColor("#333333"));
            viewContainer.addView(gifView);
            info.bringToFront();
        }
        else if (sImage.getHeight().intValue()/sImage.getWidth().intValue() > 3) {
            image.setVisibility(View.GONE);

            WebView gifView = new WebView(ImageActivity.this);
            gifView.setId(0X100);
            gifView.setScrollContainer(false);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            gifView.setLayoutParams(params);
//            gifView.loadUrl(gImage.getLink());
            String html = "<body >\n <img id=\"resizeImage\" src=\"" + sImage.getLink() + "\" width=\"100%\" alt=\"\" />\n </body>";
            gifView.loadData(html, "text/html", "utf-8");
            gifView.setBackgroundColor(Color.parseColor("#333333"));
            gifView.getSettings().setSupportZoom(true);
            gifView.getSettings().setBuiltInZoomControls(true);
            viewContainer.addView(gifView);
            info.bringToFront();
        }
        else {
            // The new size we want to scale to
            Display display = getWindowManager().getDefaultDisplay();
            final int REQUIRED_SIZE = display.getWidth();

            // Find the correct scale value. It should be the power of 2.
            int width_tmp = sImage.getWidth().intValue(), height_tmp = sImage.getHeight().intValue();
            int scale = 1;
            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE) {
                    break;
                }
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }
            image.setSampleSize(scale);
            image.setImageUrl(sImage.getLink());
        }

        caption.setText(sImage.getTitle());
        if (sImage.getUps() != null)
            upvotes.setText(sImage.getUps().toString());
        if (sImage.getDowns() != null)
            downvotes.setText(sImage.getDowns().toString());
        menu.setEnabled(false);
        upvote.setVisibility(View.GONE);
        downvote.setVisibility(View.GONE);
        author.setVisibility(View.GONE);
        comment.setVisibility(View.GONE);

    }
}
