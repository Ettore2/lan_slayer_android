package com.example.lanslayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.Random;

public class MainActivity extends AppCompatActivity{
    public class HostThread extends ActivityThread{
        public boolean runThread;
        public HostThread(MainActivity activity) {
            super(activity);
        }

        @Override
        public void run() {
            runThread = true;
            //initialize communications and check version
            try {
                game.imServerSocket = new ServerSocket(GameInstance.PORT);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            do {
                    try {
                        debug("doing server.accept");
                        game.enemySocket = game.imServerSocket.accept();
                        game.input = new BufferedReader(new InputStreamReader(game.enemySocket.getInputStream()));
                        game.output = new PrintWriter(game.enemySocket.getOutputStream(),true);
                        debug("accept");
                        //che communication channels are created in the battle activity

                        //read opponent game version
                        String enemyVersion = game.input.readLine();

                        if(enemyVersion != null){
                            //debug("client version: " + enemyVersion);
                            if(enemyVersion.equals(GameInstance.GAME_VERSION)){//same versions
                                runThread = false;
                                //decide who is starting
                                game.turnsClock = new Random().nextInt(2);
                                game.output.println(game.turnsClock);
                                //debug("host va ad activity battaglia");

                                //go to battle activity
                                doByMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        changeToBattleActivity();
                                    }
                                });

                            }else{//different versions
                                game.output.println(-1);
                                game.output.println(GameInstance.GAME_VERSION);
                                game.closeChannels();
                                //debug("stop comunications");
                                this.showAToast("connection denied from oppenent of version " + enemyVersion);
                            }
                        }

                    } catch (Exception ignored){
                    }
                }while(runThread);



        }
    }
    protected class JoinThread extends ActivityThread{

        public JoinThread(MainActivity activity) {
            super(activity);
        }

        @Override
        public void run() {
            try {
                if(game.createEnemySocket(ipToString(otherIp)) != null){
                    //debug("sucsided");

                    //initialize connection
                    game.output.println(GameInstance.GAME_VERSION);
                    int serverMessage = Integer.parseInt(game.input.readLine());
                    if(serverMessage == -1){//different versions
                        //debug("versione diversa");
                        this.showAToast("impossible connection to enemy of version " + game.input.readLine());
                        game.closeChannels();
                        //debug("yay");


                    }else{//turns clock
                        //debug("stessa versione");
                        game.turnsClock = serverMessage;
                        //debug("client va ad activity battaglia");
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                changeToBattleActivity();
                            }
                        });

                    }

                }else {
                    //debug("failed");
                    showAToast("unable to contact this room");
                }
            } catch (IOException ignored) {

            }

        }
    }

    public static final int STATE_SELECT_HEROES = 0, STATE_HOST = 1, STATE_JOIN = 2, STATE_DICTIONARY = 3;
    public static final String DEBUG_TAG = "debug";



    public HostThread hostThread;
    public JoinThread jointhread;
    public int[] myIp;
    public int[] otherIp;
    public String code;

    public int stateId;
    public EditText textRoomCode;
    public GameInstance game;
    private ImageView sxHeroIng, dxHeroIng;
    private TextView sxHeroName, dxHeroName;
    public Button btnHost, btnJoin;
    private Button btnNextSx, btnPrevSx, btnNextDx, btnPrevDx;
    public Button btnDictionary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //debug("messaggio di debug avvio activity main");

        stateId = STATE_SELECT_HEROES;
        myIp = getIp();
        otherIp = new int[4];

        game = GameInstance.getInstance();
        for(int i = 0; i < GameInstance.HEROES_X_TEAM; i++){
            game.playerHeroes[i] = Heroes.getHero(game.playerHeroes[i].getId(), game.playerHeroes[i].getLevel());
        }


        ((TextView)findViewById(R.id.text_game_version)).setText("V: " + GameInstance.GAME_VERSION);

        textRoomCode = findViewById(R.id.text_room_code);
        textRoomCode.setText("");

        sxHeroIng = findViewById(R.id.img_hero_0);
        dxHeroIng = findViewById(R.id.img_hero_1);

        sxHeroName = findViewById(R.id.name_hero_0);
        dxHeroName = findViewById(R.id.name_Hero_1);

        btnHost = findViewById(R.id.btn_host);
        btnJoin = findViewById(R.id.btn_join);
        btnNextSx = findViewById(R.id.btn_next_sx);
        btnPrevSx = findViewById(R.id.btn_prev_sx);
        btnNextDx = findViewById(R.id.btn_next_dx);
        btnPrevDx = findViewById(R.id.btn_prev_dx);

        btnDictionary = findViewById(R.id.btn_dictionary);

        updateSxHero();
        updateDxHero();


    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(hostThread != null && hostThread.isAlive()){
            hostThread.interrupt();
        }
        if(jointhread != null && jointhread.isAlive()){
            jointhread.interrupt();
        }
    }

    public void showAToast(String message){
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }

    protected void showCodeErrorToast(){
        showAToast("enter a valid code first");

    }
    public static void debug(String message){
        Log.d(DEBUG_TAG, message);
    }
    private void setHeroesSelectionButtonsEnabled(boolean enable){
        btnNextSx.setEnabled(enable);
        btnPrevSx.setEnabled(enable);
        btnNextDx.setEnabled(enable);
        btnPrevDx.setEnabled(enable);

        if(enable){
            btnNextSx.setBackgroundColor(getResources().getColor(R.color.selectable_button,null));
            btnPrevSx.setBackgroundColor(getResources().getColor(R.color.selectable_button,null));
            btnNextDx.setBackgroundColor(getResources().getColor(R.color.selectable_button,null));
            btnPrevDx.setBackgroundColor(getResources().getColor(R.color.selectable_button,null));
        }else{
            btnNextSx.setBackgroundColor(getResources().getColor(R.color.non_selectable_button,null));
            btnPrevSx.setBackgroundColor(getResources().getColor(R.color.non_selectable_button,null));
            btnNextDx.setBackgroundColor(getResources().getColor(R.color.non_selectable_button,null));
            btnPrevDx.setBackgroundColor(getResources().getColor(R.color.non_selectable_button,null));
        }


    }
    private void updateSxHero(){
        sxHeroIng.setImageResource(game.playerHeroes[0].getImageId());
        sxHeroName.setText(game.playerHeroes[0].getName());
    }
    private void updateDxHero(){
        dxHeroIng.setImageResource(game.playerHeroes[1].getImageId());
        dxHeroName.setText(game.playerHeroes[1].getName());
    }
    private static String composeCode(int[] id){
        String code = "" + id[3];
        while(code.length() < 3){
            code = "0" + code;
        }
        code = id[2] + "." + code;
        while(code.length() < 7){
            code = "0" + code;
        }
        code = id[1] + "." + code;
        while(code.length() < 11){
            code = "0" + code;
        }
        code = id[0] + "." + code;
        while(code.length() < 15){
            code = "0" + code;
        }

        return code;
    }
    private static int[] decomposeCode(String code){
        int[] ip = new int[4];

        ip[0] = Integer.parseInt(code.substring(0,3));
        ip[1] = Integer.parseInt(code.substring(4,7));
        ip[2] = Integer.parseInt(code.substring(8,11));
        ip[3] = Integer.parseInt(code.substring(12));

        return ip;
    }
    private int[] getIp(){
            int[] ip = new int[4];
            String myIpStr;

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            myIpStr = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
            //debug(myIpStr);


            //get the ip numbers
            int offset = -1;
            for(int i = 0; i < 3; i++){
                ip[i] = Integer.parseInt(myIpStr.substring(offset+1,myIpStr.indexOf('.',offset+1)));
                offset = myIpStr.indexOf('.',offset+1);
            }
            ip[3] = Integer.parseInt(myIpStr.substring(offset+1));

            return ip;
    }
    private static String ipToString(int[] ip){
        return ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3];
    }
    private void changeToBattleActivity(){
        //change the activity
        startActivity(new Intent(this, BattleActivity.class));
        finish();
    }



    public void host(View view){
        if(stateId == STATE_SELECT_HEROES){//switch to hosting
            code = composeCode(myIp);

            stateId = STATE_HOST;
            textRoomCode.setText(code);
            textRoomCode.setEnabled(false);

            hostThread = new HostThread(this);
            hostThread.start();
        }else{//switch to characters selection
            stateId = STATE_SELECT_HEROES;
            code = "";
            hostThread.runThread = false;
            hostThread = null;
            game.closeServerSocket();
            textRoomCode.setText("");
            textRoomCode.setEnabled(true);
        }


        setHeroesSelectionButtonsEnabled(stateId == STATE_SELECT_HEROES);
        btnJoin.setEnabled(stateId == STATE_SELECT_HEROES);
        btnDictionary.setEnabled(stateId == STATE_SELECT_HEROES);
        if(stateId == STATE_SELECT_HEROES){
            btnJoin.setBackgroundColor(getResources().getColor(R.color.selectable_button,null));
            btnDictionary.setBackgroundColor(getResources().getColor(R.color.selectable_button,null));
        }else{
            btnJoin.setBackgroundColor(getResources().getColor(R.color.non_selectable_button,null));
            btnDictionary.setBackgroundColor(getResources().getColor(R.color.non_selectable_button,null));
        }

        btnHost.setText(stateId == STATE_SELECT_HEROES ? "host" : "back");

    }
    public void join(View view){

        //Log.d(DEBUG_TAG, String.valueOf(textRoomCode.getText()));
        if(textRoomCode.getText().length() != 15  || textRoomCode.getText().charAt(3) != '.'  || textRoomCode.getText().charAt(7) != '.'  || textRoomCode.getText().charAt(11) != '.'){
            showCodeErrorToast();
        }else{
            game.imServerSocket = null;
            otherIp = decomposeCode(String.valueOf(textRoomCode.getText()));
            jointhread =new JoinThread(this);
            jointhread.start();
        }

    }
    public void dictionary(View view){
        /*
        if(stateId == STATE_SELECT_HEROES){
            stateId = STATE_DICTIONARY;
        }else{
            stateId = STATE_SELECT_HEROES;

        }
        setHeroesSelectionButtonsEnabled(stateId == STATE_SELECT_HEROES);
        btnJoin.setEnabled(stateId == STATE_SELECT_HEROES);
        btnHost.setEnabled(stateId == STATE_SELECT_HEROES);
        if(stateId == STATE_SELECT_HEROES){
            btnJoin.setBackgroundColor(getResources().getColor(R.color.selectable_button,null));
            btnHost.setBackgroundColor(getResources().getColor(R.color.selectable_button,null));
        }else{
            btnJoin.setBackgroundColor(getResources().getColor(R.color.non_selectable_button,null));
            btnHost.setBackgroundColor(getResources().getColor(R.color.non_selectable_button,null));
        }

        btnDictionary.setText(stateId == STATE_SELECT_HEROES ? "dictionary" : "back");*/

        startActivity(new Intent(this, DictionaryActivity.class));
    }

    // TODO: create a single vector of heroes to consume less ram
    public void nextSxHero(View view){
        game.playerHeroes[0] = Heroes.getHero((game.playerHeroes[0].getId() + 1)%Heroes.NUMBER_OF_HEROES, 1);
        updateSxHero();
    }
    public void nextDxHero(View view){
        game.playerHeroes[1] = Heroes.getHero((game.playerHeroes[1].getId() + 1)%Heroes.NUMBER_OF_HEROES,1 );
        updateDxHero();
    }
    public void prevSxHero(View view){
        game.playerHeroes[0] = Heroes.getHero((game.playerHeroes[0].getId() - 1 + Heroes.NUMBER_OF_HEROES)%Heroes.NUMBER_OF_HEROES, 1);
        updateSxHero();
    }
    public void prevDxHero(View view){
        game.playerHeroes[1] = Heroes.getHero((game.playerHeroes[1].getId() - 1 + Heroes.NUMBER_OF_HEROES)%Heroes.NUMBER_OF_HEROES, 1);
        updateDxHero();

    }


}