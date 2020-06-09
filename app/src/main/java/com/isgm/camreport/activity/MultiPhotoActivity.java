package com.isgm.camreport.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.isgm.camreport.BuildConfig;
import com.isgm.camreport.R;
import com.isgm.camreport.model.MultiPhoto;
import com.isgm.camreport.recyclerview.MultiPhotoAdapter;
import com.isgm.camreport.roomdb.DatabaseClient;
import com.isgm.camreport.roomdb.History;
import com.isgm.camreport.utility.APIService;
import com.isgm.camreport.utility.RetrofitAgent;


import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MultiPhotoActivity extends BaseActivity implements MultiPhotoAdapter.OnPhotoListener {

    private static final String TAG = MultiPhotoActivity.class.getName();
    RecyclerView recyclerView;
    Button button;
    ProgressBar progressBar;
    MultiPhotoAdapter multiPhotoAdapter;
    List<History> multiPhotoUtilList = new ArrayList<>();
    int upload_sum_count;
    int uploaded_sum_count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_multi_photo);

        gettingAllPhoto();
        init();


    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void init() {
        progressBar = (ProgressBar) findViewById(R.id.progressBar_cyclic);
        button = (Button) findViewById(R.id.button);
        button.setEnabled(false);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Do file uploading to server

                photoUploadingToServer();
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.card_view_recycler_list);
        recyclerView.setLayoutManager(
                new GridLayoutManager(this, getResources().getInteger(R.integer.shr_column_count)));
    }

    private void photoUploadingToServer() {
        progressBar.setVisibility(View.VISIBLE);
        for(History history: multiPhotoUtilList){
           // Log.i(TAG, "doInBackground: => " + history.isSelected());
            if(history.isSelected() == true) {
                sendDataToServer(history);
            }
        }
        progressBar.setVisibility(View.GONE);
        Log.i(TAG, "doInBackground: => upload_sum count => " + upload_sum_count);
       onBackPressed();

    }

    private void sendDataToServer(History history) {
        //Intialize variable
        byte[] imageBytes = new byte[0];
        final Gson gson = new Gson();
        // image to ByteArray
        try {
            InputStream inputStream = new FileInputStream(history.getImagePath());
            imageBytes = toByteArray(inputStream);
            inputStream.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        // Covert Image to Base 64 String
        if (imageBytes != null) {
            String imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            imageString = "data:image/jpg;base64," + imageString;
            // Upload Data to Server
            try {
                RetrofitAgent retrofit = RetrofitAgent.getInstance();
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("altitude", history.getAltitude());
                jsonObject.addProperty("name", history.getImage());
                jsonObject.addProperty("image_name", history.getImage());
                jsonObject.addProperty("latitude", history.getLatitude());
                jsonObject.addProperty("longitude", history.getLongitude());
                jsonObject.addProperty("route_id", history.getRouteId());
                jsonObject.addProperty("category", history.getCategory());
                jsonObject.addProperty("activity_type", history.getActivityType());
                jsonObject.addProperty("basic", history.getFiberOperationType());
                jsonObject.addProperty("image_data", imageString);
                jsonObject.addProperty("remark", history.getRemark());
                jsonObject.addProperty("ver_code", BuildConfig.VERSION_NAME);
                JsonArray jsonArray = new JsonArray();
                jsonArray.add(jsonObject);
                APIService apiService = retrofit.getApiService();
                Call<ResponseBody> call = apiService.uploadImage(jsonArray);
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            JsonObject responseJsonObject;
                            try {
                                if (response.body() != null) {
                                    responseJsonObject = gson.fromJson(response.body().string(),
                                            JsonElement.class).getAsJsonObject();
                                    String status = responseJsonObject.get("status").getAsString();
                                    String error = responseJsonObject.get("error").getAsString();

                                    if (status.equals("success")) {
                                       // update progress bar and db
                                        uploaded_sum_count += 1;
                                        Log.i(TAG, "onResponse: => Succeessfule" + uploaded_sum_count);
                                        updateDbIsSentStatus(true,history.getId());


                                    } else {
                                        Log.i(TAG, "onResponse: => Failure " );
                                       // show toast for failure and update db
                                        updateDbIsSentStatus(false, history.getId());

                                    }
                                }
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                        // show toast for failure and update db
                        Log.i(TAG, "onFailure: Failure");
                        updateDbIsSentStatus(false, history.getId());

                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MultiPhotoActivity.this,
                    "Something went wrong! Please take the photo again!", Toast.LENGTH_SHORT).show();
        }

    }

    private void updateDbIsSentStatus(boolean b, int id) {



        class updateIsSentStatus extends AsyncTask<Void,Void,Void>{


            @Override
            protected Void doInBackground(Void... voids) {
                Log.i(TAG, "doInBackground: => " + b);
                DatabaseClient.getInstance(MultiPhotoActivity.this.getApplicationContext())
                        .getAppDatabase().historyDao().updateById(b,id);
                return null;
            }
        }
        new updateIsSentStatus().execute();

    }

    // convert image input to byte[]
    private byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] byteArray = new byte[1024];
        int length;
        // Read Bytes from the InputStream and Store them in byteArray
        while ((length = inputStream.read(byteArray)) != -1) {
            // Write Bytes from the byteArray into OutputStream
            byteArrayOutputStream.write(byteArray, 0, length);
        }
        return byteArrayOutputStream.toByteArray();
    }


    private void gettingAllPhoto() {



        Log.i(TAG, "gettingAllPhoto: Reach Here");

        // Gettign all photo from local db
          class getAllDataFromDB extends AsyncTask<Void,Void,Void>{

              @Override
              protected Void doInBackground(Void... voids) {
                  multiPhotoUtilList =  DatabaseClient.getInstance(getApplicationContext())
                         .getAppDatabase().historyDao().getNotSendData(false);;
                 //Please Change Here, take data from DB
                  multiPhotoAdapter = new MultiPhotoAdapter(getBaseContext(), multiPhotoUtilList,MultiPhotoActivity.this::onPhotoClick);
                  recyclerView.setAdapter(multiPhotoAdapter);

                  for(History history: multiPhotoUtilList){
                      Log.i(TAG, "doInBackground: => id +> " + history.getId());
                  }

                  return null;
              }

          }
          new getAllDataFromDB().execute();
    }


    @Override
    public void onPhotoClick(int position, List<History> multiPhotoUtilsList) {
        boolean check = false;
        upload_sum_count = 0;

        for(History multiPhoto: multiPhotoUtilsList){

            if(multiPhoto.isSelected() == true){
                upload_sum_count += 1;
                 check = true;

            }
        }
        if (check)
            button.setEnabled(true);
        else
            button.setEnabled(false);
    }
}