package org.pvoid.apteryxaustralis.net;

import org.pvoid.apteryxaustralis.accounts.ReportsSection;
import org.pvoid.apteryxaustralis.accounts.TerminalStatus;

public class StatusRefreshRunnable implements Runnable
{
  private final String _mLogin;
  private final String _mPassword;
  private final String _mTerminal;
  private Iterable<TerminalStatus> _mStatuses = null;

  public StatusRefreshRunnable(String terminal, String login, String password)
  {
    _mLogin = login;
    _mPassword = password;
    _mTerminal = terminal;
  }

	public static Iterable<TerminalStatus> GetStatuses(String login, String passwordHash, String terminal)
	{
		Request request = new Request(login, passwordHash, terminal);
		request.getTerminalsStatus();
		Response response = request.getResponse();
		if(response==null)
			return null;
////////
		ReportsSection section = response.Reports();
		if(section==null)
			return null;
////////
		return section.getTerminalsStatus();
	}
	
	@Override
	public void run()
	{
    _mStatuses = GetStatuses(_mLogin,_mPassword,_mTerminal);
	}

  public Iterable<TerminalStatus> getStatuses()
  {
    return _mStatuses;
  }
}
