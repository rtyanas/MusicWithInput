package com.yanas.musicrandominput;

import com.yanas.musicrandominput.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Random;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class MusicControlActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_music_control);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.string_play);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.go_button).setOnTouchListener(mDelayHideTouchListener);

        final Button playMusic = (Button) findViewById(R.id.go_button);
        playMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundData sd = new SoundData();
                EditText et = (EditText)findViewById(R.id.string_play);
                String playString = et.getText().toString(); // "Hello World";
                byte[] bAr = playString.getBytes();
                Random rand;

                sd.duration = 2;
                sd.freqOfTone = 400;
                sd.freqVariationOn = false;

                for(byte b : bAr) {
                    rand = new Random(b);
                    sd.duration = rand.nextInt(3);
                    sd.freqVariationOn = rand.nextBoolean();
                    sd.freqOfTone = 700 - rand.nextInt(350);
                    new PlayTone().execute(sd);
                    try {
                        Thread.sleep(sd.duration * 900);  // Turn seconds into milli seconds, use partial since
                                                            // playing tone doesn't play the whole duration
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    } // onCreate()

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    /**
     * SoundData
     */
    class SoundData {
        int duration = 1; // seconds: 100 = 1 sec
        boolean freqVariationOn = false;
        private double freqOfTone = 440; // hz
    }


    /**
     * Create then play a tone
     */
    class PlayTone extends AsyncTask<SoundData, Integer, Integer> {

        // originally from http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
        // and modified by Steve Pomeroy <steve@staticfree.info>
        private int duration = 1; // seconds: 100 = 1 sec
        private int sampleRate = 8000;
        private int numSamples;
        private double sample[] ;
        private double freqOfTone = 440; // hz
        private double[] freqVariation = {1, 3, 6, 9, 18, 25};
        boolean frequencyVarOn = false;

        private byte generatedSnd[];

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Integer doInBackground(SoundData... frequency_in ) {
            if(frequency_in.length > 0) {
                freqOfTone = frequency_in[0].freqOfTone;
                duration = frequency_in[0].duration;
                frequencyVarOn = frequency_in[0].freqVariationOn;
                numSamples = duration * sampleRate;
                sample = new double[numSamples];
                generatedSnd = new byte[2 * numSamples];

                genTone();
                playSound();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {

        }

        final float per_20 = 0.2f;
        final float per_40 = 0.4f;
        final float per_60 = 0.6f;
        final float per_80 = 0.8f;

        void genTone() {
            int freqVarIndex = 0;
            if(freqOfTone < 80)
                freqVarIndex = 0;
            else if(freqOfTone >= 80 && freqOfTone < 150)
                freqVarIndex = 1;
            else if(freqOfTone >= 150 && freqOfTone < 230)
                freqVarIndex = 2;
            else if(freqOfTone >= 230 && freqOfTone < 280)
                freqVarIndex = 3;
            else if(freqOfTone >= 280 && freqOfTone < 370)
                freqVarIndex = 4;
            else if(freqOfTone >= 370)
                freqVarIndex = 5;

            float currNumSampPer ;
            double freqOfToneNew = freqOfTone;
            // fill out the array
            for (int i = 0; i < numSamples; ++i) {
                freqOfToneNew = freqOfTone;
                currNumSampPer = (float)i / (float)numSamples;

                if(frequencyVarOn) {
                    if( currNumSampPer < per_20)
                        freqOfToneNew = freqOfTone - (int)(Math.abs(freqVariation[freqVarIndex]) * per_80 );
                    else if( currNumSampPer >= per_20 && currNumSampPer < per_40)
                        freqOfToneNew = freqOfTone - (int)(Math.abs(freqVariation[freqVarIndex]) * per_60 );
                    else if( currNumSampPer >= per_40 && currNumSampPer < per_60)
                        freqOfToneNew = freqOfTone - (int)(Math.abs(freqVariation[freqVarIndex]) * per_40  );
                    else if( currNumSampPer >= per_60 && currNumSampPer < per_80)
                        freqOfToneNew = freqOfTone - (int)(Math.abs(freqVariation[freqVarIndex]) * per_20  );
                    else if( currNumSampPer >= per_80)
                        ; // freqOfTone += (int)(Math.abs(freqVariation[freqVarIndex]) * per_80);
                }

                sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfToneNew));
            }

            // convert to 16 bit pcm sound array
            // assumes the sample buffer is normalised.
            int idx = 0;
            for (final double dVal : sample) {
                // scale to maximum amplitude
                final short val = (short) ((dVal * 32767));
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

            }
        }

        void playSound(){
            AudioTrack audioTrack = null;
            try {
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                        AudioTrack.MODE_STATIC);
            } catch (Exception e) {
                Log.d("MusicControlActivity.playSound", "trace: "+e.getMessage());
            }
            try {
                audioTrack.write(generatedSnd, 0, generatedSnd.length);
                audioTrack.play();
            } catch (Exception e) {
                Log.e("MusicControlActivity", "playSound"+ e.getMessage());
            }
        }

    }  // class PlayTone

}
