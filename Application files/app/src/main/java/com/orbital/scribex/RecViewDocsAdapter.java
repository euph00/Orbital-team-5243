package com.orbital.scribex;

import android.content.Context;
import android.content.Intent;
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

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class RecViewDocsAdapter extends RecyclerView.Adapter<RecViewDocsAdapter.ViewHolder>{
    private static final String TAG = "RecViewDocsAdapter";

    private List<Document> docs = new ArrayList<>();
    private Context context;

    public RecViewDocsAdapter(Context context) {
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
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: Called");
        holder.name_doc.setText(docs.get(position).getName());
        Picasso.with(context).load(docs.get(position).getUrl()).rotate(90).into(holder.img_doc);
        holder.snippet_doc.setText(docs.get(position).getSnippet());
        holder.parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(context, docs.get(holder.getAdapterPosition()).getName(), Toast.LENGTH_SHORT).show();
                openDocumentViewActivity(holder);
            }
        });
    }

    private void openDocumentViewActivity(ViewHolder holder) {
        Intent viewDocumentIntent = new Intent(context, DocumentViewActivity.class);
        //pass document object to DocumentViewActivity
        viewDocumentIntent.putExtra("Document", docs.get(holder.getAdapterPosition()));
        context.startActivity(viewDocumentIntent);
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
