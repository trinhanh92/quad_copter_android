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

/**
 * Created by Do Trieu on 5/24/2016.
 */
public class XYController
{
	public interface ControlChangeListener
	{
		void ControlChanged(int xPercentage, int yPercentage);
	}

	// define the dimension values of the images. The value of these variables is specified in the DIP unit
	private static final int BM_WIDTH = 300;
	private static final int BM_HEIGHT = 200;
	private static final int IM_BG_X = 60;
	private static final int IM_BG_Y = 30;
	private static final int IM_BG_WIDTH = 120;
	private static final int IM_BG_HEIGHT = 120;
	private static final int IM_CTL_X = 102;
	private static final int IM_CTL_Y = 73;
	private static final int IM_CTL_WIDTH = 36;
	private static final int IM_CTL_HEIGHT = 36;
	private static final int MAX_RADIUS = 70;

	// used to store the dimension values of the images. The value of these variables is specified in the PIXEL unit
	private int bitmapWidth;
	private int bitmapHeight;
	private int backgroundXPosition;
	private int backgroundYPosition;
	private int backgroundWidth;
	private int backgroundHeight;
	private int controllerOriginalXPosition;
	private int controllerOriginalYPosition;
	private int controllerWidth;
	private int controllerHeight;
	private double maxRadius;

	private Activity activity;
	private boolean controlling;
	private ImageView imageView;
	private Bitmap imageBackground;
	private Bitmap imageController;
	private ControlChangeListener controlChangeListener;

	public XYController(Activity activity)
	{
		this.activity = activity;
		this.controlling = false;
		this.imageView = (ImageView) activity.findViewById(R.id.IV_XY);
		this.imageBackground = BitmapFactory.decodeResource(activity.getResources(), R.drawable.xy_bg);
		this.imageController = BitmapFactory.decodeResource(activity.getResources(), R.drawable.xy_ctrl);
		this.getConfigurations();
		// draw the images with the original positions
		this.updateControl(0, 0);
		this.imageView.setOnTouchListener(this.imageViewTouchListener);
	}

	public void setControlChangeListener(XYController.ControlChangeListener listener)
	{
		this.controlChangeListener = listener;
	}

	private float convertToPixel(int dip)
	{
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, this.activity.getResources().getDisplayMetrics());
	}

	private void getConfigurations()
	{
		this.bitmapWidth = (int)this.convertToPixel(BM_WIDTH);
		this.bitmapHeight = (int)this.convertToPixel(BM_HEIGHT);
		this.backgroundXPosition = (int)this.convertToPixel(IM_BG_X);
		this.backgroundYPosition = (int)this.convertToPixel(IM_BG_Y);
		this.backgroundWidth = (int)this.convertToPixel(IM_BG_WIDTH);
		this.backgroundHeight = (int)this.convertToPixel(IM_BG_HEIGHT);
		this.controllerOriginalXPosition = (int)this.convertToPixel(IM_CTL_X);
		this.controllerOriginalYPosition = (int)this.convertToPixel(IM_CTL_Y);
		this.controllerWidth = (int)this.convertToPixel(IM_CTL_WIDTH);
		this.controllerHeight = (int)this.convertToPixel(IM_CTL_HEIGHT);
		this.maxRadius = this.convertToPixel(MAX_RADIUS);
	}

	private View.OnTouchListener imageViewTouchListener = new View.OnTouchListener()
	{
		private float dX;
		private float dY;
		private int xPosition;
		private int yPosition;

		@Override
		public boolean onTouch(View view, MotionEvent motionEvent)
		{
			switch (motionEvent.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					this.dX = motionEvent.getX() - controllerOriginalXPosition;
					this.dY = motionEvent.getY() - controllerOriginalYPosition;
					if (this.dX > 0 && this.dX < controllerWidth && this.dY > 0 && this.dY < controllerHeight)
						controlling = true;
					else
						controlling = false;
					break;
				case MotionEvent.ACTION_MOVE:
					if (controlling == true)
					{
						this.xPosition = (int)(motionEvent.getX() - this.dX);
						this.yPosition = (int)(motionEvent.getY() - this.dY);
						updateControl(this.xPosition - controllerOriginalXPosition, this.yPosition - controllerOriginalYPosition);
					}
					break;
				case MotionEvent.ACTION_UP:
					updateControl(0, 0);
					controlling = false;
					break;
			}

			return true;
		}
	};

	private void updateControl(float xOffset, float yOffset)
	{
		double validXOffset = xOffset;
		double validYOffset = yOffset;
		double radius = Math.sqrt((xOffset * xOffset) + (yOffset * yOffset));
		if (radius > this.maxRadius)
		{
			validXOffset = (this.maxRadius * xOffset) / radius;
			validYOffset = (this.maxRadius * yOffset) / radius;
		}
		int validXPosition = this.controllerOriginalXPosition + (int) validXOffset;
		int validYPosition = this.controllerOriginalYPosition + (int) validYOffset;

		Bitmap bitmap = Bitmap.createBitmap(this.bitmapWidth, this.bitmapHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		// draw background
		canvas.drawBitmap(this.imageBackground, null, new Rect(this.backgroundXPosition, this.backgroundYPosition, this.backgroundXPosition + this.backgroundWidth, this.backgroundYPosition + this.backgroundHeight), null);
		// draw controller
		canvas.drawBitmap(this.imageController, null, new Rect(validXPosition, validYPosition, validXPosition + this.controllerWidth, validYPosition + this.controllerHeight), null);
		this.imageView.setImageBitmap(bitmap);
		if (this.controlChangeListener != null)
			this.controlChangeListener.ControlChanged((int) Math.ceil((validXOffset * 100) / this.maxRadius), 0 - (int) Math.ceil((validYOffset * 100) / this.maxRadius));
	}
}
