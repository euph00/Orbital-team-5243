package com.orbital.scribex;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

public class CustomAnimatedButton extends AppCompatButton {

    private Runnable trigger;
    private boolean notNow = false;
    private Rect rect;
    private Animation scaleDown = AnimationUtils.loadAnimation(this.getContext(), R.anim.scaledown);
    private Animation scaleUp = AnimationUtils.loadAnimation(this.getContext(), R.anim.scaleup);

    public CustomAnimatedButton(@NonNull Context context) {
        super(context);
    }

    public CustomAnimatedButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAction(Runnable r) {
        this.trigger = r;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        assert (trigger != null);
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            this.startAnimation(scaleDown);
            this.setPressed(true);
            rect = new Rect(this.getLeft(), this.getTop(), this.getRight(), this.getBottom());
        }
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            if (!notNow) {
                this.startAnimation(scaleUp);
                trigger.run();
                this.setPressed(false);
            } else //button press canceled
                notNow = false;
        }
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
            if (!notNow)
                if (!rect.contains(this.getLeft() + (int) event.getX(), this.getTop() + (int) event.getY())) {
                    // finger moved out of bounds, return button image to original
                    this.startAnimation(scaleUp);
                    this.setPressed(false);
                    notNow = true; //cancel button press the next time
                }
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
