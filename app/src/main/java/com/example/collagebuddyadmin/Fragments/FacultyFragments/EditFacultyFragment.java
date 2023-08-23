package com.example.collagebuddyadmin.Fragments.FacultyFragments;

import static android.app.Activity.RESULT_OK;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.collagebuddyadmin.Models.FacultyDataModel;
import com.example.collagebuddyadmin.Models.NoticeDataModel;
import com.example.collagebuddyadmin.R;
import com.example.collagebuddyadmin.databinding.FragmentEditFacultyBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;

public class EditFacultyFragment extends BottomSheetDialogFragment {



    public EditFacultyFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private FragmentEditFacultyBinding binding;

    private final int ReqCode = 1;
    private Bitmap bitmap;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private String imageUrl = "";

    private ProgressDialog progressDialog;
    private Calendar calendar;
   private  FacultyDataModel selectedFaculty;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentEditFacultyBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        calendar = Calendar.getInstance();
        progressDialog = new ProgressDialog(getContext());
        databaseReference = FirebaseDatabase.getInstance().getReference("FacultyDetails");
        storageReference = FirebaseStorage.getInstance().getReference();

        // Populate the spinner with designation options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.designation_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDesignation.setAdapter(adapter);

        Bundle bundle = getArguments();
     selectedFaculty = (FacultyDataModel) bundle.getSerializable("selectedFaculty");
        if (bundle != null) {
            // Use the selectedFaculty data to pre-fill edit fields
            if (selectedFaculty != null) {
                binding.name.setText(selectedFaculty.getName());
                binding.spinnerDesignation.setSelection(getDesignationPosition(selectedFaculty.getDesignation()));
                binding.contact.setText(selectedFaculty.getContact());

                if(selectedFaculty.getImageUrl().equalsIgnoreCase("none")){
                    binding.textAddImage.setVisibility(View.VISIBLE);
                }
                Glide.with(this)
                        .asBitmap()
                        .load(selectedFaculty.getImageUrl())
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                binding.imageProfile.setImageBitmap(resource);
                                binding.textAddImage.setVisibility(View.INVISIBLE);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }
                        });
            }
        }



        binding.imageProfile.setOnClickListener(v -> {
            openGallery();
        });

        binding.btnCancel.setOnClickListener(v -> {dismiss();});


        // Initialize your views and set click listeners here
        binding.btnSave.setOnClickListener(v -> {
            if (validateFields()){
                    if(selectedFaculty.getImageUrl().equalsIgnoreCase("none")){
                        updateFacultyData(selectedFaculty.getUniqueKey());
                    }
                    else{
                        UploadFacultyImage(selectedFaculty.getUniqueKey(),selectedFaculty.getImageUrl());
                    }

            }
        });

      return  view;
    }

    private void updateFacultyData(String facultyId) {
        progressDialog.setMessage("Updating");
        progressDialog.show();

        DatabaseReference facultyRef = databaseReference.child(facultyId);

        // Proceed to update faculty data
        FacultyDataModel updatedFacultyData = new FacultyDataModel(
                binding.name.getText().toString(),
                binding.spinnerDesignation.getSelectedItem().toString(),
                imageUrl,
                binding.contact.getText().toString(),
                facultyId
        );

        facultyRef.setValue(updatedFacultyData)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    showToast("Faculty Detail Updated");
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    showToast("Oops! Something went wrong");
                });
    }
    private void deleteNotice(String oldImgUrl) {
        // Delete image from Firebase Storage

            StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(oldImgUrl);
            storageReference.delete()
                    .addOnSuccessListener(aVoid -> {
                    })
                    .addOnFailureListener(exception -> {
                        showToast("Image deletion failed: " + exception.getMessage());
                    });
    }
    private void UploadFacultyImage(String facultyId,String oldImgUrl) {
        progressDialog.setMessage("Uploading");
        progressDialog.show();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] finalImg = byteArrayOutputStream.toByteArray();
        final StorageReference filePath = storageReference.child("FacultyPictures").child(System.currentTimeMillis() + ".jpg");

        final UploadTask uploadTask = filePath.putBytes(finalImg);
        uploadTask.addOnCompleteListener(requireActivity(), new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    imageUrl = String.valueOf(uri);
                                    deleteNotice(oldImgUrl);
                                    updateFacultyData(facultyId);
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



    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, ReqCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ReqCode && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            try {
                // Use the fragment's context to access getContentResolver
                bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
                binding.textAddImage.setVisibility(View.GONE);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            binding.imageProfile.setImageBitmap(bitmap);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
    private boolean validateFields() {
        if (binding.name.getText().toString().isEmpty()) {
            binding.name.setError("Enter Name");
            binding.name.requestFocus();
            return false;
        }  else if (binding.spinnerDesignation.getSelectedItem().toString().equalsIgnoreCase("Select Designation")) {
            showToast("Select Designation");
            return false;
        } else if (binding.contact.getText().toString().isEmpty()) {
            binding.contact.setError("Enter Contact");
            binding.name.requestFocus();
            return false;
        } else {
            return true;
        }
    }
    private int getDesignationPosition(String designation) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.designation_options, android.R.layout.simple_spinner_item
        );

        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            if (adapter.getItem(i).equals(designation)) {
                return i;
            }
        }

        return 0; // Default position if not found
    }

}