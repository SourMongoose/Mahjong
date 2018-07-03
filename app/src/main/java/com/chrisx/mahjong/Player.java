package com.chrisx.mahjong;

import android.graphics.Canvas;
import android.graphics.RectF;

import java.util.List;

import static java.util.Collections.shuffle;

class Player {
    private List<Card> cards;
    private int[] points;
    private int character;

    Player(int character) {
        this.character = character;

        cards = MainActivity.starterDeck();
        shuffle(cards);

        points = new int[]{0,0,0};
    }
    Player(int character, List<Card> cards) {
        this.character = character;

        this.cards = cards;
        shuffle(this.cards);

        points = new int[]{0,0,0};
    }

    boolean won() {
        return points[0] > 2 || points[1] > 2 || points[2] > 2
                || (points[0] > 0 && points[1] > 0 && points[2] > 0);
    }

    void addPoint(int type) {
        points[type]++;
    }
    int[] getPoints() {
        return points;
    }

    List<Card> getCards() {
        return cards;
    }

    Card play(int i) {
        Card c = cards.remove(i);
        cards.add(c);
        return c;
    }

    void drawHand(boolean player) {
        float margin = MainActivity.c480(80) / 6;
        float w = MainActivity.c480(112), h = MainActivity.c480(80);
        if (player) {
            for (int i = 0; i < 5; i++) {
                cards.get(i).draw(margin,margin+i*(h+margin),margin+w,(i+1)*(h+margin));
            }
        } else {
            for (int i = 0; i < 5; i++) {
                cards.get(i).drawBack(MainActivity.w()-margin-w,margin+i*(h+margin),MainActivity.w()-margin,(i+1)*(h+margin));
            }
        }
    }
    void drawHand(boolean player, int n) {
        float margin = MainActivity.c480(80) / 6;
        float w = MainActivity.c480(112), h = MainActivity.c480(80);
        if (player) {
            for (int i = 0; i < n; i++) {
                cards.get(i).draw(margin,margin+i*(h+margin),margin+w,(i+1)*(h+margin));
            }
        } else {
            for (int i = 0; i < n; i++) {
                cards.get(i).drawBack(MainActivity.w()-margin-w,margin+i*(h+margin),MainActivity.w()-margin,(i+1)*(h+margin));
            }
        }
    }

    void drawPoints(boolean player) {
        Canvas c = MainActivity.canvas;

        float w = 48, margin = MainActivity.c480(80) / 6;
        float top = MainActivity.h() - margin - 3*w;
        if (player) {
            float left = MainActivity.c480(112) + margin*2;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < points[i]; j++) {
                    c.drawBitmap(MainActivity.icons[i],null,
                            new RectF(left+j*w,top+i*w,left+(j+1)*w,top+(i+1)*w),null);
                }
            }
        } else {
            float right = MainActivity.w() - MainActivity.c480(112) - margin*2;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < points[i]; j++) {
                    c.drawBitmap(MainActivity.icons[i],null,
                            new RectF(right-(j+1)*w,top+i*w,right-j*w,top+(i+1)*w),null);
                }
            }
        }
    }
}
