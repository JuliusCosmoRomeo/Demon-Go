package com.github.demongo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.github.demongo.Map.StashUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DemonBattle {
    final String TAG = "demon-go-battle";
    final Context context;
    final FirebaseFirestore db;
    public DemonBattle(Context context, FirebaseFirestore db){
        this.context = context;
        this.db = db;
    }

    //returns the amount of stolen ep from the stash
    public boolean attackStash(Demon attacker, ArrayList<Demon> defenders, Stash stash){
        String winText = attacker.getName() + " hat alle " + defenders.size() + " Verteidiger besiegt!";
        String loseText = "Leider ist " + attacker.getName() + " beim Angriff gestorben.";

        //order in which the defenders attack the attacking demon
        List<Integer> attackOrderList = IntStream.rangeClosed(0, defenders.size()-1)
                .boxed().collect(Collectors.toList());
        Collections.shuffle(attackOrderList);
        Queue<Integer> attackOrder = new LinkedList<Integer>(attackOrderList);
        Log.i(TAG, "attack order " + Arrays.toString(attackOrderList.toArray()));
        //order in which the attacker attacks the defenders
        List<Integer> defendOrderList = IntStream.rangeClosed(0, defenders.size()-1)
                .boxed().collect(Collectors.toList());
        Collections.shuffle(defendOrderList);
        Queue<Integer> defendOrder = new LinkedList<Integer>(defendOrderList);
        Log.i(TAG, "defend order " + Arrays.toString(attackOrderList.toArray()));

        //for every round
        while(true){
            //attacker begins and attacks one defender (in the defendOrder)
            Log.i(TAG, "attacker attacks");
            Integer defenderId = defendOrder.poll();
            if (defenderId!=null){
                Demon defender = defenders.get(defenderId);
                long newDefenderHP = attackDemon(attacker.getAttackPoints(),defender.getHp());
                defender.setHp(newDefenderHP);
                if (newDefenderHP>0){
                    defendOrder.offer(defenderId);
                } else {
                    Log.i(TAG, "defeated demon " + defender.toString());
                    if(defendOrder.size()==0){
                        Log.i(TAG, "defeated every defender");
                        updateFirestore(attacker,defenders,stash);
                        Toast.makeText(context, winText, Toast.LENGTH_LONG).show();
                        return true;
                    }
                }
            } else {
                Log.i(TAG, "defeated every defender");
                //attacker won and killed all defenders
                //update firestore -> remove all demons from stash
                //update firestore -> take eps from stash
                //update firestore -> update attacker hp from null stash
                updateFirestore(attacker,defenders,stash);
                Toast.makeText(context, winText, Toast.LENGTH_LONG).show();
                return true;
            }


            //now all defenders attack the attacker
            for (int i=0;i<defenders.size();i++){
                Log.i(TAG, "defender attacks");
                defenderId = attackOrder.poll();
                Demon defender = defenders.get(defenderId);
                if(defender.getHp()>0){
                    long newAttackerHP = attackDemon(defender.getAttackPoints(),attacker.getHp());
                    attacker.setHp(newAttackerHP);
                    if (newAttackerHP>0){
                        attackOrder.offer(defenderId);
                    } else {
                        Log.i(TAG, "lost fight :( ");
                        //attacker is dead, defenders won
                        //update firestore -> remove all demons from stash and update the hp of the surviving
                        //update firestore -> remove attacker from null stash
                        updateFirestore(attacker,defenders,stash);
                        Toast.makeText(context, loseText, Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
            }
        }
    }

    //returns the new hp of the attacked demon
    private static long attackDemon(long attackPts, long defenderHP){
        long newDefenderHP = defenderHP - ThreadLocalRandom.current().nextLong(attackPts/2, attackPts + 1);
        if (newDefenderHP<0){
            return 0;
        }
        return newDefenderHP;
    }

    private void updateFirestore(Demon attacker, ArrayList<Demon> defenders, Stash stash){
        String stashId = stash.getId().toString();

        for (Demon defender : defenders){
            DocumentReference defRef = db.collection("stashes").document(stashId).collection("demons").document(defender.getId().toString());
            if (defender.getHp()>0){
                defRef.set(defender.getMap());
            } else {
                defRef.delete();
            }
        }

        DocumentReference attRef = db.collection("stashes").document(DemonGallery.nullStashId.toString()).collection("demons").document(attacker.getId().toString());
        if (attacker.getHp()>0){
            attRef.set(attacker.getMap());
            stash.setHasDefenders(false);
            Log.i(TAG, "defeated stash " +stash.toString());
            if (stash.getFilled()==0){
                Log.i(TAG, "deleting defeated stash");
                db.collection("stashes").document(stashId).delete();
            } else {
                stash.setRadius(0);
                db.collection("stashes").document(stashId).set(stash.getMap());
            }
        } else {
            attRef.delete();
            StashUtils.updateRadius(db,stash);

        }
    }
}
