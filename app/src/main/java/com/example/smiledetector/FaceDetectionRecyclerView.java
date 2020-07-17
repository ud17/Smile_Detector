package com.example.smiledetector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FaceDetectionRecyclerView extends RecyclerView.Adapter<FaceDetectionRecyclerView.ViewHolder> {

    private Context mContext;
    private List<FaceDetectionModel> mData;

    public FaceDetectionRecyclerView(Context mContext, List<FaceDetectionModel> mData) {
        this.mContext = mContext;
        this.mData = mData;
    }

    @NonNull
    @Override
    public FaceDetectionRecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_face_detection , parent , false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FaceDetectionRecyclerView.ViewHolder holder, int position) {

        FaceDetectionModel faceDetectionModel = mData.get(position);

        holder.textView1.setText("Face " + String.valueOf(faceDetectionModel.getId()));
        holder.textView2.setText("Face " + faceDetectionModel.getText());

    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textView1;
        private TextView textView2;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            textView1 = itemView.findViewById(R.id.textView1);
            textView2 = itemView.findViewById(R.id.textView2);

        }
    }
}
