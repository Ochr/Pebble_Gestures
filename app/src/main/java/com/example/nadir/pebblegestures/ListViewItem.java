package com.example.nadir.pebblegestures;

import android.graphics.drawable.Drawable;

public class ListViewItem {
    public final Drawable icon;       // the drawable for the ListView item ImageView
    public final String title;        // the text for the ListView item title
    public String description;  // the text for the ListView item description
    public int id;
    public ListViewItem(Drawable icon, String title, String description, int id) {
        this.icon = icon;
        this.title = title;
        this.description = description;
        this.id = id;
    }
}
