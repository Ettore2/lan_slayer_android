package com.example.lanslayer;

import static com.example.lanslayer.MainActivity.debug;

public abstract class Heroes {
    public static final int LEVEL_MULTIPLIER = 10;
    public static final int ABILITIES_X_HERO = 2;
    public static final int ID_BARBARIAN = 0, ID_ARCHER = 1, ID_HEALER = 2, ID_MAGE = 3, ID_VALKYRIE = 4, ID_VAMPIRE = 5;
    public static final String[] NAMES = {"Barbarian","Archer","Healer", "Mage", "Valkyrie", "Vampire"};
    public static final int[] IMAGES_IDS = {R.drawable.barbarian,R.drawable.archer,R.drawable.healer,R.drawable.mage,R.drawable.valkyrie,R.drawable.vampire};
    public static final int DEAD_IMAGE_ID = R.drawable.skull;
    public static final int NUMBER_OF_HEROES = NAMES.length;

    private final int id;
    private int battleId;
    private final int level;
    private final int maxHealth;
    public final Abilities[] abilities = new Abilities[ABILITIES_X_HERO];
    private int currHealth;
    private int extraDamageDealtPercent;
    private int extraDamageDealtDuration;
    private int extraDamageGainedPercent;
    private int extraDamageGainedDuration;
    private int extraHealPercent;
    private int extraHealDuration;
    private int slow;

    protected Heroes(int id, int level, int maxHealth) {
        this.id = id;
        this.battleId = -1;
        this.level = level;
        this.maxHealth = scaleToLevel(maxHealth);
        this.currHealth = this.maxHealth;

        extraDamageDealtPercent = 0;
        extraDamageDealtDuration = 0;
        extraDamageGainedDuration = 0;
        extraDamageGainedPercent = 0;
        extraHealPercent = 0;
        extraHealDuration = 0;
        slow = 0;

        for(int i = 0; i < this.abilities.length; i++){
            this.abilities[i] = null;
        }
    }

    //getters
    public int getId() {
        return id;
    }
    public int getBattleId() {
        return battleId;
    }
    public String getName() {
        return NAMES[id];
    }
    public String getCompleteName() {
        return NAMES[id] +"(" + battleId + ")";
    }
    public int getMaxHealth() {
        return maxHealth;
    }
    public int getCurrHealth() {
        return currHealth;
    }
    public int getCurrHealthPercentage(){
        return currHealth * 100 / maxHealth;
    }
    public int getLevel() {
        return level;
    }
    public int getExtraDamageDealtPercent() {
        return extraDamageDealtPercent;
    }
    public int getExtraDamageDealtDuration() {
        return extraDamageDealtDuration;
    }
    public int getExtraHealDuration() {
        return extraHealDuration;
    }
    public int getExtraHealPercent() {
        return extraHealPercent;
    }
    public void addSlow(int slow) {
        this.slow += slow;
        if(slow < 0){
            slow = 0;
        }
    }

    //setters
    public void setExtraDamageDealt(int percentage, int duration) {
        this.extraDamageDealtPercent = percentage;
        this.extraDamageDealtDuration = duration;
    }
    public void setExtraDamageGained(int percentage, int duration) {
        this.extraDamageGainedPercent = percentage;
        this.extraDamageGainedDuration = duration;
    }
    public void setExtraHeal(int percentage, int duration) {
        this.extraHealPercent = percentage;
        this.extraHealDuration = duration;
    }
    public int getSlow() {
        return slow;
    }
    public void setBattleId(int battleId) {
        this.battleId = battleId;
    }

