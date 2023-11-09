package com.example.lanslayer;

import static com.example.lanslayer.MainActivity.debug;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class BattleActivity extends AppCompatActivity {

    public class TurnsReader extends ActivityThread{
        public TurnsReader(BattleActivity activity) {
            super(activity);
        }

        @Override
        public void run() {
            Turn turn;
            //debug("start");
            while (game.getGameState() == GameInstance.STATE_PRE_GAME);
            //debug("start2");

            while (game.playing()){
                try {
                    //read the turn (is blocked until a turn is sent)
                    //debug("waiting for turn");
                    String s = game.input.readLine();
                    //debug("read the turn: " + s);
                    turn = new Turn(s);
                    //debug("gained turn: "+turn.toString());
                    //execute the turn
                    game.actTurn(turn);
                    //debug(" "+game.gameState);

                    //update things
                    tUpdateGraphic();

                } catch (Exception e) {
                    if(game.gameState != GameInstance.STATE_I_LOST){
                        game.gameState = GameInstance.STATE_I_WON;
                    }
                }
                while (game.playing() && game.isMyTurn());
            }
            //do battleEnd()
            doByMainThread(new Runnable() {
                @Override
                public void run() {
                    if(game.gameState == GameInstance.STATE_I_WON){
                        BattleActivity.this.showAToast("the enemy surrender");
                    }
                    battleEnd();
                }

            });
            //debug("end");
        }
    }

    public Thread battleInitializerThread, turnsReaderThread;
    public GameInstance game;
    public ImageButton[] btnsPlayerHeroes, btnsEnemyHeroes;
    public Button[] btnsAbilities;
    public Button btnQuit, btnSelect, btnPass;
    public TextView textBattleLog, textDescriptions, textTurnsIndicator;
    public ImageView arrow;
    public ProgressBar[] pBarsPlayer, pBarsEnemy;

    public boolean selectingTarget;
    public int idBattleActiveHero, idChosenAbility, idBattlePassiveHero;
    public ImageButton lastPressedHeroButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle);

        //debug("messaggio debug avvio activity battaglia");

        game = GameInstance.getInstance();
        game.gameState = GameInstance.STATE_PRE_GAME;
        selectingTarget = false;

        btnsPlayerHeroes = new ImageButton[GameInstance.HEROES_X_TEAM];
        btnsEnemyHeroes = new ImageButton[GameInstance.HEROES_X_TEAM];
        pBarsPlayer = new ProgressBar[Heroes.ABILITIES_X_HERO];
        pBarsEnemy = new ProgressBar[Heroes.ABILITIES_X_HERO];
        btnsAbilities = new Button[Heroes.ABILITIES_X_HERO];

        arrow = (ImageView) findViewById(R.id.img_arrow);

        btnsPlayerHeroes[0] = (ImageButton) findViewById(R.id.btn_player_hero_0);
        btnsPlayerHeroes[1] = (ImageButton) findViewById(R.id.btn_player_hero_1);
        btnsEnemyHeroes[0] = (ImageButton) findViewById(R.id.btn_enemy_hero_0);
        btnsEnemyHeroes[1] = (ImageButton) findViewById(R.id.btn_enemy_hero_1);
        pBarsPlayer[0] = (ProgressBar) findViewById(R.id.pBar_player_hero_0);
        pBarsPlayer[1] = (ProgressBar) findViewById(R.id.pBar_player_hero_1);
        pBarsEnemy[0] = (ProgressBar) findViewById(R.id.pBar_enemy_hero_0);
        pBarsEnemy[1] = (ProgressBar) findViewById(R.id.pBar_enemy_hero_1);
        btnsAbilities[0] = (Button) findViewById(R.id.btn_ability1);
        btnsAbilities[1] = (Button) findViewById(R.id.btn_ability2);

        btnQuit = (Button) findViewById(R.id.btn_quit);
        btnSelect = (Button) findViewById(R.id.btn_select);
        btnPass = (Button) findViewById(R.id.btn_pass);
        textBattleLog = (TextView) findViewById(R.id.text_battle_log);
        textDescriptions = (TextView) findViewById(R.id.text_descriptions);
        textTurnsIndicator = (TextView) findViewById(R.id.text_turns_indicator);

        selectingTarget = false;
        textBattleLog.setText("battle start");
        lastPressedHeroButton = btnsPlayerHeroes[0];
        idChosenAbility = 0;

        //send and get team info
        battleInitializerThread = new ActivityThread(this){
            @Override
            public void run() {
                String s = "";
                //tell heroes
                for(int i = 0; i < GameInstance.HEROES_X_TEAM; i++){
                    s = s + game.playerHeroes[i].getId() + "_" + game.playerHeroes[i].getLevel() + "_";

                }
                game.output.println(s.substring(0,s.length()-1));

                //gain heroes
                try {
                    s = game.input.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String[] sSplitted = s.split("_");
                for(int i = 0; i < GameInstance.HEROES_X_TEAM; i++){
                    game.enemyHeroes[i] = Heroes.getHero(Integer.parseInt(sSplitted[i * 2]), Integer.parseInt(sSplitted[i * 2 + 1]));
                }

                game.setBattleIds();
                idBattleActiveHero = game.playerHeroes[0].getBattleId();
                idBattlePassiveHero = game.playerHeroes[0].getBattleId();

                tUpdateGraphic();
                game.setGameState(GameInstance.STATE_PLAYING);

            }//run end
        };
        battleInitializerThread.start();

        //start turn reader
        turnsReaderThread = new TurnsReader(this);
        turnsReaderThread.start();


        //updateGraphic(); //do not do
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(battleInitializerThread != null && battleInitializerThread.isAlive()){
            battleInitializerThread.interrupt();
        }
        if(turnsReaderThread != null && turnsReaderThread.isAlive()){
            turnsReaderThread.interrupt();
        }
    }

    protected void showHeroInfo(){
        if(!selectingTarget){
            textDescriptions.setText(game.heroes[idBattleActiveHero].getDescription());
        }else{
            textDescriptions.setText(game.heroes[idBattlePassiveHero].getDescription());
        }

    }
    protected void showAbilityInfo(){
        textDescriptions.setText(game.heroes[idBattleActiveHero].abilities[idChosenAbility].getDescription());
    }
    protected void updateGraphic(){
        updateGraphic(false);

    }
    protected void updateGraphic(boolean showAbilityInfo){
        //debug("start updating");
        //update images of heroes an life bars
        for(int i = 0; i < GameInstance.HEROES_X_TEAM; i++){
            btnsPlayerHeroes[i].setImageResource(game.playerHeroes[i].getImageId());
            btnsEnemyHeroes[i].setImageResource(game.enemyHeroes[i].getImageId());

            pBarsPlayer[i].setMax(game.playerHeroes[i].getMaxHealth());
            pBarsPlayer[i].setProgress(game.playerHeroes[i].getCurrHealth());
            pBarsEnemy[i].setMax(game.enemyHeroes[i].getMaxHealth());
            pBarsEnemy[i].setProgress(game.enemyHeroes[i].getCurrHealth());
        }
        //debug("1");

        //update position of the arrow
        arrow.setX(lastPressedHeroButton.getX());
        arrow.setY(lastPressedHeroButton.getY() - 20 );
        //debug("2");

        //update abilities
        for (int i = 0; i < btnsAbilities.length; i++){
            //color
            if(game.isMyTurn() && game.playing()){
                if(game.isAPlayerHero(idBattleActiveHero)){
                    if(game.heroes[idBattleActiveHero].isAlive() && game.heroes[idBattleActiveHero].abilities[i].currCd == 0){
                        if(i == idChosenAbility){
                            btnsAbilities[i].setBackgroundColor(getResources().getColor(R.color.choosen_button_ability,null));
                        }else{
                            btnsAbilities[i].setBackgroundColor(getResources().getColor(R.color.selectable_button_ability,null));
                        }

                    }else{
                        if(i == idChosenAbility){
                            btnsAbilities[i].setBackgroundColor(getResources().getColor(R.color.non_selectable_button_dark,null));
                        }else{
                            btnsAbilities[i].setBackgroundColor(getResources().getColor(R.color.non_selectable_button,null));
                        }
                    }
                }else{
                    if(i == idChosenAbility){
                        btnsAbilities[i].setBackgroundColor(getResources().getColor(R.color.non_selectable_button_dark,null));
                    }else{
                        btnsAbilities[i].setBackgroundColor(getResources().getColor(R.color.non_selectable_button,null));
                    }
                }
            }else{
                if(i == idChosenAbility){
                    btnsAbilities[i].setBackgroundColor(getResources().getColor(R.color.non_selectable_button_dark,null));
                }else{
                    btnsAbilities[i].setBackgroundColor(getResources().getColor(R.color.non_selectable_button,null));
                }
            }

            //text

            btnsAbilities[i].setText(game.getHero(idBattleActiveHero).abilities[i].displayName);

        }
        //debug("3");

        //update info text
        if(showAbilityInfo){
            showAbilityInfo();
        }else{
            showHeroInfo();
        }
        //debug("4");

        //update pass and confirm buttons texts
        if(selectingTarget){
            btnSelect.setText("send");
            btnPass.setText("back");
        }else{
            btnSelect.setText("select");
            btnPass.setText("pass");
        }
        //debug("5");

        //update pass and confirm buttons color
        if(game.isMyTurn() && game.playing()){
            if(selectingTarget){
                boolean allowed = false;
                if(game.getHero(idBattleActiveHero).abilities[idChosenAbility].target == Abilities.TARGET_ENEMY && !game.isAPlayerHero(idBattlePassiveHero)){
                    allowed = true;
                }
                if(game.getHero(idBattleActiveHero).abilities[idChosenAbility].target == Abilities.TARGET_ALLAY && game.isAPlayerHero(idBattlePassiveHero)){
                    allowed = true;
                }
                if(game.getHero(idBattleActiveHero).abilities[idChosenAbility].target == Abilities.TARGET_BOTH){
                    allowed = true;
                }
                if(game.getHero(idBattleActiveHero).abilities[idChosenAbility].target == Abilities.TARGET_SELF && idBattleActiveHero == idBattlePassiveHero){
                    allowed = true;
                }

                if(allowed){
                    btnSelect.setBackgroundColor(getResources().getColor(R.color.selectable_button,null));
                }else {
                    btnSelect.setBackgroundColor(getResources().getColor(R.color.non_selectable_button,null));
                }
            }else{
                if(game.isAPlayerHero(idBattleActiveHero) && game.getHero(idBattleActiveHero).isAlive() && game.getHero(idBattleActiveHero).abilities[idChosenAbility].isAvailable()){
                    btnSelect.setBackgroundColor(getResources().getColor(R.color.selectable_button,null));
                }else{
                    btnSelect.setBackgroundColor(getResources().getColor(R.color.non_selectable_button,null));
                }
            }
            btnPass.setBackgroundColor(getResources().getColor(R.color.selectable_button,null));
        }else{
            btnSelect.setBackgroundColor(getResources().getColor(R.color.non_selectable_button,null));
            btnPass.setBackgroundColor(getResources().getColor(R.color.non_selectable_button,null));
        }
        //debug("6");

        //update battle log text
        textBattleLog.setText(game.getLastActedTurnDescription());

        //update turns indicator
        if(game.isMyTurn()){
            textTurnsIndicator.setText("your\nturn");
        }else{
            textTurnsIndicator.setText("Enemy's\nturn");
        }
        //debug("end updating");

    }
    public void tUpdateGraphic(){
        new Handler(Looper.getMainLooper()).post(new ActivityThread(this){
            @Override
            public void run() {
                //debug("start update of thread");
                updateGraphic();
                //debug("end update of thread");
            }
        });
    }
    public void showAToast(String message){
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }

    public void battleEnd(){
        TextView battleResultText =  (TextView) findViewById(R.id.text_battle_result);
        switch (game.gameState){
            case GameInstance.STATE_I_WON:
                battleResultText.setText("YOU WON");
                battleResultText.setVisibility(TextView.VISIBLE);
                break;
            case GameInstance.STATE_I_LOST:
                battleResultText.setText("YOU LOST");
                battleResultText.setVisibility(TextView.VISIBLE);
                break;
            case GameInstance.STATE_DAW:
                battleResultText.setText("DAW");
                battleResultText.setVisibility(TextView.VISIBLE);
                break;
            default:
                break;
        }

    }


    public void quit(View view){
        game.gameState = GameInstance.STATE_I_LOST;

        game.closeChannels();//this free turns reader from waiting to read a turn
        game.closeServerSocket();

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
    public void heroBtns(View view){
        lastPressedHeroButton = (ImageButton) view;
        for(int i = 0; i < btnsPlayerHeroes.length; i++){
            if(view.equals(btnsPlayerHeroes[i])){
                if(!selectingTarget){
                    idBattleActiveHero = game.playerHeroes[i].getBattleId();
                }
                idBattlePassiveHero = game.playerHeroes[i].getBattleId();
            }
            if(view.equals(btnsEnemyHeroes[i])){
                if(!selectingTarget){
                    idBattleActiveHero = game.enemyHeroes[i].getBattleId();
                }
                idBattlePassiveHero = game.enemyHeroes[i].getBattleId();

            }
        }
        //debug(""+idBattleActiveHero);
        //debug(""+idBattlePassiveHero);

        updateGraphic(selectingTarget);
    }
    public void abilityBtns(View view){
        for(int i = 0; i < btnsAbilities.length; i++){
            if(!selectingTarget){
                if(view.equals(btnsAbilities[i])){
                    idChosenAbility = i;
                }
            }
        }
        updateGraphic(true);
    }
    public void select(View view){
        if(game.isMyTurn() && game.playing()){
            if(selectingTarget){
                //check if everything is acceptable
                if(game.isAPlayerHero(idBattleActiveHero) && game.getHero(idBattleActiveHero).isAlive() && game.getHero(idBattleActiveHero).abilities[idChosenAbility].isAvailable()){
                    boolean allowed = false;
                    if(game.getHero(idBattleActiveHero).abilities[idChosenAbility].target == Abilities.TARGET_ENEMY && !game.isAPlayerHero(idBattlePassiveHero)){
                        allowed = true;
                    }
                    if(game.getHero(idBattleActiveHero).abilities[idChosenAbility].target == Abilities.TARGET_ALLAY && game.isAPlayerHero(idBattlePassiveHero)){
                        allowed = true;
                    }
                    if(game.getHero(idBattleActiveHero).abilities[idChosenAbility].target == Abilities.TARGET_BOTH){
                        allowed = true;
                    }
                    if(game.getHero(idBattleActiveHero).abilities[idChosenAbility].target == Abilities.TARGET_SELF && idBattleActiveHero == idBattlePassiveHero){
                        allowed = true;
                    }

                    if(allowed){
                        //send the turn
                        //debug("start to send the turn");
                        game.actTurn(new Turn(idBattleActiveHero, idChosenAbility, idBattlePassiveHero));
                        //debug("1");
                        new ActivityThread(this){
                            @Override
                            public void run() {
                                game.output.println(game.lastActedTurn.toString());
                            }
                        }.start();
                        //debug("2");
                        selectingTarget = false;
                        //debug("finisch to send the turn");
                    }else {
                        showAToast("select an available target");
                    }
                }else{
                    showAToast("select an available hero and or ability");
                }
            }else{
                if(game.getHero(idBattleActiveHero).isAlive()){
                    selectingTarget = true;
                }else{
                    showAToast("select an available hero");
                }
            }
        }

        updateGraphic();
    }

    public void pass(View view){
        if(game.isMyTurn() && game.playing()){
            if(selectingTarget){
                selectingTarget = false;
                idBattleActiveHero = idBattlePassiveHero;
            }else{
                game.actTurn(Turn.empyTurn());
                new ActivityThread(this){
                //send empty turn
                @Override
                public void run() {
                    game.output.println(Turn.empyTurn().toString());
                }
            }.start();

            }
        }

        updateGraphic();
    }


}













