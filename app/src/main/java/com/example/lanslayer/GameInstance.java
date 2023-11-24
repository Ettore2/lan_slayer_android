package com.example.lanslayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

//TUTTO CIO CHE RIGUIRDA LE SOCKET DEVE ESSERE FATTO IN DEI THREADS
//LIMITO IL NUMERO DI READ CHE FACCIO ONGI ESECUZIONE
public class GameInstance{

    public static final int PORT = 5777;
    public static final boolean USE_INTERNET = true;//the classes for socket creation do not work on emulator
    public static final int STATE_PRE_GAME = -1, STATE_PLAYING = 0, STATE_I_WON = 1, STATE_I_LOST = 2, STATE_DAW = 3;
    public static int SERVER_TURN = 0, CLIENT_TURN = 1;
    public static final int HEROES_X_TEAM = 2;
    public static final String GAME_VERSION = "7.0.1";//(big update).(balance changes / bug fix).(purely graphic update / code cleaning)
    private static GameInstance instance = null;


    public int turnsClock;
    public int gameState;
    public Turn lastActedTurn;
    public ServerSocket imServerSocket;
    public Socket enemySocket;
    public BufferedReader input;
    public PrintWriter output;
    public final Heroes[] playerHeroes, enemyHeroes, heroes;

    private GameInstance(){
        instance = this;
        gameState = STATE_PRE_GAME;

        playerHeroes = new Heroes[HEROES_X_TEAM];
        enemyHeroes = new Heroes[HEROES_X_TEAM];
        heroes = new Heroes[2 * HEROES_X_TEAM];

        for(int i = 0; i < HEROES_X_TEAM; i++){
            playerHeroes[i] = new Heroes.Barbarian(1);
            enemyHeroes[i] = new Heroes.Barbarian(1);
        }

    }

    public static GameInstance getInstance() {
        return instance == null ? new GameInstance() : instance;
    }

    public Socket createEnemySocket(String ip){
        try {
            enemySocket = new Socket(ip, PORT);
            input = new BufferedReader(new InputStreamReader(enemySocket.getInputStream()));
            output = new PrintWriter(enemySocket.getOutputStream(),true);
            //debug("bene");

        } catch (IOException e) {
            enemySocket = null;
            //debug("male");
        }

        return enemySocket;
    }
    public boolean imServer(){

        return imServerSocket != null;
    }
    public int getRelativeHeroesIndex(int battleId){
        if(imServer()){
            if(isAPlayerHero(battleId)){
                return battleId;
            }else{
                return battleId - HEROES_X_TEAM;
            }
        }else{
            if(isAPlayerHero(battleId)){
                return battleId - HEROES_X_TEAM;
            }else{
                return battleId;
            }
        }
    }
    public boolean isAPlayerHero(int battleId){
        for(int i = 0; i < playerHeroes.length; i++){
            if(playerHeroes[i].getBattleId() == battleId){
                return true;
            }
        }
        return false;
    }
    public Heroes getHero(int battleId){
        return heroes[battleId];

    }
    public void closeChannels(){
        try {
            if(enemySocket != null){
                output.flush();
                enemySocket.close();
                enemySocket = null;
                input = null;
                output = null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void closeServerSocket(){
        try {
            if(imServerSocket != null){
                imServerSocket.close();
                imServerSocket = null;
            }
        } catch (IOException ignored) {
        }
    }
    public void setBattleIds(){
        int i = 0;
        if(imServer()){
            for( int j = 0; j < HEROES_X_TEAM; j++, i++){
                heroes[i] = playerHeroes[j];
                heroes[i].setBattleId(i);
            }
            for( int j = 0; j < HEROES_X_TEAM; j++, i++){
                heroes[i] = enemyHeroes[j];
                heroes[i].setBattleId(i);
            }
        }else{
            for( int j = 0; j < HEROES_X_TEAM; j++, i++){
                heroes[i] = enemyHeroes[j];
                heroes[i].setBattleId(i);
            }
            for( int j = 0; j < HEROES_X_TEAM; j++, i++){
                heroes[i] = playerHeroes[j];
                heroes[i].setBattleId(i);
            }
        }
    }
    public int getGameState(){
        return gameState;

    }
    public void setGameState(int gameState) {
        this.gameState = gameState;

    }
    public boolean playing(){
        return gameState == STATE_PLAYING;

    }
    public boolean isMyTurn(){
        if(imServerSocket != null){
            return turnsClock == SERVER_TURN;
        }else{
            return turnsClock == CLIENT_TURN;
        }
    }
    private int increaseTurnClock(){
        turnsClock = (turnsClock + 1) % 2;
        return turnsClock;
    }
    public void actTurn(Turn turn){

        if(!turn.isEmpty()){
            heroes[turn.activeHeroBattleId].useAbility(turn.abilityId, heroes[turn.passiveHeroBattleId]);
        }


        //calculate new game state
        boolean playerLost = true, enemyLost = true;
        for(int i = 0; i < HEROES_X_TEAM && (playerLost || enemyLost); i++){
            if(playerHeroes[i].isAlive()){
                playerLost = false;
            }
            if(enemyHeroes[i].isAlive()){
                enemyLost = false;
            }
        }


        if(playerLost && enemyLost){
            gameState = STATE_DAW;
        }
        if(playerLost && !enemyLost){
            gameState = STATE_I_LOST;
        }
        if(!playerLost && enemyLost){
            gameState = STATE_I_WON;
        }

        //suspect
        //if(playerLost && enemyLost){
        //    gameState = STATE_PLAYING;
        //}


        for(int i = 0; i < HEROES_X_TEAM; i++){
            if(isMyTurn()){
                playerHeroes[i].decreaseStatusesByTurns();
                playerHeroes[i].decreaseCdsByTurns();
            }else{
                enemyHeroes[i].decreaseStatusesByTurns();
                enemyHeroes[i].decreaseCdsByTurns();
            }
        }//decrease statuses and cds

        increaseTurnClock();
        lastActedTurn = turn;

    }
    public String getLastActedTurnDescription(){
        if(lastActedTurn == null){
            return "battle start";
        }

        String s;
        if(isMyTurn()){
            s = "enemy: ";
        }else{
            s = "you: ";
        }

        if(lastActedTurn.isEmpty()){
            return s = s + "pass";
        }else{
            return s + heroes[lastActedTurn.activeHeroBattleId].getCompleteName() + " use " +
                    heroes[lastActedTurn.activeHeroBattleId].abilities[lastActedTurn.abilityId].displayName + " on " +
                    heroes[lastActedTurn.passiveHeroBattleId].getCompleteName();
        }
    }

}
