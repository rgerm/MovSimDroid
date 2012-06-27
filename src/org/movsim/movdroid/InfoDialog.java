package org.movsim.movdroid;

import android.os.Bundle;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

public class InfoDialog extends SherlockActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text);
        ((TextView)findViewById(R.id.text)).setText(R.string.dialog_content);
    }
}
