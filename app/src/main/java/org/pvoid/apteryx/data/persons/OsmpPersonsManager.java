/*
 * Copyright (C) 2010-2015  Dmitry "PVOID" Petuhov
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

package org.pvoid.apteryx.data.persons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import dagger.ObjectGraph;
import org.pvoid.apteryx.ApteryxApplication;
import org.pvoid.apteryx.annotations.GuardedBy;
import org.pvoid.apteryx.data.Storage;
import org.pvoid.apteryx.data.agents.Agent;
import org.pvoid.apteryx.data.terminals.Terminal;
import org.pvoid.apteryx.data.terminals.TerminalState;
import org.pvoid.apteryx.data.terminals.TerminalsManager;
import org.pvoid.apteryx.net.OsmpInterface;
import org.pvoid.apteryx.net.OsmpRequest;
import org.pvoid.apteryx.net.OsmpResponse;
import org.pvoid.apteryx.net.RequestExecutor;
import org.pvoid.apteryx.net.ResultCallback;
import org.pvoid.apteryx.net.commands.GetAgentInfoCommand;
import org.pvoid.apteryx.net.commands.GetAgentsCommand;
import org.pvoid.apteryx.net.commands.GetPersonInfoCommand;
import org.pvoid.apteryx.net.results.GetAgentInfoResult;
import org.pvoid.apteryx.net.results.GetAgentsResult;
import org.pvoid.apteryx.net.results.GetPersonInfoResult;
import org.pvoid.apteryx.util.log.Loggers;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/* package */ class OsmpPersonsManager implements PersonsManager {

    private static final Logger LOG = Loggers.getLogger(Loggers.Accounts);

    @NonNull private final Context mContext;
    @NonNull private final Storage mStorage;
    @NonNull private final TerminalsManager mTerminalsManager;
    @NonNull private final RequestExecutor mExecutor;
    @NonNull private final Lock mLock = new ReentrantLock();
    @GuardedBy("mLock") @NonNull private final NavigableMap<String, Person> mPersons = new TreeMap<>();
    @GuardedBy("mLock") @Nullable private Person[] mPersonsList = null;
    @GuardedBy("mLock") @Nullable private Person mCurrentPerson = null;
    @GuardedBy("mLock") @Nullable private Agent mCurrentAgent = null;
    @GuardedBy("mLock") @NonNull private Map<String, ArrayList<Agent>> mAgents = new HashMap<>();

    /* package */ OsmpPersonsManager(@NonNull Context context, @NonNull Storage storage,
                                     @NonNull TerminalsManager terminalsManager,
                                     @NonNull RequestExecutor executor) {
        mStorage = storage;
        mContext = context.getApplicationContext();
        mTerminalsManager = terminalsManager;
        mExecutor = executor;

        Person[] persons = storage.getPersons();
        if (persons != null && persons.length > 0) {
            mPersonsList = new Person[persons.length];
            for (int index = 0; index < persons.length; ++index) {
                Person person = persons[index];
                mPersons.put(person.getLogin(), person);
                mPersonsList[index] = person;
            }
        }
        Agent[] agents = storage.getAgents();
        int agentsCount = 0;
        if (agents != null && agents.length > 0) {
            for (Agent agent : agents) {
                ArrayList<Agent> a = mAgents.get(agent.getPersonLogin());
                if (a == null) {
                    a = new ArrayList<>();
                    mAgents.put(agent.getPersonLogin(), a);
                    ++agentsCount;
                }
                a.add(agent);
            }
        }
        LOG.info("Initialized. Loaded: {} accounts, {} agents", mPersonsList != null ? mPersonsList.length : 0, agentsCount);
        notifyPersonsChanged();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
        lbm.registerReceiver(new TerminalsChangesReceiver(), new IntentFilter(TerminalsManager.ACTION_CHANGED));
    }

    @Override
    public boolean add(@NonNull Person person) {
        mLock.lock();
        try {
            if (mPersons.containsKey(person.getLogin())) {
                LOG.warn("Person with login '{}' already added. Skipped.", person.getLogin());
                return false;
            }
            mPersons.put(person.getLogin(), person);
            mPersonsList = null;
            LOG.info("Person '{}' was added", person.getLogin());
        } finally {
            mLock.unlock();
        }
        mStorage.storePerson(person);
        notifyPersonsChanged();
        return true;
    }

    @Override
    public void verify(@NonNull Person person) {
        OsmpRequest.Builder builder = new OsmpRequest.Builder(person);
        builder.getInterface(OsmpInterface.Persons).add(new GetPersonInfoCommand());
//        builder.getInterface(OsmpInterface.Agents).add(new GetAgentInfoCommand());
        builder.getInterface(OsmpInterface.Agents).add(new GetAgentsCommand());
        OsmpRequest request = builder.create();

        if (request != null) {
            mExecutor.execute(request, new VerificationResultReceiver(person.getLogin()));
            LOG.info("Verification requested for '{}'", person.getLogin());
        }
    }

    @Override
    @NonNull
    public Person[] getPersons() {
        mLock.lock();
        try {
            if (mPersonsList == null) {
                mPersonsList = mPersons.values().toArray(new Person[mPersons.size()]);
            }
        } finally {
            mLock.unlock();
        }
        return mPersonsList;
    }

    @Nullable
    @Override
    public Person getPerson(@NonNull String login) {
        mLock.lock();
        try {
            return mPersons.get(login);
        } finally {
            mLock.unlock();
        }
    }

    @Nullable
    @Override
    public Person getCurrentPerson() {
        mLock.lock();
        try {
            if (mCurrentPerson == null && !mPersons.isEmpty()) {
                mCurrentPerson = mPersons.firstEntry().getValue();
                mCurrentAgent = null;
                LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(mContext);
                lbm.sendBroadcast(new Intent(ACTION_CURRENT_PERSON_CHANGED));
                if (mCurrentPerson != null) {
                    LOG.info("Current person switched to '{}'", mCurrentPerson.getLogin());
                } else {
                    LOG.error("Current person record can't be updated");
                }
            }
            return mCurrentPerson;
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void setCurrentPerson(@NonNull String login) {
        boolean notify = false;
        mLock.lock();
        try {
            Person person = mPersons.get(login);
            if (person != null && person != mCurrentPerson) {
                mCurrentPerson = person;
                mCurrentAgent = null;
                notify = true;
            }
        } finally {
            mLock.unlock();
        }

        if (notify) {
            LOG.info("Current person switched to '{}'", login);
            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(mContext);
            lbm.sendBroadcast(new Intent(ACTION_CURRENT_PERSON_CHANGED));
        }
    }

    @Nullable
    @Override
    public Agent getCurrentAgent() {
        mLock.lock();
        try {
            if (mCurrentAgent == null) {
                Person person = getCurrentPerson();
                if (person != null) {
                    List<Agent> agents = mAgents.get(person.getLogin());
                    if (agents != null) {
                        for (Agent agent : agents) {
                            if (TextUtils.equals(agent.getId(), person.getAgentId())) {
                                mCurrentAgent = agent;
                                LocalBroadcastManager.getInstance(mContext)
                                        .sendBroadcast(new Intent(ACTION_CURRENT_AGENT_CHANGED));
                                LOG.info("Current agent was set to '{}' from person '{}'", agent.getName(), person.getLogin());
                                break;
                            }
                        }
                    }
                }
            }
            return mCurrentAgent;
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void setCurrentAgent(@NonNull String agentId) {
        Person currentPerson = getCurrentPerson();
        if (currentPerson == null) {
            LOG.error("Can't set current agent, due to current person is NULL");
            return;
        }

        boolean notify = false;
        mLock.lock();
        try {
            List<Agent> agents = mAgents.get(currentPerson.getLogin());
            if (agents != null) {
                for (Agent agent : agents) {
                    if (TextUtils.equals(agentId, agent.getId())) {
                        mCurrentAgent = agent;
                        notify = true;
                        LOG.info("Current agent was set to '{}' from person '{}'", agent.getName(), currentPerson.getLogin());
                        break;                    }
                }
            }
        } finally {
            mLock.unlock();
        }

        if (notify) {
            LocalBroadcastManager.getInstance(mContext)
                    .sendBroadcast(new Intent(ACTION_CURRENT_AGENT_CHANGED));
        } else {
            LOG.error("Can't find agent with id '{}' from person '{}'", agentId, currentPerson.getLogin());
        }
    }

    @Nullable
    @Override
    public Agent[] getAgents(@NonNull String login) {
        mLock.lock();
        try {
            List<Agent> agents = mAgents.get(login);
            return agents == null ? null : agents.toArray(new Agent[agents.size()]);
        } finally {
            mLock.unlock();
        }
    }

    private void notifyPersonsChanged() {
        LOG.info("Notify about persons states changes");
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(mContext);
        lbm.sendBroadcast(new Intent(ACTION_PERSONS_CHANGED));
    }

    private void notifyVerifyResult(boolean success, @Nullable Person person) {
        Intent intent = new Intent(ACTION_PERSON_VERIFIED);
        intent.putExtra(EXTRA_PERSON, person);
        intent.putExtra(EXTRA_STATE, success);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    @Nullable
    private Person updatePersonState(@NonNull String login, @NonNull Person.State state,
                                   @Nullable String agentId, @Nullable String name) {
        Person person = null;
        boolean notifyPersonChanged = false;
        mLock.lock();
        try {
            person = mPersons.get(login);
            if (person == null) {
                LOG.error("Can't update state for '{}'", login);
                return null;
            }
            final Person.State oldState = person.getState();
            person = person.cloneWithState(agentId, name, state);
            mPersons.put(person.getLogin(), person);
            mPersonsList = null;
            if (person.equals(mCurrentPerson)) {
                mCurrentPerson = person;
                notifyPersonChanged = true;
                LOG.info("Current person '{}' state changed: {} -> {}", login, oldState.name(), state.name());
            } else {
                LOG.info("Person '{}' state changed: {} -> {}", login, oldState.name(), state.name());
            }
        } finally {
            mLock.unlock();
        }
        mStorage.storePerson(person);
        notifyVerifyResult(state == Person.State.Valid, person);
        if (notifyPersonChanged) {
            notifyPersonsChanged();
        }
        return person;
    }

    private class VerificationResultReceiver extends ResultCallback {

        @NonNull
        private final String mLogin;

        private VerificationResultReceiver(@NonNull String login) {
            mLogin = login;
        }

        @Override
        public void onSuccess(@NonNull OsmpResponse response) {

            OsmpResponse.Results results = response.getInterface(OsmpInterface.Persons);
            if (response.getResult() != OsmpResponse.RESULT_OK || results == null) {
                LOG.error("An error occurred while verifying person '{}'. Result code: {}", mLogin, response.getResult());
                updatePersonState(mLogin, Person.State.Invalid, null, null);
                return;
            }

            GetPersonInfoResult info = results.get(GetPersonInfoCommand.NAME);
            final String name = info.getPersonName();
            final String agentId = info.getAgentId();

            if (name == null || agentId == null) {
                LOG.error("An error occurred while verifying person '{}'. Result code: {}", mLogin, response.getResult());
                updatePersonState(mLogin, Person.State.Invalid, null, null);
                return;
            }

            Person person = updatePersonState(mLogin, Person.State.Valid, agentId, name);
            if (person == null) {
                return;
            }

            results = response.getInterface(OsmpInterface.Agents);
            if (results == null) {
                LOG.error("Can't obtain agents list for person '{}'", mLogin);
                return;
            }

            ArrayList<Agent> agentsList = new ArrayList<>();
//            GetAgentInfoResult agentInfoResult = results.get(GetAgentInfoCommand.NAME);
//            if (agentInfoResult != null && agentInfoResult.getAgentId() != null
//                    && agentInfoResult.getAgentName() != null) {
//                agentsList.add(new Agent(agentInfoResult.getAgentId(), null,
//                        agentInfoResult.getAgentINN(), agentInfoResult.getAgentAddress(),
//                        agentInfoResult.getAgentAddress(), agentInfoResult.getAgentName(),
//                        null, null, null, null).cloneForPerson(person));
//            }

            GetAgentsResult agentsResult = results.get(GetAgentsCommand.NAME);
            if (agentsResult != null && agentsResult.getAgents() != null) {
                for (Agent agent : agentsResult.getAgents()) {
                    agentsList.add(agent.cloneForPerson(person));
                }
            }

            if (!agentsList.isEmpty()) {
                mLock.lock();
                try {
                    mAgents.put(person.getLogin(), agentsList);
                } finally {
                    mLock.unlock();
                }
                mStorage.storeAgents(agentsList.toArray(new Agent[agentsList.size()]));
                LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(mContext);
                lbm.sendBroadcast(new Intent(ACTION_AGENTS_CHANGED));
            }

            LOG.info("{} agents had been added to person {}", agentsList.size(), mLogin);
            mTerminalsManager.sync(person, false);
        }

        @Override
        public void onError() {
            LOG.error("Error while verifying account.");
            notifyVerifyResult(false, null);
        }
    }

    private class TerminalsChangesReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            LOG.info("Updating terminals");
            ObjectGraph graph = ((ApteryxApplication) context.getApplicationContext()).getGraph();
            TerminalsManager manager = graph.get(TerminalsManager.class);
            final Terminal[] terminals = manager.getTerminals(null);
            Map<String, Integer> counts = new HashMap<>();
            Map<String, Agent.State> states = new HashMap<>();
            for (Terminal terminal : terminals) {
                final String agentId = terminal.getAgentId();
                Integer count = counts.get(agentId);
                if (count != null) {
                    counts.put(agentId, ++count);
                } else {
                    counts.put(agentId, 1);
                }

                Agent.State state = states.get(agentId);
                TerminalState st = terminal.getState();
                if (st == null) {
                    continue;
                }
                if (st.hasErrors()) {
                    if (state != Agent.State.Error) {
                        states.put(agentId, Agent.State.Error);
                    }
                } else if (st.hasWarnings()) {
                    if (state != Agent.State.Error) {
                        states.put(agentId, Agent.State.Warn);
                    }
                } else if (state == null) {
                    states.put(agentId, Agent.State.Ok);
                }
            }
            List<Agent> changed = new ArrayList<>();
            mLock.lock();
            try {
                for (ArrayList<Agent> agents : mAgents.values()) {
                    for (int index = 0; index < agents.size(); ++index) {
                        final Agent agent = agents.get(index);
                        final Integer count = counts.get(agent.getId());
                        Agent.State state = states.get(agent.getId());
                        if (count == null) {
                            continue;
                        }
                        if (state == null) {
                            state = Agent.State.Ok;
                        }
                        final Agent.State oldState = agent.getState();
                        final int oldCount = agent.getTerminalsCount();
                        if (count != oldCount || state != oldState) {
                            Agent newAgent = agent.cloneForState(count, state);
                            agents.set(index, newAgent);
                            changed.add(newAgent);
                            LOG.info("Agent's '{}' state or terminals count had been changed. {} -> {}, {} -> {}",
                                    agent.getName(), oldCount, count, oldState.name(),  state.name());
                        }
                    }
                }
            } finally {
                mLock.unlock();
            }

            if (!changed.isEmpty()) {
                notifyPersonsChanged();
                mStorage.storeAgents(changed.toArray(new Agent[changed.size()]));
            }
        }
    }
}
