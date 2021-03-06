package com.corgrimm.imgy.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.corgrimm.imgy.R;
import com.corgrimm.imgy.models.AlbumImage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopj.android.image.SmartImageView;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Anderson
 * Date: 3/22/13
 * Time: 9:08 AM
 */
public class AlbumImageAdapter extends BaseAdapter {

    private Context ctx;
    List<AlbumImage> images;
    ObjectMapper objectMapper;


    public AlbumImageAdapter(Context c, List<AlbumImage> images) {
        ctx = c;
        this.images = images;
        objectMapper = new ObjectMapper();

    }

    @Override
    public int getCount() {
        return images.size();
    }


    @Override
    public Object getItem(int arg0) {
        return arg0;
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View albumImageView = convertView;
        if (albumImageView == null) {
            albumImageView = LayoutInflater.from(ctx).inflate(R.layout.album_image, parent, false);
        }
        AlbumImage image = images.get(position);

        RelativeLayout viewContainer = (RelativeLayout) albumImageView.findViewById(R.id.viewContainer);
        SmartImageView imageView = (SmartImageView) albumImageView.findViewById(R.id.imgImage);
        TextView description = (TextView) albumImageView.findViewById(R.id.description);
        TextView title = (TextView) albumImageView.findViewById(R.id.title);

        title.setVisibility(View.GONE);
        description.setVisibility(View.GONE);

        if (viewContainer.getChildCount() > 1) {
            int i = 0;
            while (i < viewContainer.getChildCount()) {
                if (viewContainer.getChildAt(i).getClass() == WebView.class) {
                    viewContainer.removeViewAt(i);
                    break;
                }
                i++;
            }
        }

        if (image.getAnimated()) {
            imageView.setVisibility(View.GONE);

            WebView gifView = new WebView(ctx);
            gifView.setId(0X100);
            gifView.setScrollContainer(false);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, image.getHeight().intValue()*3);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            gifView.setLayoutParams(params);
            String html = "<body >\n <img id=\"resizeImage\" src=\"" + image.getLink() + "\" width=\"100%\" alt=\"\" />\n </body>";
            gifView.loadData(html, "text/html", "utf-8");
            gifView.setBackgroundColor(Color.parseColor("#333333"));
            description.bringToFront();
            title.bringToFront();
            viewContainer.addView(gifView);
        }
        else {
            imageView.setImageDrawable(null);

            // The new size we want to scale to
            Display display =((Activity) ctx).getWindowManager().getDefaultDisplay();
            final int REQUIRED_SIZE = display.getWidth();

            // Find the correct scale value. It should be the power of 2.
            int width_tmp = image.getWidth().intValue(), height_tmp = image.getHeight().intValue();
            int scale = 1;
            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE) {
                    break;
                }
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }
            imageView.setSampleSize(scale);
            imageView.setImageUrl(image.getLink());
        }

        if (image.getTitle() != null && !image.getTitle().equals("")) {
            title.setVisibility(View.VISIBLE);
            title.setText(image.getTitle());
        }
        if (image.getDescription() != null && !image.getDescription().equals("")) {
            description.setVisibility(View.VISIBLE);
            description.setText(image.getDescription());
        }
            //drawableManager.fetchDrawableOnThread(((GalleryImage)object).getLink(), imageView);
        return albumImageView;

    }
}
