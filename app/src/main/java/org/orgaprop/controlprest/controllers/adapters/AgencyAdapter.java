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

public class AgencyAdapter extends RecyclerView.Adapter<AgencyAdapter.ViewHolder> {

    private final List<String> agencyNames;
    private final List<String> agencyIds;
    private final Consumer<String> onItemClickListener;

    public AgencyAdapter(List<String> agencyNames, List<String> agencyIds, Consumer<String> onItemClickListener) {
        this.agencyNames = agencyNames;
        this.agencyIds = agencyIds;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.agence_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = agencyNames.get(position);
        String id = agencyIds.get(position);

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
        return agencyNames.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.agence_item_name);
        }
    }
}
