package com.chrisx.mahjong;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

class Tile implements Comparable<Tile> {
    private int type, id;
    private Bitmap bmp;

    Tile(int type, int id) {
        this.type = type;
        this.id = id;

        //bitmaps
        bmp = MainActivity.tiles[type][id];
    }

    int getType() {
        return type;
    }
    int getID() {
        return id;
    }

    public int compareTo(Tile c) {
        return type*9+id - (c.getType()*9+c.getID());
    }

    void draw(float left, float top, float right, float bottom) {
        Canvas c = MainActivity.canvas;
        c.drawBitmap(bmp, null, new RectF(left,top,right,bottom), null);
    }

    void drawBack(float left, float top, float right, float bottom) {
        Canvas c = MainActivity.canvas;
        c.drawBitmap(MainActivity.tile_back,null,new RectF(left,top,right,bottom),null);
    }
}
