package trieudo.android.drone;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class ZController
{
	public interface ControlChangeListener
	{
		void ControlChanged(int zPercentage);
	}

	private static final int BM_WIDTH = 100;
	private static final int BM_HEIGHT = 200;
	private static final int IM_BG_WIDTH = 40;
	private static final int IM_BG_HEIGHT = 200;
	private static final int IM_VALUE_X = 15;
	private static final int IM_VALUE_WIDTH = 11;
	private static final int IM_VALUE_BOTTOM = 164;
	private static final int IM_CTL_X = 5;
	private static final int IM_CTL_Y = 137;
	private static final int IM_CTL_WIDTH = 30;
	private static final int IM_CTL_HEIGHT = 30;
	private static final int MAX_VALUE = 125;

	private int bitmapWidth;
	private int bitmapHeight;
	private int backgroundWidth;
	private int backgroundHeight;
	private int valueBarXPosition;
	private int valueBarWidth;
	private int valueBarBottom;
	private int controllerXPosition;
	private int controllerOriginalYPosition;
	private int controllerWidth;
	private int controllerHeight;
	private int maxValue;

	private Activity activity;
	private boolean controlling;
	private ImageView imageView;
	private Bitmap imageBackground;
	private Bitmap imageValueBar;
	private Bitmap imageController;
	private ControlChangeListener controlChangeListener;

	public ZController(Activity activity)
	{
		this.activity = activity;
		this.controlling = false;
		this.imageView = (ImageView) activity.findViewById(R.id.IV_Z);
		this.imageBackground = BitmapFactory.decodeResource(activity.getResources(), R.drawable.z_bg);
		this.imageValueBar = BitmapFactory.decodeResource(activity.getResources(), R.drawable.z_value);
		this.imageController = BitmapFactory.decodeResource(activity.getResources(), R.drawable.z_ctrl);
		this.getConfigurations();
		this.updateControl(0);
		this.imageView.setOnTouchListener(this.imageViewTouchListener);
	}

	public void setControlChangeListener(ZController.ControlChangeListener listener)
	{
		this.controlChangeListener = listener;
	}

	private int convertToPixel(int dip)
	{
		return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, this.activity.getResources().getDisplayMetrics());
	}

	private void getConfigurations()
	{
		this.bitmapWidth = this.convertToPixel(BM_WIDTH);
		this.bitmapHeight = this.convertToPixel(BM_HEIGHT);
		this.backgroundWidth = this.convertToPixel(IM_BG_WIDTH);
		this.backgroundHeight = this.convertToPixel(IM_BG_HEIGHT);
		this.valueBarXPosition = this.convertToPixel(IM_VALUE_X);
		this.valueBarWidth = this.convertToPixel(IM_VALUE_WIDTH);
		this.valueBarBottom = this.convertToPixel(IM_VALUE_BOTTOM);
		this.controllerXPosition = this.convertToPixel(IM_CTL_X);
		this.controllerOriginalYPosition = this.convertToPixel(IM_CTL_Y);
		this.controllerWidth = this.convertToPixel(IM_CTL_WIDTH);
		this.controllerHeight = this.convertToPixel(IM_CTL_HEIGHT);
		this.maxValue = this.convertToPixel(MAX_VALUE);
	}

	private View.OnTouchListener imageViewTouchListener = new View.OnTouchListener()
	{
		private float dX;
		private float dY;
		private int yPosition;

		@Override
		public boolean onTouch(View view, MotionEvent motionEvent)
		{
			switch (motionEvent.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					this.dX = motionEvent.getX() - controllerXPosition;
					this.dY = motionEvent.getY() - controllerOriginalYPosition;
					if (this.dX > 0 && this.dX < controllerWidth && this.dY > 0 && this.dY < controllerHeight)
						controlling = true;
					else
						controlling = false;
					break;
				case MotionEvent.ACTION_MOVE:
					if (controlling == true)
					{
						this.yPosition = (int)(motionEvent.getY() - this.dY);
						updateControl(this.yPosition - controllerOriginalYPosition);
					}
					break;
				case MotionEvent.ACTION_UP:
					//updateControl(0);
					controlling = false;
					break;
			}

			return true;
		}
	};

	private void updateControl(float yOffset)
	{
		float validYOffset;
		if (yOffset < 0 - this.maxValue)
			validYOffset = 0 - this.maxValue;
		else if (yOffset > 0)
			validYOffset = 0;
		else
			validYOffset = yOffset;

		int validYPosition = this.controllerOriginalYPosition + (int) validYOffset;

		Bitmap bitmap = Bitmap.createBitmap(this.bitmapWidth, this.bitmapHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		// draw background
		canvas.drawBitmap(this.imageBackground, null, new Rect(0, 0, this.backgroundWidth, this.backgroundHeight), null);
		// draw value bar
		canvas.drawBitmap(this.imageValueBar, null, new Rect(this.valueBarXPosition, validYPosition + this.controllerHeight / 2, this.valueBarXPosition + this.valueBarWidth, this.valueBarBottom), null);
		// draw controller
		canvas.drawBitmap(this.imageController, null, new Rect(this.controllerXPosition, validYPosition, this.controllerXPosition + this.controllerWidth, validYPosition + this.controllerHeight), null);
		this.imageView.setImageBitmap(bitmap);
		if (this.controlChangeListener != null)
			this.controlChangeListener.ControlChanged(0 - (int) Math.ceil((validYOffset * 100) / this.maxValue));
	}
}
