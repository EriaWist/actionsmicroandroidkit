package com.actionsmicro.debug.app;

import java.io.PrintWriter;
import java.io.StringWriter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.view.KeyEvent;

public class ExceptionAlertDialogFragment extends DialogFragment {
	private static final String BUNDLE_KEY_TITLE_RES_ID = "title_res_id";
	private static final String BUNDLE_KEY_TITLE = "title";
	private static final String BUNDLE_KEY_MESSAGE = "message";
	private static final String BUNDLE_KEY_ICON = "icon";
	
	public static ExceptionAlertDialogFragment newInstance(String title, Throwable e) {
		ExceptionAlertDialogFragment frag = new ExceptionAlertDialogFragment();
        Bundle args = new Bundle();
        args.putString(BUNDLE_KEY_TITLE, title);
        StringWriter stringWriter = new StringWriter();
		e.printStackTrace(new PrintWriter(stringWriter));
        args.putString(BUNDLE_KEY_MESSAGE, e.getLocalizedMessage()+"\n"+stringWriter.toString());
        
        frag.setArguments(args);
        return frag;
    }
	private DialogInterface.OnClickListener onClickListener;
	public void setPositiveButtonOnClickListener(DialogInterface.OnClickListener onClickListener) {
		this.onClickListener = onClickListener;
	}
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Bundle arguments = getArguments();
		String title = arguments.getString(BUNDLE_KEY_TITLE);
		if (title == null && arguments.getInt(BUNDLE_KEY_TITLE_RES_ID) != 0) {
			title = getActivity().getString(arguments.getInt(BUNDLE_KEY_TITLE_RES_ID));
		}
		Dialog dialog =  new AlertDialog.Builder(getActivity())
			.setTitle(title)
			.setIcon(arguments.getInt(BUNDLE_KEY_ICON))
			.setMessage(arguments.getString(BUNDLE_KEY_MESSAGE))
			.setCancelable(false)
			.setPositiveButton(android.R.string.ok,
				onClickListener!=null?onClickListener:
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						com.actionsmicro.utils.Utils.popAllBackStackAndFinish(getActivity());
					}
				}
			)
			.create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (KeyEvent.KEYCODE_BACK == keyCode) {
					return true;
				}
				return false;
			}
			
		});
		return dialog;
	}

}