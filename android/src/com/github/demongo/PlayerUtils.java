package com.github.demongo;

import android.os.ParcelUuid;
import android.support.annotation.NonNull;

import com.github.demongo.Map.MapActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.UUID;

public class PlayerUtils {
    public static boolean isCurrentPlayer(ParcelUuid uuid, UUID playerId){
        if (uuid==null){
            return false;
        }
        return uuid.getUuid().toString().equals(playerId.toString());
    }


    public static void updateEp(Stash stash, FirebaseFirestore db, long additionalPlayerEP){
        db.collection("players").document(MapActivity.playerId.toString()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                long oldEp = (long) task.getResult().get("ep");
                db.collection("players").document(MapActivity.playerId.toString()).set(new HashMap<String,Object>(){{
                    put("ep",oldEp + additionalPlayerEP);
                }});
                stash.setFilled(stash.getFilled() - additionalPlayerEP);
                db.collection("stashes").document(stash.getId().toString()).set(stash.getMap());
            }
        });
    }

    public static void clearStash(Stash stash, FirebaseFirestore db) {
        updateEp(stash,db,stash.getFilled());
    }
}
