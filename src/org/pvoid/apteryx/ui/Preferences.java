package org.pvoid.apteryx.ui;

import org.pvoid.apteryx.Consts;
import org.pvoid.apteryx.R;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class Preferences extends TabActivity
{
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    
    final TabHost tabs = getTabHost();
    tabs.addTab(tabs.newTabSpec(Consts.TAB_ACCOUNTS)
                    .setIndicator(getString(R.string.accounts),getResources().getDrawable(R.drawable.icon))
                    .setContent(new Intent(this,AccountsList.class))
                );
    tabs.addTab(tabs.newTabSpec(Consts.TAB_PREFERENCES)
        .setIndicator(getString(R.string.settings),getResources().getDrawable(R.drawable.icon))
        .setContent(new Intent(this,CommonSettings.class))
    );
  }
}
