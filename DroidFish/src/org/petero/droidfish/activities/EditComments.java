package org.petero.droidfish.activities;

import org.petero.droidfish.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/** Activity to edit PGN comments. */
public class EditComments extends Activity {
	TextView preComment;
	TextView postComment;
	private Button okButton;
	private Button cancelButton;
	int nag;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_comments);
        preComment = (TextView)findViewById(R.id.ed_comments_pre);
        TextView moveView = (TextView)findViewById(R.id.ed_comments_move);
        postComment = (TextView)findViewById(R.id.ed_comments_post);
        okButton = (Button)findViewById(R.id.ed_comments_ok);
        cancelButton = (Button)findViewById(R.id.ed_comments_cancel);

		postComment.requestFocus();

		Intent data = getIntent();
		Bundle bundle = data.getBundleExtra("org.petero.droidfish.comments");
		String pre = bundle.getString("preComment");
		String post = bundle.getString("postComment");
		String move = bundle.getString("move");
		nag = bundle.getInt("nag");
		preComment.setText(pre);
		postComment.setText(post);
		moveView.setText(move);

		okButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				sendBackResult();
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
	}
	
	private final void sendBackResult() {
		String pre = preComment.getText().toString().trim();
		String post = postComment.getText().toString().trim();
		
		Bundle bundle = new Bundle();
		bundle.putString("preComment", pre);
		bundle.putString("postComment", post);
		bundle.putInt("nag", nag);
		Intent data = new Intent();
		data.putExtra("org.petero.droidfish.comments", bundle);
		setResult(RESULT_OK, data);
		finish();
	}
}
