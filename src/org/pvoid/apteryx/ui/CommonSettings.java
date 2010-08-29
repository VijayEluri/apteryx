package org.pvoid.apteryx.ui;

import org.pvoid.apteryx.Consts;
import org.pvoid.apteryx.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.Spinner;

public class CommonSettings extends Activity implements OnClickListener,OnItemSelectedListener
{
  private CheckedTextView _AutoCheck;
  private Spinner _Interval;
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings);
    
    _AutoCheck = (CheckedTextView)findViewById(R.id.settings_autocheck);
    _AutoCheck.setOnClickListener(this);
    _Interval = (Spinner)findViewById(R.id.settings_interval);
    
    ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.intervals, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    _Interval.setAdapter(adapter);
    SharedPreferences prefs = getSharedPreferences(Consts.APTERYX_PREFS, MODE_PRIVATE);
    int interval = prefs.getInt(Consts.PREF_INTERVAL, 3);
    int interval_position = 3;
    for(int index=0;index<Consts.INTERVALS.length;index++)
    {
      if(Consts.INTERVALS[index] == interval)
      {
        interval_position = index;
        break;
      }
    }
    _Interval.setSelection(interval_position);
    _Interval.setEnabled(false);
    _Interval.setOnItemSelectedListener(this);
  }
  @Override
  public void onClick(View v)
  {
    _AutoCheck.toggle();
    _Interval.setEnabled(_AutoCheck.isChecked());
    SharedPreferences prefs = getSharedPreferences(Consts.APTERYX_PREFS, MODE_PRIVATE);
    SharedPreferences.Editor edit = prefs.edit();
    edit.putBoolean(Consts.PREF_AUTOCHECK, _AutoCheck.isChecked());
    edit.commit();
  }
  @Override
  public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3)
  {
    int interval = Consts.INTERVALS[position];
    SharedPreferences prefs = getSharedPreferences(Consts.APTERYX_PREFS, MODE_PRIVATE);
    SharedPreferences.Editor edit = prefs.edit();
    edit.putInt(Consts.PREF_INTERVAL, interval);
    edit.commit();
  }
  @Override
  public void onNothingSelected(AdapterView<?> arg0)
  {
    // TODO Auto-generated method stub
    
  }
}
