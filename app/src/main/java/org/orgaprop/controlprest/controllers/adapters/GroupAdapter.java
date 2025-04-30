package org.orgaprop.controlprest.controllers.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.orgaprop.controlprest.R;

import java.util.List;
import java.util.function.Consumer;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {

    private final List<String> groupNames;
    private final List<String> groupIds;
    private final Consumer<String> onItemClickListener;

    public GroupAdapter(List<String> groupNames, List<String> groupIds, Consumer<String> onItemClickListener) {
        this.groupNames = groupNames;
        this.groupIds = groupIds;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.group_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = groupNames.get(position);
        String id = groupIds.get(position);

        holder.textView.setText(name);
        holder.textView.setTag(id);

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.accept(id);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groupNames.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.group_item_nom);
        }
    }
}
