package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterQuickBinding;
import com.fongmi.android.tv.setting.SiteHealthStore;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class QuickAdapter extends RecyclerView.Adapter<QuickAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Vod> mItems;
    private int width;
    private int nextFocusUp;
    private int nextFocusDown;

    public QuickAdapter(OnClickListener listener) {
        mListener = listener;
        mItems = new ArrayList<>();
        int space = ResUtil.dp2px(24) + ResUtil.dp2px(32);
        width = (ResUtil.getScreenWidth() - space) / 4;
    }

    public void addAll(List<Vod> items) {
        if (items.isEmpty()) return;
        int start = mItems.size();
        mItems.addAll(items);
        notifyItemRangeInserted(start, items.size());
    }

    public void remove(int position) {
        mItems.remove(position);
        notifyItemRemoved(position);
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public void setNextFocus(int nextFocusUp, int nextFocusDown) {
        if (this.nextFocusUp == nextFocusUp && this.nextFocusDown == nextFocusDown) return;
        this.nextFocusUp = nextFocusUp;
        this.nextFocusDown = nextFocusDown;
        notifyDataSetChanged();
    }

    public void setWidth(int width) {
        if (this.width == width) return;
        this.width = width;
        notifyDataSetChanged();
    }

    public Vod get(int position) {
        return mItems.get(position);
    }

    public int getBestPosition() {
        int position = 0;
        for (int i = 1; i < mItems.size(); i++) {
            long a = rank(mItems.get(i)), b = rank(mItems.get(position));
            if (a > b || (a == b && SiteHealthStore.compareVods(mItems.get(i), mItems.get(position)) < 0)) position = i;
        }
        return position;
    }

    /**
     * Content match first, site health second: a same-named music album from a healthy site used to beat
     * the actual show (auto-switch landed on a music collection that can never play). Penalize music
     * sites, favor episode remarks like "60集全".
     */
    private static long rank(Vod item) {
        long score = 0;
        String site = item.getSiteName() == null ? "" : item.getSiteName();
        String remark = item.getRemarks() == null ? "" : item.getRemarks();
        if (site.contains("音乐") || site.toLowerCase().contains("music")) score -= 1000;
        if (remark.matches(".*\\d+集.*") || remark.endsWith("全")) score += 500;
        if (remark.matches("\\d+")) score += 250;
        return score;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(AdapterQuickBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        holder.binding.getRoot().getLayoutParams().width = width;
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vod item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        holder.binding.site.setText(item.getSiteName());
        holder.binding.remark.setText(item.getRemarks());
        holder.binding.getRoot().setNextFocusUpId(nextFocusUp == 0 ? View.NO_ID : nextFocusUp);
        holder.binding.getRoot().setNextFocusDownId(nextFocusDown == 0 ? View.NO_ID : nextFocusDown);
        holder.binding.getRoot().setOnClickListener(v -> mListener.onItemClick(item));
    }

    public interface OnClickListener {

        void onItemClick(Vod item);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterQuickBinding binding;

        ViewHolder(@NonNull AdapterQuickBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
