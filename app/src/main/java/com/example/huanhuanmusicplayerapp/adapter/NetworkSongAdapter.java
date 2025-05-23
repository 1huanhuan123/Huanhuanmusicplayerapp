package com.example.huanhuanmusicplayerapp.adapter;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.huanhuanmusicplayerapp.R;
import com.example.huanhuanmusicplayerapp.model.NetworkSong;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NetworkSongAdapter extends RecyclerView.Adapter<NetworkSongAdapter.SongViewHolder> {

    private List<NetworkSong> songList;
    private OnSongClickListener listener;
    private int selectedPosition = -1; // 记录当前选中的项目
    private int loadingPosition = -1;  // 记录当前加载中的项目

    public interface OnSongClickListener {
        void onSongClick(int position, NetworkSong song);
    }

    public NetworkSongAdapter(List<NetworkSong> songList, OnSongClickListener listener) {
        this.songList = songList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_network_song, parent, false);
        return new SongViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        NetworkSong song = songList.get(position);
        holder.titleTextView.setText(song.getTitle());
        holder.artistTextView.setText(song.getArtist());
        holder.durationTextView.setText(formatDuration(song.getDuration()));
        holder.sourceTextView.setText(song.getSourceType());
        
        // 设置选中状态视觉反馈
        CardView cardView = (CardView) holder.itemView;
        if (position == selectedPosition) {
            cardView.setCardBackgroundColor(Color.parseColor("#E3F2FD"));
        } else {
            cardView.setCardBackgroundColor(Color.WHITE);
        }
        
        // 设置加载状态
        if (position == loadingPosition) {
            holder.loadingProgressBar.setVisibility(View.VISIBLE);
        } else {
            holder.loadingProgressBar.setVisibility(View.GONE);
        }
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && loadingPosition == -1) {
                int oldSelectedPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                loadingPosition = holder.getAdapterPosition();
                
                // 更新上一个选中项和当前选中项的视图
                if (oldSelectedPosition != -1) {
                    notifyItemChanged(oldSelectedPosition);
                }
                notifyItemChanged(selectedPosition);
                
                // 添加点击反馈动画
                addClickAnimation(cardView);
                
                // 触发点击回调
                listener.onSongClick(holder.getAdapterPosition(), song);
            }
        });
    }

    // 添加点击动画效果
    private void addClickAnimation(CardView cardView) {
        int colorFrom = Color.WHITE;
        int colorTo = Color.parseColor("#E3F2FD");
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(300);
        colorAnimation.addUpdateListener(animator -> 
            cardView.setCardBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimation.start();
    }
    
    // 重置加载状态
    public void resetLoadingState() {
        int previousLoading = loadingPosition;
        loadingPosition = -1;
        if (previousLoading != -1) {
            notifyItemChanged(previousLoading);
        }
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    // 格式化时间
    private String formatDuration(int duration) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView artistTextView;
        TextView durationTextView;
        TextView sourceTextView;
        ProgressBar loadingProgressBar;

        SongViewHolder(View view) {
            super(view);
            titleTextView = view.findViewById(R.id.network_song_title);
            artistTextView = view.findViewById(R.id.network_song_artist);
            durationTextView = view.findViewById(R.id.network_song_duration);
            sourceTextView = view.findViewById(R.id.network_song_source);
            loadingProgressBar = view.findViewById(R.id.song_loading_progress);
        }
    }
}
