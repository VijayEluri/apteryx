package org.pvoid.apteryxaustralis.accounts;

import java.util.ArrayList;
import java.util.List;

import org.pvoid.apteryxaustralis.net.IResponseParser;
import org.xml.sax.Attributes;

public class TerminalsSection implements IResponseParser
{
  private final int STATE_NONE = 0;
  private final int STATE_TERMINALS = 1;
  
  private int _State;
  private ArrayList<Terminal> _Terminals = null;
  
  public static TerminalsSection getParser()
  {
    return(new TerminalsSection());
  }
  
  @Override
  public void SectionStart()
  {
    _State = STATE_NONE;
  }

  @Override
  public void SectionEnd()
  {
    // TODO Auto-generated method stub
  }

  @Override
  public void ElementStart(String name, Attributes attributes)
  {
    if(name.equals("getTerminals"))
    {
      _State = STATE_TERMINALS;
      return;
    }
////////
    if(name.equals("row") && _State==STATE_TERMINALS)
    {
      String id = attributes.getValue("trm_id");
      String agent_id = attributes.getValue("agt_id");
      Terminal terminal = new Terminal(Long.parseLong(id));
      terminal.setAgentId(Long.parseLong(agent_id));
      terminal.setAddress(attributes.getValue("full_address"));
      terminal.setDisplayName(attributes.getValue("trm_display"));
      if(_Terminals==null)
        _Terminals = new ArrayList<Terminal>();
      _Terminals.add(terminal);
    }
  }

  @Override
  public void ElementEnd(String name, String innerText)
  {
    if(name.equals("getTerminals"))
    {
      _State = STATE_NONE;
      return;
    }
  }
  
  public List<Terminal> getTerminals()
  {
    return(_Terminals);
  }
}