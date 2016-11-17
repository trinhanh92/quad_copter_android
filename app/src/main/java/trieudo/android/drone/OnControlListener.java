package trieudo.android.drone;

public interface OnControlListener {
    void onStart();
    void onProgressing(int percent);
    void onEnd();
}
