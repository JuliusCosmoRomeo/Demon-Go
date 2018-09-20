package com.github.demongo.Map;

import android.support.annotation.NonNull;
import android.util.Log;

import com.github.demongo.Demon;
import com.github.demongo.Stash;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class StashUtils {


    public static void updateRadius(FirebaseFirestore db, Stash currentStash) {
        final String TAG = "demon-go-stash-utils";
        db.collection("stashes").document(currentStash.getId().toString()).collection("demons").get().addOnCompleteListener(
                new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        long totalDefenderHP = 0;
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Demon defender = new Demon(document.getData());
                                totalDefenderHP += defender.getHp();
                            }
                            double radius = (double)totalDefenderHP/1000;
                            //check if this radius is possible
                            db.collection("stashes").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    ArrayList<Stash> stashes = new ArrayList<>();
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        stashes.add(new Stash(document.getData()));
                                    }
                                    double maxRadius = MapUtils.getMaxRadius(currentStash, stashes);

                                    if (maxRadius != -1) {
                                        if (radius > maxRadius) {
                                            currentStash.setRadius(maxRadius);
                                        } else {
                                            currentStash.setRadius(radius);
                                        }
                                        currentStash.setHasDefenders(true);
                                        Log.i(TAG, "stash before update " + currentStash.toString());
                                        db.collection("stashes").document(currentStash.getId().toString()).set(currentStash.getMap());
                                    }

                                }
                            });

                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                }
        );


    }
}