    //other methods
    public static Heroes getHero(int id, int level){
        switch (id){
            case ID_ARCHER:
                return new Archer(level);
            case ID_HEALER:
                return new Healer(level);
            case ID_MAGE:
                return new Mage(level);
            case ID_VALKYRIE:
                return new Valkyrie(level);
            case ID_VAMPIRE:
                return new Vampire(level);
            case ID_BARBARIAN:
            default:
                return new Barbarian(level);
        }
    }
    public int takeDamage(int dmg){

        for(int i = 0; i < Heroes.ABILITIES_X_HERO; i++){
            if(abilities[i].currCd > 0 && abilities[i].chargeWithHitsTaken){
                abilities[i].currCd = abilities[i].currCd - 1;
            }
        }

        int dmgAmount = (currHealth - dmg*(100 + extraDamageGainedPercent)/100  >= 0) ?  dmg*(100 + extraDamageGainedPercent)/100 : currHealth;
        currHealth -= dmgAmount;
        //debug("" + dmgAmount);
        return dmgAmount;
    }
    public int heal(int heal){
        int healAmount = ((currHealth + (heal + heal*extraHealPercent/100)) <= maxHealth ? heal + heal*extraHealPercent/100 : maxHealth - currHealth);
        currHealth += healAmount;
        return healAmount;
    }
    public int healPercent(int percentHeal){
        int healAmount =(currHealth + maxHealth*(percentHeal * (100 + extraHealPercent)/100)/100 <= maxHealth) ? maxHealth*(percentHeal * (100 + extraHealPercent)/100)/100 : maxHealth - currHealth;
        currHealth += healAmount;
        return healAmount;
    }
    public int scaleToLevel(int val){
        return val + LEVEL_MULTIPLIER * (level - 1);
    }
    public int getImageId(){
        if(isAlive()){
            return IMAGES_IDS[id];
        }else{
            return DEAD_IMAGE_ID;
        }
    }
    public static int getImageId(int heroId){
        return IMAGES_IDS[heroId];
    }
    public boolean isAlive(){
        return currHealth > 0;

    }
    public boolean useAbility(int abilityId, Heroes target){
        if(abilityId >= 0 && abilityId < ABILITIES_X_HERO){
            abilities[abilityId].execute(target);
            return true;
        }
        return false;
    }
    public void decreaseStatusesByTurns(){
        if(extraDamageDealtDuration > 0){
            extraDamageDealtDuration--;
            if(extraDamageDealtDuration == 0){
                extraDamageDealtPercent = 0;
            }
        }
        if(extraDamageGainedDuration > 0){
            extraDamageGainedDuration--;
            if(extraDamageGainedDuration == 0){
                extraDamageGainedPercent = 0;
            }
        }
        if(extraHealDuration > 0){
            extraHealDuration --;
            if(extraHealDuration == 0){
                extraHealPercent = 0;
            }
        }
        if(slow > 0){
            slow--;
        }


    }
    public void decreaseCdsByTurns(){
        for(int i = 0; i < Heroes.ABILITIES_X_HERO; i++){
            if(abilities[i].currCd > 0 && abilities[i].chargeWithTurns){
                abilities[i].currCd = abilities[i].currCd - 1;
            }
        }

    }
    public void decreaseCdsByDamageDealt(){
        for(int i = 0; i < Heroes.ABILITIES_X_HERO; i++){
            if(abilities[i].currCd > 0 && abilities[i].chargeWithHitsDealt){
                abilities[i].currCd = abilities[i].currCd - 1;
            }
        }

    }//to use before increasing the cd of the ability
    public String getDescription(){
        String s = getCompleteName() + " level " + level + "\nhp: " + currHealth + "/" + maxHealth;

        s = s + "\nStatuses:";
        if(extraDamageDealtDuration > 0){
            s = s + "\n#" + extraDamageDealtPercent + "% extra damage dealt for " + extraDamageDealtDuration;
        }
        if(extraDamageGainedDuration > 0){
            s = s + "\n#" + extraDamageGainedPercent + "% extra damage gained for " + extraDamageGainedDuration;
        }
        if(extraHealDuration > 0){
            s = s + "\n#" + extraHealPercent + "% extra heal gained for " + extraHealDuration;
        }
        if(slow > 0){
            s = s +"\n# slow " + slow;
        }

        s = s + "\nAbilities:";
        for(int i = 0; i < abilities.length; i++){
            s = s + "\n@" + abilities[i].displayName +" | " + abilities[i].getDescription();
        }


        return s;
    }




    public static class Barbarian extends Heroes{
        public Barbarian(int level) {
            super(ID_BARBARIAN, level, 980);
            abilities[0] = new Abilities.Barbarian_attack(this);
            abilities[1] = new Abilities.Barbarian_rage(this);
        }
    }
    public static class Archer extends Heroes{
        public Archer(int level) {
            super(ID_ARCHER, level, 890);
            abilities[0] = new Abilities.Archer_attack(this);
            abilities[1] = new Abilities.Archer_special_Shot(this);
        }
    }
    public static class Healer extends Heroes{
        public Healer(int level) {
            super(ID_HEALER, level, 660);
            abilities[0] = new Abilities.Healer_attack(this);
            abilities[1] = new Abilities.Healer_heal(this);
        }
    }
    public static class Mage extends Heroes{
        public Mage(int level) {
            super(ID_MAGE, level, 810);
            abilities[0] = new Abilities.Mage_attack(this);
            abilities[1] = new Abilities.Mage_stun(this);
        }
    }
    public static class Valkyrie extends Heroes{
        public Valkyrie(int level) {
            super(ID_VALKYRIE, level, 930);
            abilities[0] = new Abilities.Valkyrie_attack(this);
            abilities[1] = new Abilities.Valkyrie_special_attack(this);
        }
    }
    public static class Vampire extends Heroes{
        public Vampire(int level) {
            super(ID_VAMPIRE, level, 710);
            abilities[0] = new Abilities.Vampire_attack(this);
            abilities[1] = new Abilities.Vampire_bite(this);
        }
    }

}
