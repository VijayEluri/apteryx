package org.pvoid.apteryx.ui;

import java.util.ArrayList;
import java.util.Comparator;

import org.pvoid.apteryx.R;
import org.pvoid.apteryx.accounts.Account;
import org.pvoid.apteryx.accounts.Accounts;
import org.pvoid.apteryx.accounts.Terminal;
import org.pvoid.apteryx.net.IStatesRespnseHandler;
import org.pvoid.apteryx.net.StatesRequestTask;
import org.pvoid.apteryx.net.TerminalsProcessData;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class MainActivity extends Activity implements IStatesRespnseHandler, OnItemClickListener
{
  private static final int SETTINGS_MENU_ID = Menu.FIRST+1; 
  private static final int REFRESH_MENU_ID = Menu.FIRST+2;
  
  private TerminalsProcessData _Terminals;
  private ListView _TerminalsList;
  private TerminalsArrayAdapter _TerminalsAdapter;
  private Accounts _Accounts;
  
  private static final Comparator<Terminal> _TerminalComparer = new Comparator<Terminal>()
  {
    @Override
    public int compare(Terminal object1, Terminal object2)
    {
      if(object1.State() == object2.State())
        return 0;
      if(object1.State()==0)
        return(-1);
      if(object2.State()==0)
        return(1);
      return(object1.State()-object2.State());
    }
  };
  
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    //requestWindowFeature(Window.FEATURE_PROGRESS);
    setContentView(R.layout.main);
    setProgressBarIndeterminateVisibility(false);
    //setProgressBarVisibility(false);
    _Terminals = new TerminalsProcessData();
    _TerminalsAdapter = new TerminalsArrayAdapter(this, R.layout.terminal);
    _TerminalsList = (ListView)findViewById(R.id.terminals_list);
    _Accounts = new Accounts(this);
    if(_TerminalsList!=null)
    {
      _TerminalsList.setAdapter(_TerminalsAdapter);
      _TerminalsList.setOnItemClickListener(this);
    }
    
    RefreshStates();
    //startService(new Intent(this,UpdateStatusService.class));
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    boolean result = super.onCreateOptionsMenu(menu);
    if(result)
    {
      MenuItem item = menu.add(Menu.NONE, REFRESH_MENU_ID, Menu.NONE, R.string.refresh);
      item.setIcon(android.R.drawable.ic_menu_directions);
      
      item = menu.add(Menu.NONE, SETTINGS_MENU_ID, Menu.NONE, R.string.settings);
      item.setIcon(android.R.drawable.ic_menu_preferences);
    }
    return(result);
  }
  
  private void ShowPreferencesActivity()
  {
    Intent intent = new Intent(this,Preferences.class);
    startActivityForResult(intent, 0); 
  }
  
  private void RefreshStates()
  {
    setProgressBarIndeterminateVisibility(true);
    ArrayList<Account> accounts = new ArrayList<Account>();
    _Accounts.GetAccounts(accounts);
    Account[] ac = new Account[accounts.size()];
    (new StatesRequestTask(this, _Terminals)).execute(accounts.toArray(ac));
  }
  
  public boolean onOptionsItemSelected(MenuItem item)
  {
    switch(item.getItemId())
    {
      case SETTINGS_MENU_ID:
        ShowPreferencesActivity();
        break;
      case REFRESH_MENU_ID:
        RefreshStates();
        break;
    }
    return(super.onOptionsItemSelected(item));
  }
  
  @Override
  public void onSuccessRequest()
  {
    _TerminalsAdapter.clear();
    for(String terminal_key : _Terminals)
    {
      _TerminalsAdapter.add(_Terminals.at(terminal_key));
    }
    _TerminalsAdapter.sort(_TerminalComparer);
    ArrayList<Terminal> states = new ArrayList<Terminal>();
    _Accounts.CheckStates(_Terminals, states);
    _Accounts.SaveStates(_Terminals);
    setProgressBarIndeterminateVisibility(false);
  }
  
  @Override
  public void onRequestError()
  {
    setProgressBarIndeterminateVisibility(false);
    // TODO Auto-generated method stub
    
  }
  @Override
  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
  {
    // TODO Auto-generated method stub
    Terminal terminal = _TerminalsAdapter.getItem(position);
    if(terminal!=null)
    {
      Intent intent = new Intent(this, FullInfo.class);
      intent.putExtra("terminal", terminal);
      startActivity(intent);
    }
  }
}