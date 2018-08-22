package com.github.demongo;

import android.os.ParcelUuid;

import java.util.UUID;

public class PlayerUtil {
    public static boolean isCurrentPlayer(ParcelUuid uuid, UUID playerId){
        if (uuid==null){
            return false;
        }
        return uuid.getUuid() == playerId;
    }
}
