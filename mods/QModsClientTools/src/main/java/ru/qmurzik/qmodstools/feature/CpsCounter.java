package ru.qmurzik.qmodstools.feature;

import org.lwjgl.input.Mouse;

import java.util.ArrayDeque;
import java.util.Deque;

public final class CpsCounter {
    private boolean leftWasDown, rightWasDown;
    private final Deque<Long> leftClicks = new ArrayDeque<Long>();
    private final Deque<Long> rightClicks = new ArrayDeque<Long>();

    public void tick() {
        boolean left = Mouse.isButtonDown(0), right = Mouse.isButtonDown(1);
        long now = System.currentTimeMillis();
        if (left && !leftWasDown) leftClicks.addLast(now);
        if (right && !rightWasDown) rightClicks.addLast(now);
        leftWasDown = left; rightWasDown = right;
        trim(leftClicks, now); trim(rightClicks, now);
    }

    private void trim(Deque<Long> q, long now) { while (!q.isEmpty() && now - q.peekFirst() > 1000L) q.pollFirst(); }
    public int leftCps() { return leftClicks.size(); }
    public int rightCps() { return rightClicks.size(); }
}
