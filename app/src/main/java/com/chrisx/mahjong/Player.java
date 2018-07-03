package com.chrisx.mahjong;

import android.graphics.Canvas;
import android.graphics.RectF;

import java.util.List;

import static java.util.Collections.shuffle;

class Player {
    private List<Tile> tiles;
    private int[] points;
    private int character;

    Player(int character) {
        this.character = character;

        tiles = MainActivity.starterDeck();
        shuffle(tiles);

        points = new int[]{0,0,0};
    }
    Player(int character, List<Tile> tiles) {
        this.character = character;

        this.tiles = tiles;
        shuffle(this.tiles);

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

    List<Tile> getTiles() {
        return tiles;
    }

    Tile play(int i) {
        Tile c = tiles.remove(i);
        tiles.add(c);
        return c;
    }

    void drawHand(boolean player) {
        float margin = MainActivity.c480(80) / 6;
        float w = MainActivity.c480(112), h = MainActivity.c480(80);
        if (player) {
            for (int i = 0; i < 5; i++) {
                tiles.get(i).draw(margin,margin+i*(h+margin),margin+w,(i+1)*(h+margin));
            }
        } else {
            for (int i = 0; i < 5; i++) {
                tiles.get(i).drawBack(MainActivity.w()-margin-w,margin+i*(h+margin),MainActivity.w()-margin,(i+1)*(h+margin));
            }
        }
    }
    void drawHand(boolean player, int n) {
        float margin = MainActivity.c480(80) / 6;
        float w = MainActivity.c480(112), h = MainActivity.c480(80);
        if (player) {
            for (int i = 0; i < n; i++) {
                tiles.get(i).draw(margin,margin+i*(h+margin),margin+w,(i+1)*(h+margin));
            }
        } else {
            for (int i = 0; i < n; i++) {
                tiles.get(i).drawBack(MainActivity.w()-margin-w,margin+i*(h+margin),MainActivity.w()-margin,(i+1)*(h+margin));
            }
        }
    }
}
