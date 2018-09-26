package com.github.demongo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DemonGallery extends Activity {

    private static final String TAG = "demon-go-gallery";

    public enum Action {
        Attack,
        Defend,
        Deposit,
        Add
    }

    public static final ParcelUuid nullStashId = new ParcelUuid(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    private Action action;
    private CustomPagerAdapter mCustomPagerAdapter;
    private ViewPager mViewPager;
    private final List<Demon> demons = new ArrayList<Demon>();

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demon_gallery);

        this.action = (Action) getIntent().getExtras().get("action");

        db.collection("stashes").document(nullStashId.toString()).collection("demons").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for (QueryDocumentSnapshot demonDoc : task.getResult()) {
                    Log.i(TAG, "demon id " + demonDoc.getId());
                    Demon demon = new Demon(demonDoc.getData());
                    demons.add(demon);
                }
                mCustomPagerAdapter = new CustomPagerAdapter(DemonGallery.this);
                mViewPager = findViewById(R.id.pager);
                mViewPager.setAdapter(mCustomPagerAdapter);
            }
        });

        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    class CustomPagerAdapter extends PagerAdapter {

        Context mContext;
        LayoutInflater mLayoutInflater;

        public CustomPagerAdapter(Context context) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return demons.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((LinearLayout) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View itemView = mLayoutInflater.inflate(R.layout.demon_view, container, false);

            ImageView imageView = itemView.findViewById(R.id.image_view);
            final Demon demon = demons.get(position);
            imageView.setImageResource((int)demon.getResource());
            TextView demonName = itemView.findViewById(R.id.demon_name);
            demonName.setText(demon.getName());
            TextView demonType = itemView.findViewById(R.id.demon_type);
            demonType.setText(demon.getType().toString());
            switch(demon.getType()){
                case Imp:
                    demonType.setTextColor(ContextCompat.getColor(DemonGallery.this, R.color.imp));
                    break;
                case Foliot:
                    demonType.setTextColor(ContextCompat.getColor(DemonGallery.this, R.color.foliot));
                    break;
                case Djinn:
                    demonType.setTextColor(ContextCompat.getColor(DemonGallery.this, R.color.djinn));
                    break;
                case Afrit:
                    demonType.setTextColor(ContextCompat.getColor(DemonGallery.this, R.color.afrit));
                    break;
                case Marid:
                    demonType.setTextColor(ContextCompat.getColor(DemonGallery.this, R.color.marid));
                    break;
                default:
                    break;
            }

            TextView hpText = itemView.findViewById(R.id.hp);
            hpText.setText(demon.getHp() + "/" + demon.getMaxHp() + " hp");
            ImageButton actionButton = itemView.findViewById(R.id.action_button);
            switch (action) {
                case Attack:
                    actionButton.setImageResource(R.drawable.icons8_schwert);
                    break;
                case Defend:
                    actionButton.setImageResource(R.drawable.icons8_schild);
                    break;
                case Deposit:
                    actionButton.setImageResource(R.drawable.icons8_gelddose);
                    break;
                case Add:
                    actionButton.setVisibility(View.INVISIBLE);
                    break;
                default:
                    break;
            }
            actionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "returning to MapActivity");
                    Intent data = new Intent();
                    //---set the data to pass back---
                    data.putExtra("demon", demon);
                    data.putExtra("action", action);
                    setResult(RESULT_OK, data);
                    finish();
                }

            });

            container.addView(itemView);
            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }
    }

}
