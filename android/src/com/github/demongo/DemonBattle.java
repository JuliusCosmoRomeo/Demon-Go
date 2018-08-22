package com.github.demongo;

import android.util.Log;

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

    //returns the amount of stolen ep from the stash
    public static long attackStash(Demon attacker, ArrayList<Demon> defenders, Stash stash){

        final String TAG = "demon-go-battle";
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
                        return stash.getFilled();
                    }
                }
            } else {
                Log.i(TAG, "defeated every defender");
                //attacker won and killed all defenders
                //update firestore -> remove all demons from stash
                //update firestore -> take eps from stash
                //update firestore -> update attacker hp from null stash
                return stash.getFilled();
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
                        return 0;
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
}
