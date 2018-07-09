package com.github.demongo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DemonGallery extends Activity {

    private static final String TAG = "demon-go-gallery";

    public enum Action {
        Attack,
        Defend,
        Deposit
    }

    private Action action;
    private CustomPagerAdapter mCustomPagerAdapter;
    private ViewPager mViewPager;
    private final List<Demon> demons = new ArrayList<Demon>(){{
        add(new Demon("luschi",100,30,230, R.drawable.notification_icon,Demon.Type.Imp));
        add(new Demon("flupsi",110,50,400, R.drawable.notification_icon,Demon.Type.Foliot));
        add(new Demon("schnucksi",150,60,780, R.drawable.notification_icon,Demon.Type.Djinn));
        add(new Demon("blubsi",220,90,1440, R.drawable.notification_icon,Demon.Type.Afrit));
        add(new Demon("superstubsi",350,150,5200, R.drawable.notification_icon,Demon.Type.Marid));
    }};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demon_gallery);
        mCustomPagerAdapter = new CustomPagerAdapter(this);

        mViewPager = findViewById(R.id.pager);
        mViewPager.setAdapter(mCustomPagerAdapter);
        this.action = (Action) getIntent().getExtras().get("action");
        //getActionBar().setDisplayHomeAsUpEnabled(true);
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
            imageView.setImageResource(demon.getResource());
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
            switch(action){
                case Attack:
                    actionButton.setImageResource(R.drawable.icons8_schwert);
                    break;
                case Defend:
                    actionButton.setImageResource(R.drawable.icons8_schild);
                    break;
                case Deposit:
                    actionButton.setImageResource(R.drawable.icons8_gelddose);
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
