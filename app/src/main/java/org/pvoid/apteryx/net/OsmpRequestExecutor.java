/*
 * Copyright (C) 2010-2014  Dmitry "PVOID" Petuhov
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

package org.pvoid.apteryx.net;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/* package */ class OsmpRequestExecutor implements RequestExecutor, RequestScheduler {

    private static final int REQUEST_DELAY_SEC = 3;

    private final Executor mExecutor;
    private final ScheduledExecutorService mDelayedExecutor;
    private final ResultFactories mFactories;

    /* package */ OsmpRequestExecutor(ResultFactories factories) {
        mFactories = factories;
        mExecutor = new ThreadPoolExecutor(1, 1, 15, TimeUnit.MINUTES,
                new LinkedBlockingQueue<Runnable>());
        mDelayedExecutor = new ScheduledThreadPoolExecutor(1);

    }

    @Nullable
    @Override
    public RequestHandle execute(@NonNull OsmpRequest request, @NonNull ResultReceiver receiver) {
        ResultHandler handler = new ResultHandler(receiver);
        RequestWork work = new RequestWork(this, request, mFactories, handler);
        mExecutor.execute(work);
        return handler;
    }

    @Override
    public void schedule(@NonNull RequestWork work) {
        mDelayedExecutor.schedule(work, REQUEST_DELAY_SEC, TimeUnit.SECONDS);
    }
}