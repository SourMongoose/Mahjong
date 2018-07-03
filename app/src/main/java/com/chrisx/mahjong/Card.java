package com.chrisx.mahjong;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

class Card implements Comparable<Card> {
    private int type, id, power;
    private Bitmap pokemon, icon, card;

    Card(int type, int id) {
        this.type = type;
        this.id = id;

        //assign card power
        power = MainActivity.power(type,id);

        //bitmaps
        pokemon = MainActivity.sprites[type][id];
        icon = MainActivity.icons[type];
        card = power < 9 ? MainActivity.cards[type] : MainActivity.powercards[type];
    }

    int getPower() {
        return power;
    }
    int getType() {
        return type;
    }
    int getID() {
        return id;
    }

    int compare(Card c) {
        int a = type, b = c.getType();

        if ((a+1)%3 == b) return 1;
        if ((b+1)%3 == a) return -1;
        return power - c.getPower();
    }

    public int compareTo(Card c) {
        return (power*3+type) - (c.getPower()*3+c.getType());
    }

    void draw(float left, float top, float right, float bottom) {
        Canvas c = MainActivity.canvas;

        float sideW = (right - left) - (bottom - top);

        c.drawBitmap(card, null, new RectF(left,top,right,bottom), null);
        c.drawBitmap(pokemon, null, new RectF(left+sideW,top,right,bottom), null);
        c.drawBitmap(icon, null, new RectF(left,(bottom+top)/2,left+sideW,(bottom+top)/2+sideW), null);

        Paint p = MainActivity.newPaint(Color.BLACK);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(sideW*.8f);
        c.drawText(power+"",left+sideW/2,(bottom+top)/2-sideW*.2f,p);
    }

    void drawBack(float left, float top, float right, float bottom) {
        Canvas c = MainActivity.canvas;
        c.drawBitmap(MainActivity.card_back,null,new RectF(left,top,right,bottom),null);
    }
}
