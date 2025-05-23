package com.example.huanhuanmusicplayerapp;

import android.content.Context;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.huanhuanmusicplayerapp.model.Song;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songList;
    private OnSongClickListener listener;
    private OnSongActionListener actionListener;
    private int contextMenuPosition = -1;  // 添加这个变量来跟踪长按的位置

    public interface OnSongClickListener {
        void onSongClick(int position, Song song);
    }

    public interface OnSongActionListener {
        void onSongEdit(int position, Song song);
        void onSongDelete(int position, Song song);
    }

    public SongAdapter(List<Song> songList, OnSongClickListener listener) {
        this.songList = songList;
        this.listener = listener;
    }

    public void setOnSongActionListener(OnSongActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.bind(song);
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    // 更新歌曲
    public void updateSong(int position, Song song) {
        if (position >= 0 && position < songList.size()) {
            songList.set(position, song);
            notifyItemChanged(position);
        }
    }

    // 删除歌曲
    public void removeSong(int position) {
        if (position >= 0 && position < songList.size()) {
            songList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, songList.size() - position);
        }
    }

    // 获取上下文菜单位置
    public int getContextMenuPosition() {
        return contextMenuPosition;
    }

    // 设置上下文菜单位置
    public void setContextMenuPosition(int position) {
        this.contextMenuPosition = position;
    }

    class SongViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        ImageView albumCover;
        TextView songTitle;
        TextView artistName;
        TextView duration;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            albumCover = itemView.findViewById(R.id.item_album_cover);
            songTitle = itemView.findViewById(R.id.item_song_title);
            artistName = itemView.findViewById(R.id.item_artist_name);
            duration = itemView.findViewById(R.id.item_duration);

            // 设置点击监听器
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSongClick(position, songList.get(position));
                }
            });

            // 设置长按菜单
            itemView.setOnLongClickListener(v -> {
                setContextMenuPosition(getAdapterPosition());  // 保存位置
                v.showContextMenu();
                return true;
            });

            // 注册上下文菜单创建监听器
            itemView.setOnCreateContextMenuListener(this);
        }

        void bind(Song song) {
            songTitle.setText(song.getTitle());
            artistName.setText(song.getArtist());
            duration.setText(formatTime(song.getDuration()));

            // 这里可以使用Glide或Picasso等库加载图片
            // 简单起见，我们这里不实现图片加载
        }

        private String formatTime(int milliseconds) {
            return String.format(Locale.getDefault(), "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                    TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))
            );
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            // 确保使用正确的位置
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                setContextMenuPosition(position);
            }
            
            // 使用资源ID直接引用菜单项
            menu.add(0, R.id.action_edit, 0, "编辑");
            menu.add(0, R.id.action_delete, 1, "删除");
        }
    }

    // 添加处理菜单项点击的方法
    public boolean onContextItemSelected(MenuItem item) {
        int position = contextMenuPosition;
        if (position < 0 || position >= songList.size()) {
            return false;
        }

        Song song = songList.get(position);
        int itemId = item.getItemId();
        
        if (itemId == R.id.action_edit) {
            if (actionListener != null) {
                actionListener.onSongEdit(position, song);
            }
            return true;
        } else if (itemId == R.id.action_delete) {
            if (actionListener != null) {
                actionListener.onSongDelete(position, song);
            }
            return true;
        }
        
        return false;
    }
    
    // 修改以前的方法声明，不再需要额外的position参数
    public boolean onContextItemSelected(MenuItem item, int position) {
        // 保留此方法以兼容旧代码，但内部使用新方法
        contextMenuPosition = position;
        return onContextItemSelected(item);
    }
}