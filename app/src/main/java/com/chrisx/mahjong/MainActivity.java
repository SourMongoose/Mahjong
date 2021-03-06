package com.chrisx.mahjong;

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
import android.media.MediaPlayer;
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
import java.util.HashSet;
import java.util.List;

import static java.util.Collections.shuffle;
import static java.util.Collections.sort;

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

    private final int ID_INITIALIZE = 0,
            ID_PLAY = 1,
            ID_RECEIVED = 2,
            ID_DRAW = 10,
            ID_TAKE = 11,
            ID_REVEAL = 20,
            ID_WIN = 30,
            ID_REMATCH = 31;

    private GoogleSignInAccount acc;
    private GoogleSignInOptions gsio;
    private GoogleSignInClient gsic;

    private RoomConfig mJoinedRoomConfig;
    private String mMyParticipantId;

    static Bitmap[][] tiles;
    static Bitmap mahjong, soundon, soundoff, gplay, loggedin, tile_back, back, quick, create, join;

    private MediaPlayer mp;

    static Typeface font;

    private boolean paused = false;
    private long frameCount = 0;

    private String menu = "start";
    private int turn, selected = -1;
    private int lastMid;
    private boolean checkedHand;
    private boolean canWinMid, canWinHand;
    private int[] won;
    private boolean[] received, rematch;
    private byte[] deckBytes;

    //frame data
    private static final int FRAMES_PER_SECOND = 60;
    private long nanosecondsPerFrame;

    private int TRANSITION_MAX = FRAMES_PER_SECOND * 2 / 3;
    private int transition = TRANSITION_MAX / 2;
    private int animation;

    private float downX, downY;

    private Paint w50, w75, w125, b15, b20, b25, b50, b75, b100, line2, line3, button;

    private List<Tile> deck, middle;
    private List<List<Tile>> hands, revealed;

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

        tiles = new Bitmap[3][9];
        int[] pos = {R.drawable.pin1, R.drawable.bamboo1, R.drawable.man1};
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                tiles[i][j] = Bitmap.createBitmap(BitmapFactory.decodeResource(res, pos[i]+j),
                    22, 0, 84, 128);
            }
        }

        mahjong = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.mahjong),
                Math.round(c480(345)),Math.round(c480(170)),false);
        soundon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.soundon),
                Math.round(c480(60)),Math.round(c480(60)),false);
        soundoff = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.soundoff),
                Math.round(c480(60)),Math.round(c480(60)),false);
        gplay = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.googleplay),
                Math.round(c480(60)),Math.round(c480(60)),false);
        loggedin = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.loggedin),
                Math.round(c480(60)),Math.round(c480(60)),false);
        tile_back = Bitmap.createBitmap(BitmapFactory.decodeResource(res, R.drawable.facedown),
                22, 0, 84, 128);
        back = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.back),
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

        b15 = new Paint(b50);
        b15.setTextSize(c480(15));

        b20 = new Paint(b50);
        b20.setTextSize(c480(20));

        b25 = new Paint(b50);
        b25.setTextSize(c480(25));

        b75 = new Paint(b50);
        b75.setTextSize(c480(75));

        b100 = new Paint(b50);
        b100.setTextSize(c480(100));

        line2 = newPaint(Color.BLACK);
        line2.setStrokeWidth(c480(2));

        line3 = newPaint(Color.WHITE);
        line3.setStrokeWidth(c480(3));

        button = newPaint(Color.rgb(220,220,220));


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
                                if (menu.equals("MP_game")) {
                                    //if players are still waiting for deck
                                    if (getPlayerIndex() == 0) {
                                        for (int i = 1; i < nPlayers(); i++) {
                                            if (!received[i] && frameCount % 300 == 0) sendToAllReliably(deckBytes);
                                        }
                                    }

                                    List<Tile> hand = hands.get(getPlayerIndex());
                                    if (hand.size() % 3 == 2 && !checkedHand) {
                                        canWinHand = checkForWin(hand);
                                        checkedHand = true;
                                    } else if (hand.size() % 3 != 2) {
                                        checkedHand = false;
                                    }

                                    if (hand.size() % 3 == 1 && lastMid != turn && !middle.isEmpty()) {
                                        canWinMid = checkForWin(hand, middle.get(middle.size()-1));
                                        lastMid = turn;
                                    }
                                } else if (menu.equals("MP_gameover")) {
                                    //rematch
                                    if (getPlayerIndex() == 0) {
                                        boolean shouldRematch = true;

                                        for (int i = 0; i < nPlayers(); i++)
                                            if (!rematch[i]) shouldRematch = false;

                                        if (shouldRematch) {
                                            goToMenu("MP_select");
                                            goToMenu("MP_game");
                                            startGame();
                                        }
                                    } else if (rematch[getPlayerIndex()]) {
                                        if (frameCount % 120 == 0)
                                            sendToAllReliably(new byte[]{ID_REMATCH, (byte) getPlayerIndex()});
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
                                    } else if (menu.equals("MP_select")) {
                                        drawMPSelect();
                                    } else if (menu.equals("MP_waiting")) {
                                        Bitmap bmp = tiles[0][(int)frameCount/15%9];
                                        canvas.drawBitmap(bmp,w()/2-bmp.getWidth()/2,h()/2-bmp.getHeight()/2,null);
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
        if (menu.equals("MP_select")) {
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
                Rect mp = new Rect();
                w75.getTextBounds("START",0,5,mp);

                //toggle sound
                if (X < c480(100) && Y < c480(100)) {
                    editor.putBoolean("sound_on", !soundOn());
                    editor.apply();
                }

                //sign in to google play
                if (X > w()-c480(100) && Y < c480(100)) {
                    if (isSignedIn()) signOut();
                    else startSignInIntent();
                }
                //multiplayer mode
                else if (X > w()/2-mp.width()/2 && X < w()/2+mp.width()/2
                        && Y > h()*5/6-mp.height() && Y < h()*5/6) {
                    if (isSignedIn()) goToMenu("MP_select");
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
        } else if (menu.equals("MP_gameover")) {
            if (action == MotionEvent.ACTION_DOWN) {
                Rect rm = new Rect(), bm = new Rect();
                w50.getTextBounds("Rematch",0,7,rm);
                w50.getTextBounds("Back to Menu",0,12,bm);

                //rematch
                if (X > w()/2-rm.width()/2 && X < w()/2+rm.width()/2
                        && Y > c480(350)+rm.top && Y < c480(350)+rm.bottom) {
                    rematch[getPlayerIndex()] = true;
                    sendToAllReliably(new byte[]{ID_REMATCH,(byte)getPlayerIndex()});
                }
                //back to menu
                if (X > w()/2-bm.width()/2 && X < w()/2+bm.width()/2
                        && Y > c480(400)+bm.top && Y < c480(400)+bm.bottom) {
                    leaveRoom();
                }
            }
        } else if (menu.equals("MP_game")) {
            if (action == MotionEvent.ACTION_DOWN) {
                //back to MP select
                if (X < c480(80) && Y < c480(80)) leaveRoom();

                List<Tile> hand = hands.get(getPlayerIndex());

                String[] text = {"Draw", "Play", "Triple", "Quad", "Win"};
                float w = c480(75), h = c480(30), margin = c480(20);
                float left = w()/2 - (w*text.length + margin*(text.length-1))/2,
                        top = h() - c480(20) - c480(64) - margin - h;

                //handle button presses
                for (int i = 0; i < text.length; i++) {
                    if (X > left+i*(w+margin) && X < left+i*(w+margin)+w
                            && Y > top && Y < top+h) {
                        if (text[i].equals("Draw")) {
                            if (turn == getPlayerIndex() && hand.size() % 3 == 1 && frameCount > 4*FRAMES_PER_SECOND) {
                                //make sure all players have received the deck
                                if (getPlayerIndex() == 0) {
                                    for (int j = 1; i < nPlayers(); i++) {
                                        if (!received[j]) return true;
                                    }
                                }
                                sendToAllReliably(new byte[]{ID_DRAW,(byte)getPlayerIndex()});
                                hand.add(nextTileFromDeck());
                            }
                        } else if (text[i].equals("Play")) {
                            if (turn == getPlayerIndex() && selected != -1) {
                                sendToAllReliably(new byte[]{ID_PLAY,(byte)getPlayerIndex(),
                                        (byte)hand.get(selected).getType(),(byte)hand.get(selected).getID()});
                                middle.add(copy(hand.get(selected)));
                                playSound(middle.get(middle.size()-1));
                                hand.remove(selected);
                                sort(hand);
                                selected = -1;

                                nextTurn();
                            }
                        } else if (text[i].equals("Triple")) {
                            if (turn != (getPlayerIndex()+1)%nPlayers() && canTakeTriple() && frameCount > 2*FRAMES_PER_SECOND)
                                takeTriple();
                        } else if (text[i].equals("Quad")) {
                            if (turn != (getPlayerIndex()+1)%nPlayers() && canTakeQuad() && frameCount > 2*FRAMES_PER_SECOND)
                                takeQuad();
                            else if (hand.size() % 3 == 2 && canShowQuad())
                                showQuad();
                        } else if (text[i].equals("Win")) {
                            if (turn != (getPlayerIndex()+1)%nPlayers() && !middle.isEmpty()
                                    && lastMid == turn && canWinMid && hands.get(getPlayerIndex()).size() % 3 == 1) {
                                //take winning tile from middle
                                sendToAllReliably(new byte[]{ID_TAKE,(byte)getPlayerIndex()});
                                revealed.get(getPlayerIndex()).add(copy(middle.get(middle.size()-1)));
                                middle.remove(middle.size()-1);
                                //declare victory
                                sendToAllReliably(new byte[]{ID_WIN,(byte)getPlayerIndex()});
                                setWin(getPlayerIndex());
                                nextTurn();
                            } else if (checkedHand && canWinHand) {
                                //reveal winning tile
                                Tile win = hand.get(hand.size()-1);
                                sendToAllReliably(new byte[]{ID_REVEAL,(byte)getPlayerIndex(),
                                        (byte)win.getType(),(byte)win.getID()});
                                revealed.get(getPlayerIndex()).add(copy(win));
                                hand.remove(hand.size()-1);
                                //declare victory
                                sendToAllReliably(new byte[]{ID_WIN,(byte)getPlayerIndex()});
                                setWin(getPlayerIndex());
                                nextTurn();
                            }
                        }
                    }
                }

                //select tile
                if (turn == getPlayerIndex() && hands.get(getPlayerIndex()).size() % 3 == 2) {
                    List<Tile> reveal = revealed.get(getPlayerIndex());
                    w = c480(42);
                    h = c480(64);
                    float totalW = (hand.size() + reveal.size()) * w + c480(20);
                    left = w()/2 - totalW/2 + reveal.size()*w + c480(20);
                    top = h() - c480(20) - h;
                    for (int i = 0; i < hand.size(); i++) {
                        if (X > left+w*i && X < left+w*(i+1) && Y > top && Y < top+h) {
                            selected = i;
                        }
                    }
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
                mRoom = room;
                showWaitingRoom(room, 4);
            } else {
                Log.w(TAG, "Error creating room: " + code);
                leaveRoom();
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
                showWaitingRoom(room, 4);
            } else {
                Log.w(TAG, "Error joining room: " + code);
                leaveRoom();
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
                leaveRoom();
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
        // 2 to 4 people
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(1, 3, 0);

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
        // minimum: 1 other player; maximum: 3 other players
        getClient().getSelectOpponentsIntent(1, 3, true)
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

            /**
             * 0 - initialize deck (0, deck)
             * 1 - add to middle (1, player, type, id)
             * 2 - received deck (2, player)
             * 10 - draw from deck (10, player)
             * 11 - take from middle (11, player)
             * 20 - reveal (20, player, type, id)
             * 30 - declare victory (30, player)
             * 31 - rematch (31, player)
             */
            if (message[0] == ID_INITIALIZE) {
                //let player 1 know that you've received the msg
                sendToAllReliably(new byte[]{ID_RECEIVED,(byte)getPlayerIndex()});

                //rematch
                if (menu.equals("MP_gameover")) {
                    goToMenu("MP_select");
                    goToMenu("MP_game");
                }

                if (deck.isEmpty()) {
                    for (int i = 1; i < message.length; i++) {
                        deck.add(new Tile(message[i]/9,message[i]%9));
                    }
                    for (int i = 0; i < nPlayers(); i++) {
                        for (int j = 0; j < 13; j++)
                            hands.get(i).add(nextTileFromDeck());
                        sort(hands.get(i));
                    }
                }
            } else if (message[0] == ID_PLAY) {
                List<Tile> hand = hands.get(message[1]);
                for (int i = 0; i < hand.size(); i++) {
                    if (hand.get(i).getType() == message[2] && hand.get(i).getID() == message[3]) {
                        hand.remove(i);
                        middle.add(new Tile(message[2],message[3]));
                        playSound(middle.get(middle.size()-1));
                        break;
                    }
                }
                nextTurn();
                if (nPlayers() > 2) frameCount = 0;
            } else if (message[0] == ID_RECEIVED) {
                if (getPlayerIndex() == 0)
                    received[message[1]] = true;
            } else if (message[0] == ID_DRAW) {
                hands.get(message[1]).add(nextTileFromDeck());
            } else if (message[0] == ID_TAKE) {
                revealed.get(message[1]).add(copy(middle.get(middle.size()-1)));
                middle.remove(middle.size()-1);
                sort(revealed.get(message[1]));
                turn = message[1];
            } else if (message[0] == ID_REVEAL) {
                List<Tile> hand = hands.get(message[1]);
                for (int i = 0; i < hand.size(); i++) {
                    if (hand.get(i).getType() == message[2] && hand.get(i).getID() == message[3]) {
                        hand.remove(i);
                        revealed.get(message[1]).add(new Tile(message[2],message[3]));
                        break;
                    }
                }
                sort(revealed.get(message[1]));
            } else if (message[0] == ID_WIN) {
                setWin(message[1]);
                nextTurn();
            } else if (message[0] == ID_REMATCH) {
                if (getPlayerIndex() == 0)
                    rematch[message[1]] = true;
            }
        }
    };

    private int getPlayerIndex() {
        if (mRoom == null) return -1;

        for (int i = 0; i < nPlayers(); i++) {
            if (mRoom.getParticipantIds().get(i).equals(mMyParticipantId)) {
                return i;
            }
        }
        return -1;
    }

    private int nPlayers() {
        if (mRoom == null) return 0;
        return mRoom.getParticipantIds().size();
    }

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

                //If you're player 1:
                if (getPlayerIndex() == 0) startGame();
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

    static float c480(float f) {
        return h() / (480 / f);
    }
    static float c854(float f) {
        return w() / (854 / f);
    }

    private boolean soundOn() {
        return sharedPref.getBoolean("sound_on", true);
    }

    private void goToMenu(String s) {
        transition = TRANSITION_MAX;

        if (s.equals("MP_select")) {
            //initialize decks
            deck = new ArrayList<>();
            middle = new ArrayList<>();
            hands = new ArrayList<>();
            revealed = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                hands.add(new ArrayList<Tile>());
                revealed.add(new ArrayList<Tile>());
            }

            turn = 0;
            selected = -1;
            lastMid = -1;
            checkedHand = false;
            canWinMid = canWinHand = false;
            won = new int[4];
            received = new boolean[4];
        } else if (s.equals("MP_gameover")) {
            frameCount = 0;
            transition = 0;

            //sort player hands
            for (int i = 0; i < nPlayers(); i++) {
                sort(hands.get(i));
            }

            rematch = new boolean[4];
        }

        menu = s;
    }

    boolean canTakeTriple() {
        if (middle.size() < 1) return false;

        List<Tile> hand = hands.get(getPlayerIndex());

        //check that players haven't drawn
        for (int i = 0; i < nPlayers(); i++) {
            if (hands.get(i).size() % 3 != 1) return false;
        }

        int matches = 0;
        for (Tile t : hand) {
            if (t.compareTo(middle.get(middle.size()-1)) == 0) matches++;
        }
        return matches >= 2;
    }
    void takeTriple() {
        List<Tile> hand = hands.get(getPlayerIndex()),
                reveal = revealed.get(getPlayerIndex());

        int matches = 0;
        for (int i = hand.size()-1; i >= 0 && matches < 2; i--) {
            if (hand.get(i).compareTo(middle.get(middle.size()-1)) == 0) {
                matches++;
                sendToAllReliably(new byte[]{ID_REVEAL,(byte)getPlayerIndex(),
                        (byte)hand.get(i).getType(),(byte)hand.get(i).getID()});
                reveal.add(copy(hand.get(i)));
                hand.remove(i);
            }
        }
        sendToAllReliably(new byte[]{ID_TAKE,(byte)getPlayerIndex()});
        reveal.add(copy(middle.get(middle.size()-1)));
        middle.remove(middle.size()-1);

        sort(reveal);
        turn = getPlayerIndex();
    }

    boolean canTakeQuad() {
        if (middle.size() < 1) return false;

        List<Tile> hand = hands.get(getPlayerIndex());

        //check that players haven't drawn
        for (int i = 0; i < nPlayers(); i++) {
            if (hands.get(i).size() % 3 != 1) return false;
        }

        int matches = 0;
        for (Tile t : hand) {
            if (t.compareTo(middle.get(middle.size()-1)) == 0) matches++;
        }
        return matches >= 3;
    }
    void takeQuad() {
        List<Tile> hand = hands.get(getPlayerIndex()),
                reveal = revealed.get(getPlayerIndex());

        int matches = 0;
        for (int i = hand.size()-1; i >= 0 && matches < 3; i--) {
            if (hand.get(i).compareTo(middle.get(middle.size()-1)) == 0) {
                matches++;
                sendToAllReliably(new byte[]{ID_REVEAL,(byte)getPlayerIndex(),
                        (byte)hand.get(i).getType(),(byte)hand.get(i).getID()});
                reveal.add(copy(hand.get(i)));
                hand.remove(i);
            }
        }
        sendToAllReliably(new byte[]{ID_TAKE,(byte)getPlayerIndex()});
        reveal.add(copy(middle.get(middle.size()-1)));
        middle.remove(middle.size()-1);

        sort(reveal);
        turn = getPlayerIndex();
    }

    boolean canShowQuad() {
        if (turn != getPlayerIndex()) return false;

        List<Tile> hand = hands.get(getPlayerIndex()),
                reveal = revealed.get(getPlayerIndex());
        if (hand.size() % 3 != 2) return false;

        int[][] occur = new int[3][9];
        boolean[][] inHand = new boolean[3][9];

        for (Tile t : hand) {
            occur[t.getType()][t.getID()]++;
            inHand[t.getType()][t.getID()] = true;
        }
        for (Tile t : reveal) {
            occur[t.getType()][t.getID()]++;
        }

        for (int i = 0; i < occur.length; i++) {
            for (int j = 0; j < occur[i].length; j++) {
                if (occur[i][j] >= 4 && inHand[i][j]) return true;
            }
        }

        return false;
    }
    void showQuad() {
        List<Tile> hand = hands.get(getPlayerIndex()),
                reveal = revealed.get(getPlayerIndex());

        int[][] occur = new int[3][9];
        boolean[][] inHand = new boolean[3][9];

        for (Tile t : hand) {
            occur[t.getType()][t.getID()]++;
            inHand[t.getType()][t.getID()] = true;
        }
        for (Tile t : reveal) {
            occur[t.getType()][t.getID()]++;
        }

        for (int i = 0; i < occur.length; i++) {
            for (int j = 0; j < occur[i].length; j++) {
                if (occur[i][j] >= 4 && inHand[i][j]) {
                    for (int t = hand.size()-1; t >= 0; t--) {
                        if (hand.get(t).compareTo(new Tile(i,j)) == 0) {
                            sendToAllReliably(new byte[]{ID_REVEAL,
                                    (byte)getPlayerIndex(),(byte)i,(byte)j});
                            reveal.add(copy(hand.get(t)));
                            hand.remove(t);
                        }
                    }

                    sort(reveal);
                    selected = -1;
                    return;
                }
            }
        }
    }

    boolean checkForWin(List<Tile> hand) {
        List<Tile> newList = new ArrayList<>();
        for (Tile t : hand) newList.add(copy(t));
        sort(newList);

        //two-suit rule
        boolean[] appears = new boolean[3];
        for (Tile t : newList) appears[t.getType()] = true;
        if (appears[0] && appears[1] && appears[2]) return false;

        //check for 7 pairs
        if (newList.size() == 14) {
            boolean works = true;
            for (int i = 0; i < 14; i += 2) {
                if (newList.get(i).compareTo(newList.get(i+1)) != 0) {
                    works = false;
                    break;
                }
            }
            if (works) return true;
        }

        //check for 4 triples and a pair
        return check(newList, 0,
                new ArrayList<Tile>(),
                new ArrayList<Tile>(),
                new ArrayList<Tile>(),
                new ArrayList<Tile>(),
                new ArrayList<Tile>());
    }
    boolean checkForWin(List<Tile> hand, Tile t) {
        List<Tile> newList = new ArrayList<>();
        for (Tile tile : hand) newList.add(copy(tile));
        newList.add(copy(t));

        return checkForWin(newList);
    }
    boolean check(List<Tile> hand, int index, List<Tile> pair, List<Tile> t1, List<Tile> t2, List<Tile> t3, List<Tile> t4) {
        if (index == hand.size()) {
            //check all possibilites; some triples may already be revealed
            if (index == 2) return pair.size() == 2;
            if (index == 5) return pair.size() + t1.size() == 5;
            if (index == 8) return pair.size() + t1.size() + t2.size() == 8;
            if (index == 11) return pair.size() + t1.size() + t2.size() + t3.size() == 11;
            if (index == 14) return pair.size() + t1.size() + t2.size() + t3.size() + t4.size() == 14;
            return false;
        }

        Tile curr = hand.get(index);

        if (pair.isEmpty() || (pair.size() == 1 && curr.compareTo(pair.get(0)) == 0)) {
            pair.add(copy(curr));
            if (check(hand,index+1,pair,t1,t2,t3,t4)) return true;
            pair.remove(pair.size()-1);
        }
        if (t1.isEmpty()
                || t1.size() == 1 && (curr.compareTo(t1.get(0)) == 0 || curr.compareTo(t1.get(0)) == 1)
                || t1.size() == 2 && (curr.compareTo(t1.get(0)) == 0 && curr.compareTo(t1.get(1)) == 0
                        || curr.compareTo(t1.get(1)) == 1 && t1.get(1).compareTo(t1.get(0)) == 1)) {
            t1.add(copy(curr));
            if (check(hand,index+1,pair,t1,t2,t3,t4)) return true;
            t1.remove(t1.size()-1);
        }
        if (!t1.isEmpty()) {
            if (t2.isEmpty()
                    || t2.size() == 1 && (curr.compareTo(t2.get(0)) == 0 || curr.compareTo(t2.get(0)) == 1)
                    || t2.size() == 2 && (curr.compareTo(t2.get(0)) == 0 && curr.compareTo(t2.get(1)) == 0
                            || curr.compareTo(t2.get(1)) == 1 && t2.get(1).compareTo(t2.get(0)) == 1)) {
                t2.add(copy(curr));
                if (check(hand,index+1,pair,t1,t2,t3,t4)) return true;
                t2.remove(t2.size()-1);
            }
        }
        if (!t1.isEmpty() && !t2.isEmpty()) {
            if (t3.isEmpty()
                    || t3.size() == 1 && (curr.compareTo(t3.get(0)) == 0 || curr.compareTo(t3.get(0)) == 1)
                    || t3.size() == 2 && (curr.compareTo(t3.get(0)) == 0 && curr.compareTo(t3.get(1)) == 0
                            || curr.compareTo(t3.get(1)) == 1 && t3.get(1).compareTo(t3.get(0)) == 1)) {
                t3.add(copy(curr));
                if (check(hand,index+1,pair,t1,t2,t3,t4)) return true;
                t3.remove(t3.size()-1);
            }
        }
        if (!t1.isEmpty() && !t2.isEmpty() && !t3.isEmpty()) {
            if (t4.isEmpty()
                    || t4.size() == 1 && (curr.compareTo(t4.get(0)) == 0 || curr.compareTo(t4.get(0)) == 1)
                    || t4.size() == 2 && (curr.compareTo(t4.get(0)) == 0 && curr.compareTo(t4.get(1)) == 0
                            || curr.compareTo(t4.get(1)) == 1 && t4.get(1).compareTo(t4.get(0)) == 1)) {
                t4.add(copy(curr));
                if (check(hand,index+1,pair,t1,t2,t3,t4)) return true;
                t4.remove(t4.size()-1);
            }
        }

        return false;
    }

    void startGame() {
        //send initial deck to all players
        deck = startingDeck();
        deckBytes = new byte[1+deck.size()];
        deckBytes[0] = ID_INITIALIZE;
        for (int i = 0; i < deck.size(); i++) {
            deckBytes[i+1] = (byte)(deck.get(i).getType()*9+deck.get(i).getID());
        }
        sendToAllReliably(deckBytes);

        for (int i = 0; i < nPlayers(); i++) {
            for (int j = 0; j < 13; j++)
                hands.get(i).add(nextTileFromDeck());
            sort(hands.get(i));
        }
    }

    void setWin(int i) {
        int mx = 0, nWon = 0;
        for (int x = 0; x < won.length; x++) {
            mx = Math.max(mx,won[x]);
        }
        won[i] = mx + 1;

        //go to that player
        turn = i;

        //end game
        for (int x = 0; x < won.length; x++) {
            if (won[x] > 0) nWon++;
        }
        if (nWon + 1 == nPlayers()) goToMenu("MP_gameover");
    }

    void nextTurn() {
        if (mRoom == null) return;
        do {
            turn = (turn + 1) % nPlayers();
        } while (won[turn] > 0);
    }

    static List<Tile> startingDeck() {
        List<Tile> list = new ArrayList<>();

        //For each type:
        for (int type = 0; type < 3; type++)
            for (int id = 0; id < 9; id++)
                for (int i = 0; i < 4; i++)
                    list.add(new Tile(type,id));

        shuffle(list);
        return list;
    }

    private Tile copy(Tile t) {
        return new Tile(t.getType(), t.getID());
    }

    private Tile nextTileFromDeck() {
        Tile output = copy(deck.get(0));
        deck.remove(0);

        //ran out of tiles
        if (deck.size() == 0) goToMenu("MP_gameover");

        return output;
    }

    private void drawTitleMenu() {
        //title
        canvas.drawBitmap(mahjong,w()/2-mahjong.getWidth()/2,h()/3-mahjong.getHeight()/2,null);

        //play
        if (!isSignedIn()) w75.setAlpha(50);
        canvas.drawText("START",w()/2,h()*5/6,w75);
        w75.setAlpha(255);

        //sound
        if (soundOn()) canvas.drawBitmap(soundon,c480(20),c480(20),null);
        else canvas.drawBitmap(soundoff,c480(20),c480(20),null);

        //sign in/out
        if (isSignedIn()) canvas.drawBitmap(loggedin,w()-c480(80),c480(20),null);
        canvas.drawBitmap(gplay,w()-c480(80),c480(20),null);
    }

    private void drawGame() {
        //back to menu
        canvas.drawBitmap(back,c480(10),c480(10),null);

        //players are still waiting for deck message
        if (getPlayerIndex() == 0) {
            boolean ready = true;
            for (int i = 1; i < nPlayers(); i++) {
                if (!received[i]) {
                    ready = false;
                    break;
                }
            }
            if (!ready) {
                canvas.drawText("Waiting for players...",w()/2,h()/2+c480(25),w50);
            }
        }

        //draw player's tiles
        List<Tile> hand = hands.get(getPlayerIndex());
        List<Tile> reveal = revealed.get(getPlayerIndex());
        float w = c480(42), h = c480(64);
        float totalW = (hand.size() + reveal.size()) * w + c480(20);
        float left = w()/2 - totalW/2,
                top = h() - c480(20) - h;
        //draw revealed tiles
        for (int i = 0; i < reveal.size(); i++) {
            canvas.save();
            canvas.translate(left+w*i+w/2,top+h/2+c480(10));
            canvas.rotate(180);
            reveal.get(i).draw(-w/2,-h/2,w/2,h/2);
            canvas.restore();
        }
        //draw hidden tiles
        left += c480(20) + reveal.size()*w;
        for (int i = 0; i < hand.size(); i++) {
            if (i != selected) {
                hand.get(i).draw(left+w*i,top,left+w*(i+1),top+h);
            } else {
                hand.get(i).draw(left+w*i,top-c480(10),left+w*(i+1),top+h-c480(10));
            }
        }
        //player's turn
        if (turn == getPlayerIndex()) {
            canvas.drawLine(w()/2-c480(50),h()-c480(10),w()/2+c480(50),h()-c480(10),line3);
        }

        w = c480(21);
        h = c480(32);

        //draw player 2's tiles
        int p2index = getPlayerIndex() < 1 ? 1 : 0;
        hand = hands.get(p2index);
        left = w()/2-(hand.size()/2f)*w;
        top = c480(20);
        for (int i = 0; i < hand.size(); i++) {
            if (menu.equals("MP_game")) hand.get(i).drawBack(left+w*i,top,left+w*(i+1),top+h);
            else hand.get(i).draw(left+w*i,top,left+w*(i+1),top+h);
        }
        hand = revealed.get(p2index);
        left = w()/2-(hand.size()/2f)*w;
        top += c480(20) + h;
        for (int i = 0; i < hand.size(); i++) {
            hand.get(i).draw(left+w*i,top,left+w*(i+1),top+h);
        }
        //player 2's turn
        if (turn == p2index) {
            canvas.drawLine(w()/2-c480(50),c480(10),w()/2+c480(50),c480(10),line3);
        }
        //player 2 name
        try {
            canvas.drawText(mRoom.getParticipants().get(p2index).getDisplayName(),
                    w()/2, top-c480(5), b15);
        } catch (Exception ex) {}


        //draw player 3's tiles
        int p3index = getPlayerIndex() < 2 ? 2 : 1;
        hand = hands.get(p3index);
        canvas.save();
        canvas.translate(0,h()/2);
        canvas.rotate(90);
        left = -(hand.size()/2f)*w;
        top = -c480(20) - h;
        for (int i = 0; i < hand.size(); i++) {
            if (menu.equals("MP_game")) hand.get(i).drawBack(left+w*i,top,left+w*(i+1),top+h);
            else hand.get(i).draw(left+w*i,top,left+w*(i+1),top+h);
        }
        //player 3 name
        try {
            canvas.drawText(mRoom.getParticipants().get(p3index).getDisplayName(),
                    0, top-c480(5), b15);
        } catch (Exception ex) {}
        hand = revealed.get(p3index);
        left = -(hand.size()/2f)*w;
        top -= c480(20) + h;
        for (int i = 0; i < hand.size(); i++) {
            hand.get(i).draw(left+w*i,top,left+w*(i+1),top+h);
        }
        canvas.restore();
        //player 3's turn
        if (turn == p3index) {
            canvas.drawLine(c480(10),h()/2-c480(50),c480(10),h()/2+c480(50),line3);
        }

        //draw player 4's tiles
        int p4index = getPlayerIndex() < 3 ? 3 : 2;
        hand = hands.get(p4index);
        canvas.save();
        canvas.translate(w(),h()/2);
        canvas.rotate(270);
        left = -(hand.size()/2f)*w;
        top = -c480(20) - h;
        for (int i = 0; i < hand.size(); i++) {
            if (menu.equals("MP_game")) hand.get(i).drawBack(left+w*i,top,left+w*(i+1),top+h);
            else hand.get(i).draw(left+w*i,top,left+w*(i+1),top+h);
        }
        //player 4 name
        try {
            canvas.drawText(mRoom.getParticipants().get(p4index).getDisplayName(),
                    0, top-c480(5), b15);
        } catch (Exception ex) {}
        hand = revealed.get(p4index);
        left = -(hand.size()/2f)*w;
        top -= c480(20) + h;
        for (int i = 0; i < hand.size(); i++) {
            hand.get(i).draw(left+w*i,top,left+w*(i+1),top+h);
        }
        canvas.restore();
        //player 4's turn
        if (turn == p4index) {
            canvas.drawLine(w()-c480(10),h()/2-c480(50),w()-c480(10),h()/2+c480(50),line3);
        }

        //middle tiles
        int rows = 5, columns = 17;
        left = w()/2-columns/2f*w;
        top = h()/2-rows/2f*h-h/2;
        for (int i = 0; i < middle.size(); i++) {
            middle.get(i).draw(left+(i%columns)*w,top+(i/columns)*h,left+(i%columns+1)*w,top+(i/columns+1)*h);
        }

        drawButtons();
    }
    private void drawButtons() {
        String[] text = {"Draw", "Play", "Triple", "Quad", "Win"};
        float w = c480(75), h = c480(30), margin = c480(20);
        float left = w()/2 - (w*text.length + margin*(text.length-1))/2,
                top = h() - c480(20) - c480(64) - margin - h;

        for (int i = 0; i < text.length; i++) {
            button.setAlpha(100);
            b20.setAlpha(50);

            if (won[getPlayerIndex()] == 0) {
                if (text[i].equals("Draw")) {
                    if (turn == getPlayerIndex() && hands.get(getPlayerIndex()).size() % 3 == 1) {
                        int alpha = (int)Math.min(255, 50 + 205 * ((int)frameCount / (4.*FRAMES_PER_SECOND)));
                        button.setAlpha(alpha);
                        b20.setAlpha(alpha);
                    }
                } else if (text[i].equals("Play")) {
                    if (turn == getPlayerIndex() && selected != -1) {
                        button.setAlpha(255);
                        b20.setAlpha(255);
                    }
                } else if (text[i].equals("Triple")) {
                    if (turn != (getPlayerIndex()+1)%nPlayers() && canTakeTriple()) {
                        int alpha = (int)Math.min(255, 50 + 205 * ((int)frameCount / (2.*FRAMES_PER_SECOND)));
                        button.setAlpha(alpha);
                        b20.setAlpha(alpha);
                    }
                } else if (text[i].equals("Quad")) {
                    if (turn != (getPlayerIndex()+1)%nPlayers() && canTakeQuad()
                            || hands.get(getPlayerIndex()).size() % 3 == 2 && canShowQuad()) {
                        if (canTakeQuad()) {
                            int alpha = (int)Math.min(255, 50 + 205 * ((int)frameCount / (2.*FRAMES_PER_SECOND)));
                            button.setAlpha(alpha);
                            b20.setAlpha(alpha);
                        } else {
                            button.setAlpha(255);
                            b20.setAlpha(255);
                        }
                    }
                } else if (text[i].equals("Win")) {
                    if (turn != (getPlayerIndex()+1)%nPlayers() && !middle.isEmpty()
                            && lastMid == turn && canWinMid && hands.get(getPlayerIndex()).size() % 3 == 1
                            || checkedHand && canWinHand) {
                        button.setAlpha(255);
                        b20.setAlpha(255);
                    }
                }
            }

            canvas.drawRoundRect(new RectF(left+i*(w+margin),top,left+i*(w+margin)+w,top+h), c480(5), c480(5), button);
            canvas.drawText(text[i], left+i*(w+margin)+w/2, top+c480(23), b20);
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

        String[] place = {"last.","1st!","2nd!","3rd."};
        String msg = "You placed " + place[won[getPlayerIndex()]];
        canvas.drawText(msg, w()/2, c480(230), w75);

        if (rematch[getPlayerIndex()]) canvas.drawText("Waiting...", w()/2, c480(350), w50);
        else canvas.drawText("Rematch", w()/2, c480(350), w50);

        canvas.drawText("Back to Menu", w()/2, c480(400), w50);
    }

    private void playSound(Tile t) {
        //check if sound is muted
        if (!soundOn()) return;

        int tmp = t.getType() == 0 ? R.raw.tong1 : t.getType() == 1 ? R.raw.tiao1 : R.raw.wan1;
        mp = MediaPlayer.create(this, tmp+t.getID());
        mp.start();
    }
}
