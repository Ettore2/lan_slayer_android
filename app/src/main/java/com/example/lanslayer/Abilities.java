package com.example.lanslayer;

public abstract class Abilities {
    public static final int TARGET_ALLAY = 0,TARGET_ENEMY = 1,TARGET_BOTH = 2,TARGET_SELF = 3;

    public boolean chargeWithTurns, chargeWithHitsTaken, chargeWithHitsDealt;
    public final String displayName;
    public final int cd;
    public final int target;
    public final Heroes owner;
    public int currCd;
    protected Abilities(Heroes owner,String displayName,int target, int cd){
        this.owner = owner;
        this.displayName = displayName;
        this.target = target;
        this.cd = cd;

        chargeWithTurns = true;
        chargeWithHitsTaken = false;
        chargeWithHitsDealt = false;

        currCd = 0;


    }
    public void execute (Heroes target){
        currCd = cd + (owner.getSlow() > 0 ? 1 : 0);
    }
    public String getDescriptionStart(){
        return "";
    }
    public String getDescriptionEnd(){
        String s = "";
        if(chargeWithTurns){
            s = s + " turns,";
        }
        if(chargeWithHitsDealt){
            s = s + " damage dealt,";
        }
        if(chargeWithHitsTaken){
            s = s + " damage taken,";
        }

        //if it does never recharge
        if(s.equals("")){
            s = " never,";
        }

        return "\ncountdown: "+ currCd + "/" + cd + "\nrecharge by:" + s.substring(0,s.length()-1);
    }
    public String getDescription (){
        return getDescriptionStart() + getDescriptionEnd();
    }
    public boolean isAvailable(){
        return currCd == 0;

    }
    protected int scaleToLevel(int val){
        return owner.scaleToLevel(val);
    }


    private static abstract class AttackAbility extends Abilities{
        private final int dmg;

        private AttackAbility(Heroes owner, String displayName, int cd, int dmg) {
            super(owner, displayName, TARGET_ENEMY, cd);
            this.dmg = dmg;
        }
        public int getCurrentDmg(){
            return (scaleToLevel(dmg)) * (100 + owner.getExtraDamageDealtPercent()) / 100;
        }

        @Override
        public void execute(Heroes target) {
            super.execute(target);
            owner.decreaseCdsByDamageDealt();
            target.takeDamage(getCurrentDmg());
        }
        @Override
        public String getDescriptionStart(){
            return " deal " + getCurrentDmg() + " damage to an enemy";
        }
    }//already have owner.decreaseCdsByDamageDealt()



    public static class Archer_attack extends AttackAbility{
        public Archer_attack(Heroes owner){
            super(owner, "attack", 1,90);
        }
    }
    public static class Archer_special_Shot extends AttackAbility{
        int slowVal = 3;
        public Archer_special_Shot(Heroes owner){
            super(owner, "special shot", 4,140);
        }

        @Override
        public void execute(Heroes target) {
            super.execute(target);
            target.addSlow(slowVal);
        }

        @Override
        public String getDescription() {
            return super.getDescriptionStart() + " and add " + slowVal + " slow to him" + getDescriptionEnd();
        }
    }

    public static class Barbarian_attack extends AttackAbility{
        public Barbarian_attack(Heroes owner){
            super(owner, "attack", 1,100);
        }
    }
    public static class Barbarian_rage extends Abilities{
        public final int extraDmgPercent = 60, extraHealPercent = 60;
        public final int effectsDuration = 4;

        public Barbarian_rage(Heroes owner){
            super(owner, "rage", TARGET_SELF, 6);
        }


        @Override
        public void execute(Heroes target) {
            super.execute(owner);
            target.setExtraDamageDealt(extraDmgPercent, effectsDuration);
            target.setExtraHeal(extraHealPercent, effectsDuration);
        }
        @Override
        public String getDescriptionStart() {
            return "gain " + extraDmgPercent + "% extra damage and " +
                    extraHealPercent + "% extra heal for " + effectsDuration + " turns";
        }
    }

    public static class Healer_attack extends AttackAbility{
        public static int slowVal = 1;
        public Healer_attack(Heroes owner) {
            super(owner, "attack", 1, 60);
        }

        @Override
        public void execute(Heroes target) {
            super.execute(target);
            target.addSlow(1);
        }

