package com.example.inclass05_amad;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.estimote.coresdk.common.requirements.SystemRequirementsChecker;
import com.estimote.coresdk.observation.region.beacon.BeaconRegion;
import com.estimote.coresdk.recognition.packets.Beacon;
import com.estimote.coresdk.service.BeaconManager;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.UUID;

public class ShoppingActivity extends AppCompatActivity implements AddToCartInterface{

    private Button goToCartButton;
    private RecyclerView shoppingItemsRecyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private BeaconManager beaconManager;
    private BeaconRegion region;
    OkHttpClient client,client1;

    private ArrayList<Product> ShoppingItemArrayList;
    private ArrayList<Product> SelectedItemsArrayList;
    ShoppingItemAdapter shoppingItemAdapter;
    Gson gson = new Gson();
    String currentZone="All";
    int flag=-1;
    HashMap<Beacon, Integer>  map= new HashMap<>();
    int cycle=0;
    boolean check=false;
    List<Beacon> majority =  new ArrayList<>();
    int i=0;
    int[] num = new int[3];
    int minor=0;
    int id=-1;
    int major=0;
    int c=0;


    @Override
    protected void onPostResume() {

        SelectedItemsArrayList=new ArrayList<>();
        for(int i=0;i<ShoppingItemArrayList.size();i++)
            ShoppingItemArrayList.get(i).isAdded=false;
        shoppingItemsRecyclerView.setAdapter(new ShoppingItemAdapter(this,ShoppingItemArrayList,this));
        super.onPostResume();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping);
        setTitle("Shop");

        client = new OkHttpClient();
        client1 = new OkHttpClient();

        ShoppingItemArrayList = new ArrayList<>();
        SelectedItemsArrayList = new ArrayList<>();

        goToCartButton = findViewById(R.id.goToCartBtn);
        shoppingItemsRecyclerView = findViewById(R.id.shoppingItemsRV);

        shoppingItemsRecyclerView.setHasFixedSize(true);
        layoutManager=new LinearLayoutManager(ShoppingActivity.this);
        shoppingItemsRecyclerView.setLayoutManager(layoutManager);

        ShoppingItemArrayList.clear();
        shoppingItemAdapter = new ShoppingItemAdapter(this,ShoppingItemArrayList,ShoppingActivity.this);
        shoppingItemsRecyclerView.setAdapter(shoppingItemAdapter);
        getShoppingItems();

        beaconManager = new BeaconManager(this);

        // (100, 5)
        beaconManager.setForegroundScanPeriod(30,2);

