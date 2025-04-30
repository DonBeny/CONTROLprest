package org.orgaprop.controlprest.controllers.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.models.ListResidModel;

import java.util.List;
import java.util.function.Consumer;

public class ResidenceAdapter extends RecyclerView.Adapter<ResidenceAdapter.ViewHolder> {

    private final List<ListResidModel> residences;
    private final List<String> residenceIds;
    private final Consumer<String> onItemClickListener;

    public ResidenceAdapter(List<ListResidModel> residences, List<String> residenceIds, Consumer<String> onItemClickListener) {
        this.residences = residences;
        this.residenceIds = residenceIds;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.resid_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ListResidModel residence = residences.get(position);
        String id = residenceIds.get(position);

        // Configure the ViewHolder with residence data
        holder.textViewRef.setText(residence.getRef());
        holder.textViewName.setText(residence.getName());
        holder.textViewEntry.setText(residence.getEntry());
        holder.textViewAdr.setText(residence.getAdr());
        holder.textViewCity.setText(residence.getCity());
        holder.textViewLast.setText(residence.getLast());

        // Set background based on visited status
        if (residence.isVisited()) {
            holder.itemView.setBackground(AppCompatResources.getDrawable(
                    holder.itemView.getContext(), R.drawable.button_enabled));
        } else {
            holder.itemView.setBackground(AppCompatResources.getDrawable(
                    holder.itemView.getContext(), R.drawable.button_standard));
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.accept(id);
            }
        });
    }

    @Override
    public int getItemCount() {
        return residences.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewRef;
        final TextView textViewName;
        final TextView textViewEntry;
        final TextView textViewAdr;
        final TextView textViewCity;
        final TextView textViewLast;

        ViewHolder(View itemView) {
            super(itemView);
            textViewRef = itemView.findViewById(R.id.fiche_search_resid_ref);
            textViewName = itemView.findViewById(R.id.fiche_search_resid_name);
            textViewEntry = itemView.findViewById(R.id.fiche_search_resid_entry);
            textViewAdr = itemView.findViewById(R.id.fiche_search_resid_adr);
            textViewCity = itemView.findViewById(R.id.fiche_search_resid_city);
            textViewLast = itemView.findViewById(R.id.fiche_search_resid_last);
        }
    }
}