        @Override
        public String getDescriptionStart() {
            return super.getDescriptionStart() + " and add " + slowVal + " slow to him";
        }
    }
    public static class Healer_heal extends Abilities{
        private final int healPercent = 33;
        public Healer_heal(Heroes owner) {
            super(owner, "heal", TARGET_ALLAY, 3);
        }
        @Override
        public void execute(Heroes target){
            super.execute(target);
            target.healPercent(scaleToLevel(healPercent));
        }
        @Override
        public String getDescriptionStart() {
            return "heal " + scaleToLevel(healPercent) + "% of an ally's max health";
        }
    }

    public static class Mage_attack extends AttackAbility{
        public Mage_attack(Heroes owner) {
            super(owner, "attack", 1, 80);
        }
    }
    public static class Mage_stun extends Abilities{
        public Mage_stun(Heroes owner) {
            super(owner, "stun", TARGET_ENEMY, 6);
        }
        @Override
        public void execute(Heroes target){
            super.execute(target);
            for(int i = 0; i < target.abilities.length; i++){
                target.abilities[i].currCd = target.abilities[i].cd + 1;
            }
        }
        @Override
        public String getDescriptionStart() {
            return "set the current cd of all the target ability to their use cd + 1";
        }
    }

    public static class Valkyrie_attack extends AttackAbility{
        public Valkyrie_attack(Heroes owner) {
            super(owner, "attack", 1, 70);
        }
    }
    public static class Valkyrie_special_attack extends AttackAbility{
        public Valkyrie_special_attack(Heroes owner) {
            super(owner, "special attack", 3, 140);
            chargeWithTurns = false;
            chargeWithHitsTaken = true;
        }
        @Override
        public void execute(Heroes target){
            super.execute(target);//already deal damage to the target
            for(int i = 0; i < GameInstance.getInstance().playerHeroes.length; i++){
                if(GameInstance.getInstance().isAPlayerHero(target.getBattleId())){
                    if(GameInstance.getInstance().playerHeroes[i] != target){
                        GameInstance.getInstance().playerHeroes[i].takeDamage(getCurrentDmg());
                    }
                }else{
                    if(GameInstance.getInstance().enemyHeroes[i] != target){
                        GameInstance.getInstance().enemyHeroes[i].takeDamage(getCurrentDmg());
                    }
                }
            }
        }
        @Override
        public String getDescriptionStart() {
            return "deal " + getCurrentDmg() + " to all enemies";
        }
    }

    public static class Vampire_attack extends AttackAbility{
        public static final int DMG_HEAL_PERCENT = 33;
        public Vampire_attack(Heroes owner){
            super(owner, "attack", 1,80);
        }

        @Override
        public void execute(Heroes target) {
            currCd = cd + (owner.getSlow() > 0 ? 1 : 0);
            owner.decreaseCdsByDamageDealt();
            owner.heal(target.takeDamage(getCurrentDmg())*DMG_HEAL_PERCENT/100);
        }
        @Override
        public String getDescriptionStart() {
            return super.getDescriptionStart() + " and heal " + DMG_HEAL_PERCENT + "% of the damage dealt";
        }
    }
    public static class Vampire_bite extends AttackAbility{
        public static final int DMG_HEAL_PERCENT = 33;
        public static final int EXTRA_DMG_TAKEN_PERCENT = 100;
        public static final int EXTRA_DMG_TAKEN_DURATION = 3;
        public Vampire_bite(Heroes owner){
            super(owner, "bite", 5,40);
        }

        @Override
        public void execute(Heroes target) {
            currCd = cd + (owner.getSlow() > 0 ? 1 : 0);
            owner.decreaseCdsByDamageDealt();
            owner.heal(target.takeDamage(getCurrentDmg())*DMG_HEAL_PERCENT/100);
            target.setExtraDamageGained(EXTRA_DMG_TAKEN_PERCENT, EXTRA_DMG_TAKEN_DURATION);
        }

        @Override
        public String getDescription() {
            return super.getDescriptionStart() + " and heal " + DMG_HEAL_PERCENT + "% of the damage dealt and add " + EXTRA_DMG_TAKEN_PERCENT + " % extra damage taken tor " +
                    EXTRA_DMG_TAKEN_DURATION + " turns to him" + getDescriptionEnd();
        }
    }



}