        beaconManager.setRangingListener(new BeaconManager.BeaconRangingListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onBeaconsDiscovered(BeaconRegion beaconRegion, List<Beacon> list) {
                if(cycle==3)
                {
                    cycle=0;
                    id = majorityElement(num);
                    if(id==0)
                    {
                        minor = 61548;
                        major = 47152;
                    }
                    else if(id==1)
                    {
                        minor = 44931;
                        major = 41072;
                    }
                    else if(id==2)
                    {
                        minor=56751;
                        major=15326;
                    }
                    num = new int[3];
                    i=0;
                }else{
                    cycle++;

                    if(list.get(0).getMinor()==61548)
                        num[i]=0;
                    else if(list.get(0).getMinor()==44931)
                        num[i]=1;
                    else if(list.get(0).getMinor()==56751)
                        num[i]=2;
                    i++;
                }

                Log.d("demoop", String.valueOf(cycle));


                if (minor!=0 && major!=0) {

                    if (minor == 61548  && major == 47152 &&  flag!=0){
                        ShoppingItemArrayList.clear();
                        shoppingItemAdapter.notifyDataSetChanged();
                        flag=0;
                        getShoppingListByRegion("grocery");
                    }
                    else if(minor == 44931  && major == 41072 && flag!=1){
                        ShoppingItemArrayList.clear();
                        shoppingItemAdapter.notifyDataSetChanged();
                        flag=1;
                        getShoppingListByRegion("produce");

                    }
                    else if(minor == 56751  && major == 15326 && flag!=2){
                        ShoppingItemArrayList.clear();
                        shoppingItemAdapter.notifyDataSetChanged();
                        flag=2;
                        getShoppingListByRegion("lifestyle");
                    }
                    else if(minor != 61548 && minor != 44931 && minor != 56751 && (flag==1 || flag==0 || flag==2)){
                        ShoppingItemArrayList.clear();
                        shoppingItemAdapter.notifyDataSetChanged();
                        flag=-1;
                        getShoppingItems();
                    }

                }
                else if(c==0){
                    ShoppingItemArrayList.clear();
                    shoppingItemAdapter.notifyDataSetChanged();
                    flag=-1;
                    c=1;
                    getShoppingItems();
                }

            }
        });
        region = new BeaconRegion("ranged region",
                UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), null, null);

        goToCartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Gson gson = new Gson();
                String SelectedItems = gson.toJson(SelectedItemsArrayList);
                Intent goToCartIntent = new Intent(ShoppingActivity.this, AddToCartActivity.class);
                goToCartIntent.putExtra("list_as_string", SelectedItems);
                startActivity(goToCartIntent);
            }
        });
    }

    private void getShoppingListByRegion(String region) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject jsonObject = new JSONObject();

        try {
          //
            //  Log.d("region is", "getShoppingListByRegion: "+region);
            jsonObject.put("region",region);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String jsonString=jsonObject.toString();
        RequestBody requestBody = RequestBody.create(JSON, jsonString);

        Request request = new Request.Builder()
                .url("http://ec2-3-17-204-58.us-east-2.compute.amazonaws.com:4000/support/getProductsByRegion")
                .post(requestBody)
                .build();

        client1.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();

                try {
                    JSONArray jsonArray = new JSONArray(json);
                    for (int i=0;i< jsonArray.length();i++) {
                        Product product = gson.fromJson(jsonArray.getString(i), Product.class);
                        ShoppingItemArrayList.add(product);
                      //  Log.d("shopping list", "onResponse: "+ShoppingItemArrayList);
                    }

                 //   shoppingItemAdapter.notifyDataSetChanged();
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            shoppingItemAdapter = new ShoppingItemAdapter(ShoppingActivity.this,ShoppingItemArrayList,ShoppingActivity.this);
                            shoppingItemsRecyclerView.setAdapter(shoppingItemAdapter);
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startRanging(region);
            }
        });
    }

    @Override
    protected void onPause() {
        beaconManager.stopRanging(region);

        super.onPause();
    }


    private void getShoppingItems() {

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject jsonObject = new JSONObject();

        Request request = new Request.Builder()
                .url("http://ec2-3-17-204-58.us-east-2.compute.amazonaws.com:4000/support/getProducts")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();

                try {
                        JSONArray jsonArray = new JSONArray(json);
                        for (int i=0;i< jsonArray.length();i++) {
                        Product product = gson.fromJson(jsonArray.getString(i), Product.class);
                        ShoppingItemArrayList.add(product);
                    }
                   // shoppingItemAdapter.notifyDataSetChanged();
Collections.sort(ShoppingItemArrayList, new Comparator<Product>() {
    @Override
    public int compare(Product t1, Product t2) {
        return  (t1.getRegion().compareTo(t2.getRegion()));
    }
});

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            shoppingItemAdapter = new ShoppingItemAdapter(ShoppingActivity.this,ShoppingItemArrayList,ShoppingActivity.this);
                            shoppingItemsRecyclerView.setAdapter(shoppingItemAdapter);
                        }
                    });


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    @Override
    public void addToCart(Product product) {
        SelectedItemsArrayList.add(product);
    }

    @Override
    public void removeFromCart(Product product) {
        SelectedItemsArrayList.remove(product);
    }


    public int majorityElement(int[] nums) {
        int count =0;
        Integer candidate = null;

        for(int num : nums)
        {
            if(count == 0)
                candidate = num;

            count = count + (candidate == num ? 1 : -1);
        }
        return candidate;
    }


}
