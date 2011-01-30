package org.pvoid.apteryxaustralis.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;
import org.pvoid.apteryxaustralis.accounts.Agent;

public class AgentListView extends ListView
{
  private Agent _mAgent;
  
  public AgentListView(Context context, Agent agent)
  {
    super(context);
    _mAgent = agent;
    setupUI(context);
  }
  
  public AgentListView(Context context, Agent agent, AttributeSet attrs)
  {
    super(context, attrs);
    _mAgent = agent;
    setupUI(context);
  }
  
  public AgentListView(Context context, Agent agent, AttributeSet attrs, int defStyle)
  {
    super(context, attrs, defStyle);
    _mAgent = agent;
    setupUI(context);
  }

  protected void setupUI(Context context)
  {
    // nope
  }

  public Agent getAgent()
  {
    return(_mAgent);
  }

}
