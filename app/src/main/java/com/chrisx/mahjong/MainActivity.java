package com.chrisx.mahjong;

/**
 * Organized in order of priority:
 * @TODO everything
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesCallbackStatusCodes;
import com.google.android.gms.games.RealTimeMultiplayerClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.OnRealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateCallback;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends Activity {
    private Bitmap bmp;
    static Canvas canvas;
    private LinearLayout ll;
    private float scaleFactor;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    private final int RC_SIGN_IN = 1,
            RC_SELECT_PLAYERS = 2,
            RC_WAITING_ROOM = 3,
            RC_INVITATION_INBOX = 4;
    private String TAG = "Multiplayer";

    private GoogleSignInAccount acc;
    private GoogleSignInOptions gsio;
    private GoogleSignInClient gsic;

    private RoomConfig mJoinedRoomConfig;
    private String mMyParticipantId;

    static int N[] = {33, 52, 89};
    static Bitmap[][] sprites;
    static Bitmap[] icons, cards, powercards, trainers, ludicolo;
    static Bitmap pokebo, deck, gplay, loggedin, card_back, back, left, right, quick, create, join;

    static Typeface font;

    private boolean paused = false;
    private long frameCount = 0;

    private String menu = "start";
    private int selected, page;
    private int bots[];

    //frame data
    private static final int FRAMES_PER_SECOND = 60;
    private long nanosecondsPerFrame;

    private int TRANSITION_MAX = FRAMES_PER_SECOND * 2 / 3;
    private int transition = TRANSITION_MAX / 2;
    private int animation;

    private float downX, downY;

    private Paint w50, w75, w125, b25, b50, b75, b100, line2;

    private List<Card> playerDeck;
    private List<Player> players;
    private Card[] middle = new Card[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set up Google Play sign-in
        gsio = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .requestEmail().build();
        gsic = GoogleSignIn.getClient(this, gsio);

        //creates the bitmap
        //note: Star 4.5 is 480x854
        int targetH = 480,
                wpx = getAppUsableScreenSize(this).x,
                hpx = getAppUsableScreenSize(this).y;
        scaleFactor = Math.min(1,(float)targetH/hpx);
        bmp = Bitmap.createBitmap(Math.round(wpx*scaleFactor),Math.round(hpx*scaleFactor),Bitmap.Config.RGB_565);

        //creates canvas
        canvas = new Canvas(bmp);

        ll = (LinearLayout) findViewById(R.id.draw_area);
        ll.setBackgroundDrawable(new BitmapDrawable(bmp));

        //initializes SharedPreferences
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        Resources res = getResources();

        sprites = new Bitmap[3][Math.max(N[0],Math.max(N[1],N[2]))];
        for (int i = 0; i < N[0]; i++) {
            sprites[0][i] = BitmapFactory.decodeResource(res, R.drawable.fire00+i);
        }
        for (int i = 0; i < N[1]; i++) {
            sprites[1][i] = BitmapFactory.decodeResource(res, R.drawable.grass00+i);
        }
        for (int i = 0; i < N[2]; i++) {
            sprites[2][i] = BitmapFactory.decodeResource(res, R.drawable.water00+i);
        }
        icons = new Bitmap[3];
        for (int i = 0; i < icons.length; i++) {
            icons[i] = BitmapFactory.decodeResource(res, R.drawable.icon_fire+i);
        }
        cards = new Bitmap[3];
        for (int i = 0; i < cards.length; i++) {
            cards[i] = BitmapFactory.decodeResource(res, R.drawable.card_fire+i);
        }
        powercards = new Bitmap[3];
        for (int i = 0; i < powercards.length; i++) {
            powercards[i] = BitmapFactory.decodeResource(res, R.drawable.powercard_fire+i);
        }
        trainers = new Bitmap[3];
        for (int i = 0; i < trainers.length; i++) {
            trainers[i] = BitmapFactory.decodeResource(res, R.drawable.trainer_fire+i);
        }
        ludicolo = new Bitmap[6];
        for (int i = 0; i < ludicolo.length; i++) {
            ludicolo[i] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.frame1+i),
                    Math.round(c480(192)),Math.round(c480(192)),false);
        }

        pokebo = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.pokebo),
                Math.round(c480(304)),Math.round(c480(118)),false);
        deck = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.deck),
                Math.round(c480(60)),Math.round(c480(60)),false);
        gplay = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.googleplay),
                Math.round(c480(60)),Math.round(c480(60)),false);
        loggedin = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.loggedin),
                Math.round(c480(60)),Math.round(c480(60)),false);
        card_back = BitmapFactory.decodeResource(res, R.drawable.card_back);
        back = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.back),
                Math.round(c480(60)),Math.round(c480(60)),false);
        left = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.left),
                Math.round(c480(60)),Math.round(c480(60)),false);
        right = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.right),
                Math.round(c480(60)),Math.round(c480(60)),false);
        quick = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.quick),
                Math.round(c480(128)),Math.round(c480(128)),false);
        create = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.create),
                Math.round(c480(128)),Math.round(c480(128)),false);
        join = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.join),
                Math.round(c480(128)),Math.round(c480(128)),false);

        nanosecondsPerFrame = (long)1e9 / FRAMES_PER_SECOND;

        //initialize fonts
        font = Typeface.createFromAsset(getAssets(), "fonts/Exo2-SemiBold.ttf");

        canvas.drawColor(Color.BLACK);

        //pre-defined paints
        w75 = newPaint(Color.WHITE);
        w75.setTextAlign(Paint.Align.CENTER);
        w75.setTextSize(c480(75));

        w50 = new Paint(w75);
        w50.setTextSize(c480(50));

        w125 = new Paint(w75);
        w125.setTextSize(c480(125));

        b50 = newPaint(Color.BLACK);
        b50.setTextAlign(Paint.Align.CENTER);
        b50.setTextSize(c480(50));

        b25 = new Paint(b50);
        b25.setTextSize(c480(25));

        b75 = new Paint(b50);
        b75.setTextSize(c480(75));

        b100 = new Paint(b50);
        b100.setTextSize(c480(100));

        line2 = newPaint(Color.BLACK);
        line2.setStrokeWidth(c480(2));

        playerDeck = starterDeck();
        playerDeck.addAll(starterDeck());


        editor.putInt("character",-1);
        editor.apply();

        final Handler handler = new Handler();

        //Update thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                //draw loop
                while (!menu.equals("quit")) {
                    final long startTime = System.nanoTime();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!paused) {
                                if (menu.equals("1P_game")) {
                                    if (animation == 0 && middle[0] != null && middle[1] != null) {
                                        //find winner
                                        if (middle[0].compare(middle[1]) < 0) {
                                            players.get(1).addPoint(middle[1].getType());
                                        } else if (middle[0].compare(middle[1]) > 0) {
                                            players.get(0).addPoint(middle[0].getType());
                                        }

                                        animation = FRAMES_PER_SECOND;
                                    } else if (animation > 0) {
                                        animation--;
                                        if (animation == 0) {
                                            middle = new Card[2];
                                            frameCount = 0;

                                            //either player or CPU won
                                            if (players.get(0).won() || players.get(1).won()) {
                                                goToMenu("1P_gameover");
                                            }
                                        }
                                    }

                                    //have bot play highest card
                                    if (frameCount > FRAMES_PER_SECOND*2 && middle[1] == null) {
                                        int maxIndex = 0;
                                        for (int i = 1; i < 5; i++) {
                                            if (players.get(1).getCards().get(i).getPower() >
                                                    players.get(1).getCards().get(maxIndex).getPower())
                                                maxIndex = i;
                                        }
                                        middle[1] = players.get(1).play(maxIndex);
                                    }
                                } else if (menu.equals("MP_game")) {
                                    if (animation == 0 && middle[0] != null && middle[1] != null) {
                                        //find winner
                                        if (middle[0].compare(middle[1]) < 0) {
                                            players.get(1).addPoint(middle[1].getType());
                                        } else if (middle[0].compare(middle[1]) > 0) {
                                            players.get(0).addPoint(middle[0].getType());
                                        }

                                        animation = FRAMES_PER_SECOND;
                                    } else if (animation > 0) {
                                        animation--;
                                        if (animation == 0) {
                                            middle = new Card[2];
                                            frameCount = 0;

                                            //either player won
                                            if (players.get(0).won() || players.get(1).won()) {
                                                goToMenu("MP_gameover");
                                            }
                                        }
                                    }
                                }

                                //fading transition effect
                                if (transition > 0) {
                                    transition--;
                                }
                            }
                        }
                    });

                    frameCount++;

                    //wait until frame is done
                    while (System.nanoTime() - startTime < nanosecondsPerFrame);
                }
            }
        }).start();

        //UI thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                //draw loop
                while (!menu.equals("quit")) {
                    final long startTime = System.nanoTime();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!paused) {
                                if (transition < TRANSITION_MAX / 2) {
                                    canvas.drawColor(Color.rgb(100, 200, 100));

                                    if (menu.equals("start")) {
                                        drawTitleMenu();
                                    } else if (menu.equals("deck")) {
                                        drawDeck();
                                    } else if (menu.equals("select")) {
                                        drawTrainerSelect();
                                    } else if (menu.equals("greeting")) {
                                        drawGreeting();
                                    } else if (menu.equals("1P_lobby")) {
                                        //draw1PLobby();
                                    } else if (menu.equals("1P_game")) {
                                        drawGame();
                                    } else if (menu.equals("MP_select")) {
                                        drawMPSelect();
                                    } else if (menu.equals("1P_gameover")) {
                                        drawGameOver();
                                    } else if (menu.equals("MP_waiting")) {
                                        canvas.drawBitmap(ludicolo[(int)frameCount/15%6],
                                                w()/2-ludicolo[0].getWidth()/2,h()/2-ludicolo[0].getHeight()/2,null);
                                    } else if (menu.equals("MP_game")) {
                                        drawGame();
                                    } else if (menu.equals("MP_gameover")) {
                                        drawGameOver();
                                    }
                                }

                                //fading transition effect
                                if (transition > 0) {
                                    int t = TRANSITION_MAX / 2, alpha;
                                    if (transition > t) {
                                        alpha = 255 - 255 * (transition - t) / t;
                                    } else {
                                        alpha = 255 - 255 * (t - transition) / t;
                                    }
                                    canvas.drawColor(Color.argb(alpha, 20, 20, 20));
                                }

                                //update canvas
                                ll.invalidate();
                            }
                        }
                    });

                    //wait until frame is done
                    while (System.nanoTime() - startTime < nanosecondsPerFrame);
                }
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;

        //try to sign in to Google Play
        if (!isSignedIn()) signInSilently();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mRoom != null) leaveRoom();
    }

    @Override
    public void onBackPressed() {
        if (menu.equals("deck") || menu.equals("1P_lobby") || menu.equals("MP_select")) {
            goToMenu("start");
        }
    }

    @Override
    //handles touch events
    public boolean onTouchEvent(MotionEvent event) {
        if (transition > 0) return true;

        float X = event.getX()*scaleFactor;
        float Y = event.getY()*scaleFactor;
        int action = event.getAction();

        if (menu.equals("start")) {
            if (action == MotionEvent.ACTION_DOWN) {
                Rect sp = new Rect(), mp = new Rect();
                w75.getTextBounds("SINGLEPLAYER",0,12,sp);
                w75.getTextBounds("MULTIPLAYER",0,11,mp);

                //view deck
                if (X < c480(100) && Y < c480(100)) {
                    goToMenu("deck");
                }
                //sign in to google play
                else if (X > w()-c480(100) && Y < c480(100)) {
                    if (isSignedIn()) signOut();
                    else startSignInIntent();
                }
                //singleplayer mode
                else if (X > w()/2-sp.width()/2 && X < w()/2+sp.width()/2
                        && Y > h()*4/6-sp.height() && Y < h()*4/6) {
                    goToMenu("1P_game");
                }
                //multiplayer mode
                else if (X > w()/2-mp.width()/2 && X < w()/2+mp.width()/2
                        && Y > h()*5/6-mp.height() && Y < h()*5/6) {
                    if (isSignedIn()) goToMenu("MP_select");
                }
            }
        } else if (menu.equals("deck")) {
            if (action == MotionEvent.ACTION_DOWN) {
                //back to home
                if (X < c480(100) && Y < c480(100)) {
                    goToMenu("start");
                }
                //previous page
                else if (page > 0 && X < c480(100) && Y > c480(190) && Y < c480(290)) {
                    page--;
                }
                //next page
                else if ((page+1)*12 < playerDeck.size() && X > w()-c480(100) && Y > c480(190) && Y < c480(290)) {
                    page++;
                }
            }
        } else if (menu.equals("select")) {
            if (action == MotionEvent.ACTION_DOWN) {
                for (int i = 0; i < 3; i++) {
                    if (X >= w() / 3 * i && X <= w() / 3 * (i + 1)) {
                        if (i != selected) selected = i;
                        else {
                            editor.putInt("character", i);
                            editor.apply();

                            playerDeck.add(getCard(getCharacter(),10));

                            goToMenu("greeting");
                        }

                        break;
                    }
                }
            }
        } else if (menu.equals("greeting")) {
            if (action == MotionEvent.ACTION_DOWN) {
                goToMenu("start");
            }
        } /*else if (menu.equals("1P_lobby")) {
            if (action == MotionEvent.ACTION_DOWN) {
                //Toggle bots
                for (int i = 0; i < 3; i++) {
                    float mid = w()/8 * (i*2 + 3);
                    if (X >= mid-c480(64) && X <= mid+c480(64)
                            && Y >= h()/2-c480(64) && Y <= h()/2+c480(64)) {
                        //Toggle bot
                        if (bots[i] >= 0) bots[i] = -1;
                        else bots[i] = (int)(Math.random()*3);

                        break;
                    }
                }

                //Start game
                Rect r = new Rect();
                w75.getTextBounds("Ready!",0,6,r);
                if (X > w()/2-r.width()/2 && X < w()/2+r.width()/2
                        && Y > c480(450)+r.top && Y < c480(450)+r.bottom) {
                    goToMenu("1P_game");
                }
            }
        }*/else if (menu.equals("1P_game")) {
            if (action == MotionEvent.ACTION_DOWN && middle[0] == null) {
                float margin = MainActivity.c480(80) / 6;
                float w = MainActivity.c480(112), h = MainActivity.c480(80);
                for (int i = 0; i < 5; i++) {
                    if (X > margin && X < w+margin && Y > margin+i*(h+margin) && Y < (i+1)*(h+margin)) {
                        //play card
                        middle[0] = players.get(0).play(i);
                        break;
                    }
                }
            }
        } else if (menu.equals("MP_select")) {
            if (action == MotionEvent.ACTION_DOWN) {
                //back to menu
                if (X < c480(100) && Y < c480(100)) {
                    goToMenu("start");
                } else if (Y > c480(100) && Y < c480(430)) {
                    //quick match
                    if (X < w() / 3) startQuickGame();
                    //create private room
                    else if (X < w() * 2 / 3) invitePlayers();
                    //join private room (view invitations)
                    else showInvitationInbox();

                    goToMenu("MP_waiting");
                }
            }
        } else if (menu.equals("MP_game")) {
            if (action == MotionEvent.ACTION_DOWN && middle[0] == null) {
                float margin = MainActivity.c480(80) / 6;
                float w = MainActivity.c480(112), h = MainActivity.c480(80);
                for (int i = 0; i < 5; i++) {
                    if (X > margin && X < w+margin && Y > margin+i*(h+margin) && Y < (i+1)*(h+margin)) {
                        //play card
                        middle[0] = players.get(0).play(i);
                        //send card type and ID to other player
                        sendToAllReliably(new byte[]{(byte)middle[0].getType(),(byte)middle[0].getID()});

                        break;
                    }
                }
            }
        } else if (menu.equals("1P_gameover")) {
            if (action == MotionEvent.ACTION_DOWN) {
                Rect pa = new Rect(), bm = new Rect();
                w50.getTextBounds("Play Again?",0,11,pa);
                w50.getTextBounds("Back to Menu",0,12,bm);

                if (X > w()/2-pa.width()/2 && X < w()/2+pa.width()/2
                        && Y > c480(330)+pa.top && Y < c480(330)+pa.bottom) {
                    goToMenu("1P_game");
                } else if (X > w()/2-bm.width()/2 && X < w()/2+bm.width()/2
                        && Y > c480(400)+bm.top && Y < c480(400)+bm.bottom) {
                    goToMenu("start");
                }
            }
        } else if (menu.equals("MP_gameover")) {
            if (action == MotionEvent.ACTION_DOWN) {
                Rect pa = new Rect(), bm = new Rect();
                w50.getTextBounds("Play Again?",0,11,pa);
                w50.getTextBounds("Back to Menu",0,12,bm);

                if (X > w()/2-pa.width()/2 && X < w()/2+pa.width()/2
                        && Y > c480(330)+pa.top && Y < c480(330)+pa.bottom) {
                    goToMenu("MP_game");
                } else if (X > w()/2-bm.width()/2 && X < w()/2+bm.width()/2
                        && Y > c480(400)+bm.top && Y < c480(400)+bm.bottom) {
                    leaveRoom();
                }
            }
        }

        return true;
    }

    private GoogleSignInAccount getAcc() {
        return GoogleSignIn.getLastSignedInAccount(this);
    }

    private RealTimeMultiplayerClient getClient() {
        return Games.getRealTimeMultiplayerClient(this, getAcc());
    }

    private boolean isSignedIn() {
        return getAcc() != null;
    }

    private void signInSilently() {
        gsic.silentSignIn().addOnCompleteListener(this,
                new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        if (task.isSuccessful()) {
                            // The signed in account is stored in the task's result.
                            acc = task.getResult();
                        } else {
                            // Player will need to sign-in explicitly using via UI
                        }
                    }
                });
    }

    private void startSignInIntent() {
        Intent intent = gsic.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    private void signOut() {
        gsic.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // at this point, the user is signed out.
                    }
                });
    }

    private void leaveRoom() {
        if (mJoinedRoomConfig != null && mRoom != null) {
            getClient().leave(mJoinedRoomConfig, mRoom.getRoomId());
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        goToMenu("MP_select");
    }

    private RoomUpdateCallback mRoomUpdateCallback = new RoomUpdateCallback() {
        @Override
        public void onRoomCreated(int code, @Nullable Room room) {
            // Update UI and internal state based on room updates.
            if (code == GamesCallbackStatusCodes.OK && room != null) {
                Log.w(TAG, "Room " + room.getRoomId() + " created.");
                mRoom = room;showWaitingRoom(room, 2);
            } else {
                Log.w(TAG, "Error creating room: " + code);
                new AlertDialog.Builder(thisActivity).setMessage("Error creating room")
                        .setNeutralButton(android.R.string.ok, null).show();
                // let screen go to sleep
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }

        @Override
        public void onJoinedRoom(int code, @Nullable Room room) {
            // Update UI and internal state based on room updates.
            if (code == GamesCallbackStatusCodes.OK && room != null) {
                Log.w(TAG, "Room " + room.getRoomId() + " joined.");
                mRoom = room;
                showWaitingRoom(room, 2);
            } else {
                Log.w(TAG, "Error joining room: " + code);
                new AlertDialog.Builder(thisActivity).setMessage("Error joining room")
                        .setNeutralButton(android.R.string.ok, null).show();
                // let screen go to sleep
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }

        @Override
        public void onLeftRoom(int code, @NonNull String roomId) {
            Log.w(TAG, "Left room " + roomId);
        }

        @Override
        public void onRoomConnected(int code, @Nullable Room room) {
            if (code == GamesCallbackStatusCodes.OK && room != null) {
                Log.w(TAG, "Room " + room.getRoomId() + " connected.");
                mRoom = room;
                //showWaitingRoom(room, 2);
            } else {
                Log.w(TAG, "Error connecting to room: " + code);
                new AlertDialog.Builder(thisActivity).setMessage("Error connecting to room")
                        .setNeutralButton(android.R.string.ok, null).show();
                // let screen go to sleep
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    };

    // are we already playing?
    boolean mPlaying = false;

    // at least 2 players required for our game
    final static int MIN_PLAYERS = 2;

    // returns whether there are enough players to start the game
    boolean shouldStartGame(Room room) {
        int connectedPlayers = 0;
        for (Participant p : room.getParticipants()) {
            if (p.isConnectedToRoom()) {
                ++connectedPlayers;
            }
        }
        return connectedPlayers >= MIN_PLAYERS;
    }

    // Returns whether the room is in a state where the game should be canceled.
    boolean shouldCancelGame(Room room) {
        // TODO: Your game-specific cancellation logic here. For example, you might decide to
        // cancel the game if enough people have declined the invitation or left the room.
        // You can check a participant's status with Participant.getStatus().
        // (Also, your UI should have a Cancel button that cancels the game too)
        return false;
    }

    private Activity thisActivity = this;
    private Room mRoom;
    private RoomStatusUpdateCallback mRoomStatusCallbackHandler = new RoomStatusUpdateCallback() {
        @Override
        public void onRoomConnecting(@Nullable Room room) {
            // Update the UI status since we are in the process of connecting to a specific room.
        }

        @Override
        public void onRoomAutoMatching(@Nullable Room room) {
            // Update the UI status since we are in the process of matching other players.
        }

        @Override
        public void onPeerInvitedToRoom(@Nullable Room room, @NonNull List<String> list) {
            // Update the UI status since we are in the process of matching other players.
        }

        @Override
        public void onPeerDeclined(@Nullable Room room, @NonNull List<String> list) {
            // Peer declined invitation, see if game should be canceled
            if (!mPlaying && shouldCancelGame(room)) {
                leaveRoom();
            }
        }

        @Override
        public void onPeerJoined(@Nullable Room room, @NonNull List<String> list) {
            // Update UI status indicating new players have joined!
        }

        @Override
        public void onPeerLeft(@Nullable Room room, @NonNull List<String> list) {
            // Peer left, see if game should be canceled.
            if (!mPlaying && shouldCancelGame(room)) {
                leaveRoom();
            }
        }

        @Override
        public void onConnectedToRoom(@Nullable Room room) {
            // Connected to room, record the room Id.
            mRoom = room;
            Games.getPlayersClient(thisActivity, getAcc())
                    .getCurrentPlayerId().addOnSuccessListener(new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String playerId) {
                    mMyParticipantId = mRoom.getParticipantId(playerId);
                }
            });
            //showWaitingRoom(mRoom, 2);
        }

        @Override
        public void onDisconnectedFromRoom(@Nullable Room room) {
            // This usually happens due to a network error, leave the game.
            leaveRoom();
            // show error message and return to main screen
            new AlertDialog.Builder(thisActivity).setMessage("Disconnected from room")
                    .setNeutralButton(android.R.string.ok, null).show();
            mRoom = null;
            mJoinedRoomConfig = null;
        }

        @Override
        public void onPeersConnected(@Nullable Room room, @NonNull List<String> list) {
            if (mPlaying) {
                // add new player to an ongoing game
            } else if (shouldStartGame(room)) {
                // start game!
            }
        }

        @Override
        public void onPeersDisconnected(@Nullable Room room, @NonNull List<String> list) {
            if (mPlaying) {
                // do game-specific handling of this -- remove player's avatar
                // from the screen, etc. If not enough players are left for
                // the game to go on, end the game and leave the room.
            } else if (shouldCancelGame(room)) {
                // cancel the game
                leaveRoom();
            }
        }

        @Override
        public void onP2PConnected(@NonNull String participantId) {
            // Update status due to new peer to peer connection.
        }

        @Override
        public void onP2PDisconnected(@NonNull String participantId) {
            // Update status due to peer to peer connection being disconnected.
        }
    };

    private void startQuickGame() {
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(1, 1, 0);

        // build the room config:
        RoomConfig roomConfig =
                RoomConfig.builder(mRoomUpdateCallback)
                        .setOnMessageReceivedListener(mMessageReceivedHandler)
                        .setRoomStatusUpdateCallback(mRoomStatusCallbackHandler)
                        .setAutoMatchCriteria(autoMatchCriteria)
                        .build();

        // prevent screen from sleeping during handshake
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Save the roomConfig so we can use it if we call leave().
        mJoinedRoomConfig = roomConfig;

        // create room:
        getClient().create(roomConfig);
    }

    private void invitePlayers() {
        // launch the player selection screen
        // minimum: 1 other player; maximum: 1 other player
        getClient().getSelectOpponentsIntent(1, 1, true)
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_SELECT_PLAYERS);
                    }
                });
    }

    private void showWaitingRoom(Room room, int maxPlayersToStartGame) {
        Log.w(TAG,"Attempting to show waiting room " + room.getRoomId());
        getClient().getWaitingRoomIntent(room, maxPlayersToStartGame)
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_WAITING_ROOM);
                    }
                });
    }

    boolean mWaitingRoomFinishedFromCode = false;

    private void onStartGameMessageReceived() {
        mWaitingRoomFinishedFromCode = true;
        finishActivity(RC_WAITING_ROOM);
    }

    private void showInvitationInbox() {
        Games.getInvitationsClient(this, getAcc())
                .getInvitationInboxIntent()
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_INVITATION_INBOX);
                    }
                });
    }

    void sendToAllReliably(byte[] message) {
        for (String participantId : mRoom.getParticipantIds()) {
            if (!participantId.equals(mMyParticipantId)) {
                Task<Integer> task = getClient().sendReliableMessage(message, mRoom.getRoomId(), participantId,
                        handleMessageSentCallback).addOnCompleteListener(new OnCompleteListener<Integer>() {
                            @Override
                            public void onComplete(@NonNull Task<Integer> task) {
                                // Keep track of which messages are sent, if desired.
                                recordMessageToken(task.getResult());
                            }
                        });
            }
        }
    }

    HashSet<Integer> pendingMessageSet = new HashSet<>();

    synchronized void recordMessageToken(int tokenId) {
        pendingMessageSet.add(tokenId);
    }

    private RealTimeMultiplayerClient.ReliableMessageSentCallback handleMessageSentCallback =
            new RealTimeMultiplayerClient.ReliableMessageSentCallback() {
                @Override
                public void onRealTimeMessageSent(int statusCode, int tokenId, String recipientId) {
                    // handle the message being sent.
                    synchronized (this) {
                        pendingMessageSet.remove(tokenId);
                    }
                }
            };

    private OnRealTimeMessageReceivedListener mMessageReceivedHandler = new OnRealTimeMessageReceivedListener() {
        @Override
        public void onRealTimeMessageReceived(@NonNull RealTimeMessage realTimeMessage) {
            // Handle messages received here.
            byte[] message = realTimeMessage.getMessageData();
            Log.w(TAG, message.toString());
            
            middle[1] = new Card(message[0],message[1]);
            players.get(1).play(0);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // The signed in account is stored in the result.
                acc = result.getSignInAccount();
            } else {
                String message = result.getStatus().getStatusMessage();
                if (message == null || message.isEmpty()) {
                    message = getString(R.string.signin_other_error);
                }
                new AlertDialog.Builder(this).setMessage(message)
                        .setNeutralButton(android.R.string.ok, null).show();
            }
        }
        if (requestCode == RC_SELECT_PLAYERS) {
            if (resultCode != Activity.RESULT_OK) {
                // Canceled or some other error.
                leaveRoom();
                return;
            }

            // Get the invitee list.
            final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
            //Log.w("Invitees",invitees.toString());

            // Get Automatch criteria.
            int minAutoPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
            int maxAutoPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

            // Create the room configuration.
            RoomConfig.Builder roomBuilder = RoomConfig.builder(mRoomUpdateCallback)
                    .setOnMessageReceivedListener(mMessageReceivedHandler)
                    .setRoomStatusUpdateCallback(mRoomStatusCallbackHandler)
                    .addPlayersToInvite(invitees);
            if (minAutoPlayers > 0) {
                roomBuilder.setAutoMatchCriteria(
                        RoomConfig.createAutoMatchCriteria(minAutoPlayers, maxAutoPlayers, 0));
            }

            // Save the roomConfig so we can use it if we call leave().
            mJoinedRoomConfig = roomBuilder.build();
            getClient().create(mJoinedRoomConfig);
        }
        if (requestCode == RC_WAITING_ROOM) {
            // Look for finishing the waiting room from code, for example if a
            // "start game" message is received.  In this case, ignore the result.
            if (mWaitingRoomFinishedFromCode) {
                return;
            }

            if (resultCode == Activity.RESULT_OK) {
                // Start the game!
                goToMenu("MP_game");
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // Back button pressed.
                leaveRoom();
            } else if (resultCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                // player wants to leave the room.
                leaveRoom();
            }
        }
        if (requestCode == RC_INVITATION_INBOX) {
            if (resultCode != Activity.RESULT_OK) {
                // Canceled or some error.
                leaveRoom();
                return;
            }
            Invitation invitation = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);
            if (invitation != null) {
                RoomConfig.Builder builder = RoomConfig.builder(mRoomUpdateCallback)
                        .setOnMessageReceivedListener(mMessageReceivedHandler)
                        .setRoomStatusUpdateCallback(mRoomStatusCallbackHandler)
                        .setInvitationIdToAccept(invitation.getInvitationId());
                mJoinedRoomConfig = builder.build();
                getClient().join(mJoinedRoomConfig);
                // prevent screen from sleeping during handshake
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    private Point getAppUsableScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    //shorthand for w() and h()
    static float w() {
        return canvas.getWidth();
    }
    static float h() {
        return canvas.getHeight();
    }

    //creates an instance of Paint set to a given color
    static Paint newPaint(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTypeface(font);

        return p;
    }

    static int power(int type, int id) {
        int n = N[type];
        for (int j = 1; j < 13; j++)
            if (id*1./n < j*(j+1)/2*1./(12*13/2))
                return 13 - j;
        return 1;
    }

    static float c480(float f) {
        return h() / (480 / f);
    }
    static float c854(float f) {
        return w() / (854 / f);
    }

    private int getCharacter() {
        return sharedPref.getInt("character", -1);
    }

    private double toRad(double deg) {
        return Math.PI/180*deg;
    }

    private void goToMenu(String s) {
        transition = TRANSITION_MAX;

        if (s.equals("1P_game") || s.equals("MP_select")) {
            if (getCharacter() == -1) {
                s = "select";
                selected = -1;
            } else if (s.equals("1P_game")) {
                bots = new int[]{(int)(Math.random()*3)};
                /*
                if (bots == null) {
                    bots = new int[]{(int)(Math.random()*3), -1, -1};
                } else {
                    for (int i = 0; i < bots.length; i++) {
                        if (bots[i] >= 0) bots[i] = (int)(Math.random()*3);
                    }
                }
                */
            }
        } else if (s.equals("deck")) {
            Collections.sort(playerDeck);
            page = 0;
        }

        if (s.equals("1P_game")) {
            players = new ArrayList<>();

            //add player
            players.add(new Player(getCharacter(),playerDeck));

            //add bots
            for (int i = 0; i < bots.length; i++)
                if (bots[i] >= 0) players.add(new Player(bots[i]));

            middle = new Card[2];
            frameCount = 0;
        }

        if (s.equals("MP_game")) {
            players = new ArrayList<>();

            //add player
            players.add(new Player(getCharacter(),playerDeck));

            //add placeholder for P2
            players.add(new Player(0));

            middle = new Card[2];
            frameCount = 0;
        }

        if (s.equals("1P_gameover") || s.equals("MP_gameover")) {
            transition = 0;
        }

        menu = s;
    }

    static Card getCard(int type, int p) {
        int id;
        do {
            id = (int)(Math.random()*N[type]);
        } while (power(type,id) != p);
        return new Card(type,id);
    }

    static List<Card> starterDeck() {
        List<Card> list = new ArrayList<>();

        //For each type:
        for (int type = 0; type < 3; type++) {
            //Add one card each of power 1-2, 3-4, and 5-6
            int a = (int)(Math.random()*2+1), b = (int)(Math.random()*2+3), c = (int)(Math.random()*2+5);

            list.add(getCard(type,a));
            list.add(getCard(type,b));
            list.add(getCard(type,c));
        }

        return list;
    }

    private void drawTitleMenu() {
        //title
        canvas.drawBitmap(pokebo,w()/2-pokebo.getWidth()/2,h()/3-pokebo.getHeight()/2,null);

        //play
        canvas.drawText("SINGLEPLAYER",w()/2,h()*4/6,w75);
        if (!isSignedIn()) w75.setAlpha(50);
        canvas.drawText("MULTIPLAYER",w()/2,h()*5/6,w75);
        w75.setAlpha(255);

        //view deck
        canvas.drawBitmap(deck,c480(20),c480(20),null);
        //sign in/out
        if (isSignedIn()) canvas.drawBitmap(loggedin,w()-c480(80),c480(20),null);
        canvas.drawBitmap(gplay,w()-c480(80),c480(20),null);
    }

    private void drawDeck() {
        canvas.drawText("My Cards", w()/2, c480(55), b50);

        //back to home
        canvas.drawBitmap(back,c480(20),c480(20),null);

        //previous page
        if (page > 0) canvas.drawBitmap(left,c480(20),c480(210),null);
        //next page
        if ((page+1)*12 < playerDeck.size()) canvas.drawBitmap(right,w()-c480(80),c480(210),null);

        float margin = c480(20);
        float w = c480(112), h = c480(80);
        float totalW = w*3+margin*2, totalH = h*4+margin*4;

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                int i = page*12 + r*3 + c;
                if (i >= playerDeck.size()) return;

                float left = w()/2-totalW/2+(w+margin)*c,
                        top = h()-totalH+(h+margin)*r;
                playerDeck.get(i).draw(left,top,left+w,top+h);
            }
        }
    }

    private void drawTrainerSelect() {
        int[] colors = {
                Color.rgb(255,170,135),
                Color.rgb(155,205,165),
                Color.rgb(165,165,255)
        };

        //Draw backgrounds and trainer sprites
        for (int i = 0; i < colors.length; i++) {
            float x = w()/6*(i*2+1);
            canvas.drawRect(w()/3*i,0,w()/3*(i+1),h(),newPaint(colors[i]));
            canvas.drawBitmap(trainers[i],null,new RectF(x-c480(96),h()/2-c480(96),x+c480(96),h()/2+c480(96)),null);
            if (selected != -1 && selected != i) canvas.drawRect(w()/3*i,0,w()/3*(i+1),h(),newPaint(Color.argb(50,0,0,0)));
        }

        if (selected == -1) {
            canvas.drawText("Before we start...", w()/2, c480(50), b50);
            canvas.drawText("...who are you?", w()/2, c480(100), b50);
        } else {
            canvas.drawText("So, is this you?", w()/2, c480(50), b50);
        }
    }

    private void drawGreeting() {
        canvas.drawBitmap(trainers[getCharacter()],null,
                new RectF(w()/2-c480(300),c480(264),w()/2-c480(108),c480(456)),null);
        playerDeck.get(playerDeck.size()-1).draw(w()/2-c480(36),c480(216),w()/2+c480(300),c480(456));

        canvas.drawText("Nice to meet you!", w()/2, c480(50), b50);
        canvas.drawText("Here's something to", w()/2, c480(100), b50);
        canvas.drawText("get you started.", w()/2, c480(150), b50);
    }

    private void draw1PLobby() {
        //player's character
        //canvas.drawText("Me",w()/8,h()/2-c480(89),b25);
        canvas.drawBitmap(trainers[getCharacter()],null,
                new RectF(w()/8-c480(64),h()/2-c480(64),w()/8+c480(64),h()/2+c480(64)),null);

        //bots
        for (int i = 0; i < 3; i++) {
            float mid = w()/8 * (i*2 + 3);
            if (bots[i] >= 0) {
                canvas.drawText("CPU"+(i+1),mid,h()/2-c480(89),b25);
                canvas.drawBitmap(trainers[bots[i]],null,
                        new RectF(mid-c480(64),h()/2-c480(64),mid+c480(64),h()/2+c480(64)),null);
            } else {
                canvas.drawText("+",mid,h()/2-(b100.ascent()+b100.descent())/2,b100);
            }
        }

        //start button
        if (bots[0] == -1 && bots[1] == -1 && bots[2] == -1) w75.setAlpha(50);
        canvas.drawText("Ready!",w()/2,h()-c480(30),w75);
        w75.setAlpha(255);
    }

    private void drawGame() {
        //draw hands
        if (middle[0] == null) players.get(0).drawHand(true);
        else players.get(0).drawHand(true,4);
        players.get(0).drawPoints(true);

        //opponent's hand
        if (middle[1] == null) players.get(1).drawHand(false);
        else players.get(1).drawHand(false,4);
        players.get(1).drawPoints(false);

        //draw middle
        float w = c480(112), h = c480(80), x1 = w()/2-c480(70), x2 = w()/2+c480(70);
        if (middle[0] != null) {
            middle[0].draw(x1-w/2,h()/2-h/2,x1+w/2,h()/2+h/2);
        }
        if (middle[1] != null) {
            if (middle[0] != null) {
                middle[1].draw(x2-w/2,h()/2-h/2,x2+w/2,h()/2+h/2);
            } else {
                middle[1].drawBack(x2-w/2,h()/2-h/2,x2+w/2,h()/2+h/2);
            }
        }
    }

    private void drawMPSelect() {
        canvas.drawText("MULTIPLAYER",w()/2,c480(55),b50);

        canvas.drawBitmap(back,c480(20),c480(20),null);

        canvas.drawBitmap(quick,w()/6-c480(64),h()/2-c480(64),null);
        canvas.drawText("Quick",w()/6,c480(380),b25);
        canvas.drawText("Match",w()/6,c480(405),b25);

        canvas.drawBitmap(create,w()/2-c480(64),h()/2-c480(64),null);
        canvas.drawText("Create",w()/2,c480(380),b25);
        canvas.drawText("Room",w()/2,c480(405),b25);

        canvas.drawBitmap(join,w()*5/6-c480(64),h()/2-c480(64),null);
        canvas.drawText("Join",w()*5/6,c480(380),b25);
        canvas.drawText("Room",w()*5/6,c480(405),b25);

        canvas.drawLine(w()/3,c480(100),w()/3,c480(430),line2);
        canvas.drawLine(w()*2/3,c480(100),w()*2/3,c480(430),line2);
    }

    private void drawGameOver() {
        drawGame();
        canvas.drawColor(Color.argb((int)Math.min(frameCount*2,150),0,0,0));

        String msg = players.get(0).won() ? "You win!" : "You lost.";
        canvas.drawText(msg, w()/2, c480(230), w125);

        canvas.drawText("Play Again?", w()/2, c480(330), w50);
        canvas.drawText("Back to Menu", w()/2, c480(400), w50);
    }
}
