package com.beetle.bauhinia;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.imkit.R;

public class MessageTimeBaseView extends MessageRowView {
    public MessageTimeBaseView(Context context) {
        super(context);
        final int contentLayout;
        contentLayout = R.layout.chat_content_text;

        View convertView;

        convertView = inflater.inflate(
                R.layout.chat_container_center, this);

        ViewGroup group = (ViewGroup)convertView.findViewById(R.id.content);
        group.addView(inflater.inflate(contentLayout, group, false));

        this.contentView = group;
    }

    public void setTimeBaseMessage(IMessage msg, String t) {
        super.setMessage(msg, true);
        TextView content = (TextView) findViewById(R.id.text);
        content.setText(t);
        requestLayout();
    }




}