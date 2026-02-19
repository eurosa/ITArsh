package com.googleapi.bluetoothweight;



import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import androidx.appcompat.widget.AppCompatEditText;

public class NumberEditText extends AppCompatEditText {

    public NumberEditText(Context context) {
        super(context);
        init();
    }

    public NumberEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NumberEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Set to number input type
        setRawInputType(EditorInfo.TYPE_CLASS_NUMBER);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        // This method is called before the IME handles the key
        // It's the earliest point we can intercept key events

        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                // Move focus to the next view
                View next = focusSearch(View.FOCUS_DOWN);
                if (next != null) {
                    next.requestFocus();
                }

                // Return true to consume the event completely
                // This prevents the IME from ever seeing the Enter key
                return true;
            }
            // Also consume ACTION_UP to be safe
            return true;
        }

        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        // This intercepts the input connection to the IME
        return new InputConnectionWrapper(super.onCreateInputConnection(outAttrs), true) {
            @Override
            public boolean sendKeyEvent(KeyEvent event) {
                // Intercept key events before they're sent to the IME
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER ||
                            event.getKeyCode() == KeyEvent.KEYCODE_NUMPAD_ENTER) {

                        // Move focus to next view
                        View next = focusSearch(View.FOCUS_DOWN);
                        if (next != null) {
                            next.requestFocus();
                        }

                        // Consume the event
                        return true;
                    }
                }
                return super.sendKeyEvent(event);
            }

            @Override
            public boolean performEditorAction(int actionCode) {
                // Intercept editor actions
                if (actionCode == EditorInfo.IME_ACTION_NEXT ||
                        actionCode == EditorInfo.IME_ACTION_DONE) {

                    View next = focusSearch(View.FOCUS_DOWN);
                    if (next != null) {
                        next.requestFocus();
                    }
                    return true;
                }
                return super.performEditorAction(actionCode);
            }
        };
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Handle key down events
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            View next = focusSearch(View.FOCUS_DOWN);
            if (next != null) {
                next.requestFocus();
            }
            return true; // Consume the event
        }
        return super.onKeyDown(keyCode, event);
    }
}