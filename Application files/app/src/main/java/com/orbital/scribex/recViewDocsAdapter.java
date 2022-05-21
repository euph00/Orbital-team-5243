package com.orbital.scribex;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class recViewDocsAdapter extends RecyclerView.Adapter<recViewDocsAdapter.ViewHolder>{
    private static final String TAG = "recViewDocsAdapter";

    private List<Document> docs = new ArrayList<>();
    private Context context;

    public recViewDocsAdapter(Context context) {
        this.context = context;
    }

    public void setDocs(List<Document> docs) {
        this.docs = docs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: Called");
        holder.name_doc.setText(docs.get(position).getName());
        //TODO: set document image when implemented
        holder.snippet_doc.setText(docs.get(position).getSnippet());
        holder.parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, docs.get(position).getName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return docs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private CardView parent;
        private ImageView img_doc;
        private TextView name_doc;
        private TextView snippet_doc;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            parent = itemView.findViewById(R.id.parent);
            img_doc = itemView.findViewById(R.id.img_doc);
            name_doc = itemView.findViewById(R.id.name_doc);
            snippet_doc = itemView.findViewById(R.id.snippet_doc);
        }
    }
}
