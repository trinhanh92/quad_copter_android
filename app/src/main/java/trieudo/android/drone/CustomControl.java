package trieudo.android.drone;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CustomControl extends RelativeLayout {
    ImageView imageView;
    ProgressBar mProgressbar;
    private Context mContext;
    private TextView tvStatus;
    private boolean isFirstStart = true;

    public void setControlListener(OnControlListener mControlListener) {
        this.mControlListener = mControlListener;
    }

    private OnControlListener mControlListener;

    public CustomControl(Context context) {
        super(context);
    }

    public CustomControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.e("CustomControl", "CustomControl");
        mContext = context;
       // this.setBackgroundColor(Color.BLUE);

    }

    public void updateProgress(int progeress) {
        mProgressbar.setProgress(progeress);
    }

    @Override
    protected void onFinishInflate() {
        initView(mContext);
        super.onFinishInflate();
    }

    public CustomControl(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    PointF lastEvent;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                if(event.getPointerCount() > 1) return false;
                if(lastEvent == null) {
                    lastEvent = new PointF(event.getX(), event.getY());
                }
                float diffY = event.getY() - lastEvent.y;
                if (Math.abs(diffY) >= mProgressbar.getHeight() / (float) 100){
                    int progressP = mProgressbar.getProgress();
                    Log.e("diff", "diff: " + diffY + " b " + progressP + " / " + mProgressbar.getHeight());
                    if (diffY > 0) {
                        if (progressP > 0)
                            progressP -= 1;
                    } else {
                        if (progressP < 100)
                            progressP += 1;
                    }
                    mProgressbar.setProgress(progressP);
                    tvStatus.setText(String.valueOf(progressP));
                    if(mControlListener != null) {
                        mControlListener.onProgressing(progressP);
                    }
                    /*if(mVideoTouch != VideoTouch.DRAG_VERTICAL) {
                        hide();
                    }
                    mVideoTouch = VideoTouch.DRAG_VERTICAL;
                    show();
                    int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    currentVolume += diffY<0?1:-1;
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);*/
                    lastEvent = new PointF(event.getX(), event.getY());
                }

                break;

            case MotionEvent.ACTION_UP:
                lastEvent = null;
                /*if(needPause) {
                    pauseOrStartWhenDrag(false);
                    needPause = false;
                }
                if(mVideoTouch == VideoTouch.NONE)
                    show();
                else {
                    mVideoTouch = VideoTouch.NONE;
                }*/
                break;
        }
        return true;
    }

    /*private boolean getInRect () {

    }*/

    void initView(Context mContext) {
        RelativeLayout.LayoutParams params = new LayoutParams(200, 200);
        //params.setMargins();
        imageView = new ImageView(mContext);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.up_and_down));
        imageView.setLayoutParams(params);
        mProgressbar = (ProgressBar) this.findViewById(R.id.gasControl);
        mProgressbar.setMax(100);
        mProgressbar.setProgress(0);
        tvStatus = (TextView) this.findViewById(R.id.tvStatus);

    }
}
