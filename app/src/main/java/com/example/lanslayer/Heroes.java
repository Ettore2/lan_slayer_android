package com.example.lanslayer;

public abstract class Heroes {
    public static final int LEVEL_MULTIPLIER = 10;
    public static final int ABILITIES_X_HERO = 2;
    public static final int ID_BARBARIAN = 0, ID_ARCHER = 1, ID_HEALER = 2, ID_MAGE = 3, ID_VALKYRIE = 4, ID_VAMPIRE = 5, ID_DEMON = 6, ID_ELF = 7;
    public static final String[] NAMES = {"Barbarian","Archer","Healer", "Mage", "Valkyrie", "Vampire","Demon","Elf"};
    public static final int[] IMAGES_IDS = {R.drawable.barbarian,R.drawable.archer,R.drawable.healer,R.drawable.mage,R.drawable.valkyrie,R.drawable.vampire,R.drawable.demon,R.drawable.elf};
    public static final int DEAD_IMAGE_ID = R.drawable.skull;
    public static final int NUMBER_OF_HEROES = NAMES.length;

    private final int id;
    private int battleId;
    private final int level;
    private final int maxHealth;
    public final Abilities[] abilities = new Abilities[ABILITIES_X_HERO];
    private int currHealth;
    private int extraDamageDealtPercent, extraDamageDealtDuration;
    private int extraDamageGainedPercent, extraDamageGainedDuration;
    private int extraHealPercent, extraHealDuration;
    private int fireAmount, fireDuration;
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
        fireAmount = 0;
        fireDuration = 0;
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
    public int getSlow() {
        return slow;
    }

    //setters
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
            case ID_DEMON:
                return new Demon(level);
            case ID_ELF:
                return new Elf(level);
            case ID_BARBARIAN:
            default:
                return new Barbarian(level);
        }
    }
    public void setExtraDamageDealt(int percentage, int duration) {
        this.extraDamageDealtPercent = percentage;
        this.extraDamageDealtDuration = duration;
    }
    public void cleanseExtraDamageDealt() {
        this.extraDamageDealtPercent = 0;
        this.extraDamageDealtDuration = 0;
    }
    public void setExtraDamageGained(int percentage, int duration) {
        this.extraDamageGainedPercent = percentage;
        this.extraDamageGainedDuration = duration;
    }
    public void cleanseExtraDamageGained() {
        this.extraDamageGainedPercent = 0;
        this.extraDamageGainedDuration = 0;
    }
    public void setExtraHeal(int percentage, int duration) {
        this.extraHealPercent = percentage;
        this.extraHealDuration = duration;
    }
    public void cleanseExtraHeal() {
        this.extraHealPercent = 0;
        this.extraHealDuration = 0;
    }
    public void addFire(int amount, int duration) {
        if(this.fireAmount < amount){
            this.fireAmount = amount;
        }
        this.fireDuration += duration;
    }
    public void cleanseFire() {
        fireAmount = 0;
        fireDuration = 0;
    }
    public void addSlow(int slow) {
        this.slow += slow;
        if(slow < 0){
            slow = 0;
        }
    }
    public void cleanseSlow() {
        slow = 0;

    }
    public int takeDamage(int dmg){
        decreaseCdsByTakingDmg();

        int dmgAmount = (currHealth - applyDmgGainMods(dmg)  >= 0) ?  applyDmgGainMods(dmg) : currHealth;
        currHealth -= dmgAmount;
        //debug("" + dmgAmount);
        return dmgAmount;
    }//scale with effects, triggers recharge by taking damage
    public int takeFixedDamage(int dmg){
        decreaseCdsByTakingDmg();

        int dmgAmount = (currHealth - dmg >= 0) ?  dmg : currHealth;
        currHealth -= dmgAmount;
        //debug("" + fixedDmgAmount);
        return dmgAmount;
    }//don't scale with effects, triggers recharge by taking damage
    public int takeSpecialDamage(int dmg){

        int dmgAmount = (currHealth - dmg >= 0) ?  dmg : currHealth;
        currHealth -= dmgAmount;
        //debug("" + dmgAmount);
        return dmgAmount;
    }//don't scale with effects,don't  triggers recharge by taking damage
    public int heal(int heal){
        int healAmount = ((currHealth + applyHealMods(heal)) <= maxHealth ? applyHealMods(heal) : maxHealth - currHealth);
        currHealth += healAmount;
        return healAmount;
    }
    public int healPercent(int percentHeal){
        int healAmount =(currHealth + maxHealth*applyHealMods(percentHeal)/100 <= maxHealth) ? maxHealth*applyHealMods(percentHeal)/100 : maxHealth - currHealth;
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
        if(fireDuration > 0){
            takeSpecialDamage(fireAmount);

            fireDuration--;
            if(fireDuration == 0){
                fireAmount = 0;
            }
        }

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
        if(fireDuration > 0){
            s = s + "\n#" + fireAmount + " fire for " + fireDuration;
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


    //function-methods
    public static int getImageId(int heroId){
        return IMAGES_IDS[heroId];
    }
    private void decreaseCdsByTakingDmg(){
        for(int i = 0; i < Heroes.ABILITIES_X_HERO; i++){
            if(abilities[i].currCd > 0 && abilities[i].chargeWithHitsTaken){
                abilities[i].currCd = abilities[i].currCd - 1;
            }
        }
    }
    private int applyDmgGainMods(int dmg){
        return dmg*(100 + extraDamageGainedPercent)/100;

    }
    private int applyHealMods(int heal){
        return heal*(100 + extraHealPercent)/100;

    }



    //sub classes
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
    public static class Demon extends Heroes{
        public Demon(int level) {
            super(ID_DEMON, level, 930);
            abilities[0] = new Abilities.Demon_attack(this);
            abilities[1] = new Abilities.Demon_curse(this);
        }
    }
    public static class Elf extends Heroes{
        public Elf(int level) {
            super(ID_ELF, level, 640);
            abilities[0] = new Abilities.NothingAbility(this);
            abilities[1] = new Abilities.NothingAbility(this);
        }
    }

}
