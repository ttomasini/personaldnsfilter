package dnsfilter.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;

public class PaddedCheckBox extends CheckBox {

	private static int dpAsPx_32 = 0;
	private static int dpAsPx_10 = 0;

	private int convertDpToPx(int padding_in_dp) {
		final float scale = getResources().getDisplayMetrics().density;
		return (int) (padding_in_dp * scale + 0.5f);
	}


	public PaddedCheckBox(Context context) {
		super(context);
		doPadding();
	}

	public PaddedCheckBox(Context context, AttributeSet attrs) {
		super(context, attrs);
		doPadding();
	}

	public PaddedCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		doPadding();
	}

	@SuppressLint("NewApi")
	public PaddedCheckBox(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		doPadding();
	}

	private void doPadding() {

		if (dpAsPx_32 == 0) {
			dpAsPx_32 = convertDpToPx(32);
			dpAsPx_10 = convertDpToPx(10);
		}

		this.setPadding(dpAsPx_10, dpAsPx_10, dpAsPx_10, dpAsPx_10);

	}
}
