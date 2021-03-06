/*
 * Copyright (C) 2010-2011  Dmitry Petuhov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.pvoid.apteryxaustralis.types;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class Account implements Parcelable
{
  public long id;
  public String title;
  public String login;
  public String passwordHash;
  public String terminal;
  
  public Account(long id, String title, String login,String password,String terminal)
  {
    this.id = id;
    this.title = title;
    this.login = login;
    passwordHash = password;
    this.terminal = terminal;
  }
  public String toString()
  {
    if(TextUtils.isEmpty(title))
      return(login);
    return(title);
  }

  @Override
  public int describeContents()
  {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i)
  {
    parcel.writeLong(id);
    parcel.writeString(title);
    parcel.writeString(login);
    parcel.writeString(passwordHash);
    parcel.writeString(terminal);
  }

  public static final Parcelable.Creator<Account> CREATOR = new Parcelable.Creator<Account>()
  {
    public Account createFromParcel(Parcel parcel)
    {
      return new Account(parcel.readLong(),parcel.readString(),parcel.readString(),parcel.readString(),parcel.readString());
    }

    public Account[] newArray(int size)
    {
      return new Account[size];
    }
  };
}
