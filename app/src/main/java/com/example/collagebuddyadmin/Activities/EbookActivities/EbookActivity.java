package com.example.collagebuddyadmin.Activities.EbookActivities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.collagebuddyadmin.Activities.NoticeActivities.NoticeActivity;
import com.example.collagebuddyadmin.Adapters.EbookAdapter;
import com.example.collagebuddyadmin.Adapters.NoticeAdapter;
import com.example.collagebuddyadmin.Models.EbookDataModel;
import com.example.collagebuddyadmin.Models.NoticeDataModel;
import com.example.collagebuddyadmin.R;
import com.example.collagebuddyadmin.databinding.ActivityEbookBinding;
import com.example.collagebuddyadmin.databinding.ActivityNoticeBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class EbookActivity extends AppCompatActivity {

   private ActivityEbookBinding binding;

    private final int ReqCode = 1;
    private Uri pdfData;

    private DatabaseReference databaseReference;
    private String eBookName;
    private StorageReference storageReference;

    private ProgressDialog progressDialog;
    private List<NoticeDataModel> eBookList;
    private Calendar calendar;
    private Bitmap thumbnail;
    private String thumbnailUrl ="";
    private EbookAdapter ebookAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEbookBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        calendar = Calendar.getInstance();
        progressDialog = new ProgressDialog(this);
        databaseReference = FirebaseDatabase.getInstance().getReference("pdf");
        storageReference = FirebaseStorage.getInstance().getReference();

           binding.selectbookBtn.setOnClickListener(view -> {
               openGallery();
           });

           binding.uploadEbookBtn.setOnClickListener(v -> {
               if(binding.bookTitle.getText().toString().isEmpty())
               {
                   binding.bookTitle.setError("Empty");
                   binding.bookTitle.requestFocus();
               }
               else if(pdfData==null)
               {
                   showToast("Please Upload PDF");
               }
               else {
                   Upload_Ebook_Thumbnail();
               }

           });
    }

    private void fetchDataFromFirebase() {
        // Attach a ValueEventListener to the database reference
        databaseReference.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eBookList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    NoticeDataModel notice = dataSnapshot.getValue(NoticeDataModel.class);
                    eBookList.add(notice);
                }
                if (eBookList.isEmpty()) {
                    binding.noNotice.setVisibility(View.VISIBLE);
                    binding.prevNotice.setVisibility(View.GONE);
                } else {
                    binding.noNotice.setVisibility(View.GONE);
                    binding.prevNotice.setVisibility(View.VISIBLE);
                }
                // Notify the adapter of data change
             //   ebookAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle errors if any
                showToast("Failed to fetch data");
            }
        });
    }

    private void Upload_Ebook_Thumbnail() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] finalImg = byteArrayOutputStream.toByteArray();
        final StorageReference filePath = storageReference.child("Ebook_Thumbnails").child(System.currentTimeMillis() + ".jpg");

        final UploadTask uploadTask = filePath.putBytes(finalImg);
        uploadTask.addOnCompleteListener(EbookActivity.this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    thumbnailUrl = String.valueOf(uri);
                                    uploadPdf();
                                }
                            });
                        }
                    });
                } else {
                    progressDialog.dismiss();
                    showToast("Oops! Something went wrong");
                }
            }
        });
    }


    private void uploadPdf() {
        progressDialog.setTitle("Please wait...");
        progressDialog.setMessage("Uploading E-Book...");
        progressDialog.show();
        StorageReference reference = storageReference.child("pdf/"+binding.bookTitle.getText().toString()+".pdf");

        reference.putFile(pdfData).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Get the download URL of the uploaded file
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();

                uriTask.addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> uriTask) {
                        if (uriTask.isSuccessful()) {
                            Uri uri = uriTask.getResult();
                            // Upload the data with the obtained download URL
                            uploadData(uri.toString());
                        } else {
                            showToast("Failed to retrieve download URL");
                        }
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showToast("Oops! Something went wrong");
                progressDialog.dismiss();
            }
    }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showToast("Oops! Something went wrong");
                progressDialog.dismiss();
            }
        });
    }
    private void uploadData(String pdfUrl) {
        DatabaseReference eBookReference = databaseReference;
        final String uniqueKey = eBookReference.push().getKey();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MM-yy hh:mm a");
        String date = currentDate.format(calendar.getTime());

        EbookDataModel ebookDataModel = new EbookDataModel(
                binding.bookTitle.getText().toString(),thumbnailUrl,date,pdfUrl
        );

        HashMap data = new HashMap();
        data.put("pdfTitle",binding.bookTitle.getText().toString());
        data.put("pdfUrl",pdfUrl);
        data.put("thumbnailUrl",thumbnailUrl);

                eBookReference.child(uniqueKey).setValue(ebookDataModel).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressDialog.dismiss();

                showToast("E-Book Uploaded successfully");
                clear();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();

                showToast("Failed to upload E-Book");
            }
        });

    }

    private void openGallery() {
        Intent intent = new Intent();
        String[] mimeTypes = {"application/pdf", "application/msword", "application/vnd.ms-powerpoint"};
        intent.setType("*/*");  // This will allow any file type
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);  // Set allowed MIME types
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Document"), ReqCode);
    }

    @SuppressLint("Range")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ReqCode && resultCode == RESULT_OK) {
         pdfData =data.getData();
            thumbnail = generatePdfThumbnail(pdfData);
            if (thumbnail != null) {
                binding.prevImage.setImageBitmap(thumbnail);
            } else {
                showToast("Failed to generate PDF thumbnail");
            }
            if(pdfData.toString().startsWith("content://")){
                Cursor cursor = null;
                try {
                    cursor =EbookActivity.this.getContentResolver().query(pdfData,null,null,null,null);
                    if(cursor!=null && cursor.moveToFirst()){
                        eBookName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }else if(pdfData.toString().startsWith("file://"))
            {
                eBookName = new File(pdfData.toString()).getName();
            }
            binding.bookFileName.setText(eBookName.toString());
        }
    }

    private Bitmap generatePdfThumbnail(Uri pdfUri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(pdfUri, "r");
            if (parcelFileDescriptor != null) {
                PdfRenderer pdfRenderer = new PdfRenderer(parcelFileDescriptor);
                PdfRenderer.Page page = pdfRenderer.openPage(0);

                Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                page.close();
                pdfRenderer.close();
                parcelFileDescriptor.close();

                return bitmap;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void clear() {
        binding.bookTitle.setText(null);
        binding.prevImage.setImageBitmap(null);
        binding.bookFileName.setText("No file Selected");
    }
}




