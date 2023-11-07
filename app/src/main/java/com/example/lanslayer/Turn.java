package com.example.lanslayer;

public class Turn {
    public static final char SEPARATOR = '_';
    public final int abilityId;
    public final int activeHeroBattleId, passiveHeroBattleId;

    Turn(int activeHeroBattleId, int abilityId,  int passiveHeroBattleId){
        this.activeHeroBattleId = activeHeroBattleId;
        this.abilityId = abilityId;
        this.passiveHeroBattleId = passiveHeroBattleId;
    }

    public static Turn empyTurn(){
        return new Turn(-1,-1,-1);

    }

    Turn(String turnToString){
        int offset = - 1;
        activeHeroBattleId = Integer.parseInt(turnToString.substring(offset + 1, turnToString.indexOf(SEPARATOR, offset + 1)));
        offset = turnToString.indexOf(SEPARATOR, offset + 1);
        abilityId = Integer.parseInt(turnToString.substring(offset + 1, turnToString.indexOf(SEPARATOR, offset + 1)));
        offset = turnToString.indexOf(SEPARATOR, offset + 1);
        passiveHeroBattleId = Integer.parseInt(turnToString.substring(offset + 1));

    }

    public boolean isEmpty(){
        return activeHeroBattleId == -1 || abilityId == -1 || passiveHeroBattleId == -1;

    }
    @Override
    public String toString() {
        return "" + activeHeroBattleId + SEPARATOR + abilityId + SEPARATOR + passiveHeroBattleId;
    }
}
