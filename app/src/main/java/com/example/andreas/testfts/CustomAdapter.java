package com.example.andreas.testfts;

import android.content.Context;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by andreas on 7/22/15.
 */
public class CustomAdapter extends BaseAdapter {

    private final List<UserResult> userResultList;
    private final LayoutInflater inflater;

    public CustomAdapter(
            List<UserResult> userResultList,
            Context context) {
        this.userResultList = userResultList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return userResultList.size();
    }

    @Override
    public Object getItem(int i) {
        return userResultList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).hashCode();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.user_item, viewGroup, false);
            holder = new ViewHolder();
            holder.fullNameText = (TextView) view.findViewById(R.id.text_full_name);
            holder.statusText = (TextView) view.findViewById(R.id.text_status);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        UserResult userResult = (UserResult) getItem(i);

        if (userResult.offsets != null) {
            Spannable spannable = Spannable.Factory.getInstance().newSpannable(userResult.fullName);
            for (Offset offset : userResult.offsets) {
                spannable.setSpan(new BackgroundColorSpan(0xFFFFFF00), offset.start, offset.end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            holder.fullNameText.setText(spannable);
        } else {
            holder.fullNameText.setText(userResult.fullName);
        }

        holder.statusText.setText(userResult.status);

        return view;
    }

    private static class ViewHolder {
        TextView fullNameText;
        TextView statusText;
    }
}
