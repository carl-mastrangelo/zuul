/*
 * Copyright 2018 Netflix, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package com.netflix.zuul.origins;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.ExecutionContext;
import com.netflix.spectator.api.Registry;
import com.netflix.zuul.context.SessionContext;
import com.netflix.zuul.message.http.HttpRequestMessage;
import com.netflix.zuul.message.http.HttpResponseMessage;
import com.netflix.zuul.netty.connectionpool.PooledConnection;
import com.netflix.zuul.niws.RequestAttempt;
import com.netflix.zuul.passport.CurrentPassport;
import com.netflix.zuul.stats.Timing;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Promise;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Netty Origin interface for integrating cleanly with the ProxyEndpoint state management class.
 *
 * Author: Arthur Gonigberg
 * Date: November 29, 2017
 */
public interface NettyOrigin extends InstrumentedOrigin {

    /**
     * Use {@link #connectToOrigin(HttpRequestMessage, EventLoop, ChannelFactory, int, CurrentPassport, AtomicReference,
     * AtomicReference)} instead.  Zuul will not call this method.  Users are expected to implement one of the
     * {@code connectToOrigin} methods.
     */
    @Deprecated
    default Promise<PooledConnection> connectToOrigin(final HttpRequestMessage zuulReq, EventLoop eventLoop,
                                              int attemptNumber, CurrentPassport passport,
                                              AtomicReference<Server> chosenServer,
                                              AtomicReference<String> chosenHostAddr) {
        throw new UnsupportedOperationException("unimplemented");
    }

    default Promise<PooledConnection> connectToOrigin(
            final HttpRequestMessage zuulReq, EventLoop eventLoop, ChannelFactory<?> channelFactory, int attemptNumber,
            CurrentPassport passport, AtomicReference<Server> chosenServer, AtomicReference<String> chosenHostAddr) {
        // TODO(carl-mastrangelo): The generics on this method should be ? super String and so on.
        return connectToOrigin(
                zuulReq, eventLoop, attemptNumber, passport, chosenServer, chosenHostAddr);
    }

    Timing getProxyTiming(HttpRequestMessage zuulReq);

    int getMaxRetriesForRequest(SessionContext context);

    void onRequestExecutionStart(final HttpRequestMessage zuulReq);

    void onRequestStartWithServer(final HttpRequestMessage zuulReq, final Server originServer, int attemptNum);

    void onRequestExceptionWithServer(final HttpRequestMessage zuulReq, final Server originServer,
                                      final int attemptNum, Throwable t);

    void onRequestExecutionSuccess(final HttpRequestMessage zuulReq, final HttpResponseMessage zuulResp,
                                   final Server originServer, final int attemptNum);

    void onRequestExecutionFailed(final HttpRequestMessage zuulReq, final Server originServer,
                                  final int attemptNum, Throwable t);

    void recordFinalError(final HttpRequestMessage requestMsg, final Throwable throwable);

    void recordFinalResponse(final HttpResponseMessage resp);

    RequestAttempt newRequestAttempt(final Server server, final SessionContext zuulCtx, int attemptNum);

    String getIpAddrFromServer(Server server);

    IClientConfig getClientConfig();

    Registry getSpectatorRegistry();

    ExecutionContext<?> getExecutionContext(HttpRequestMessage zuulRequest);
}
