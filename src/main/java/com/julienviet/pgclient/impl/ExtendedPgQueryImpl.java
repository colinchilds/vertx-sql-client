/*
 * Copyright (C) 2017 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.julienviet.pgclient.impl;

import com.julienviet.pgclient.PgQuery;
import com.julienviet.pgclient.PgResult;
import com.julienviet.pgclient.Row;
import com.julienviet.pgclient.Tuple;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.util.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ExtendedPgQueryImpl implements PgQuery {

  private final PgPreparedQueryImpl ps;
  private final Tuple params;
  private final int fetch;

  private String portal;
  private boolean completed;
  private boolean closed;
  private ExtendedQueryResultHandler result;

  ExtendedPgQueryImpl(PgPreparedQueryImpl ps, int fetch, Tuple params) {
    this.ps = ps;
    this.fetch = fetch;
    this.params = params;
  }

  @Override
  public boolean hasMore() {
    return result.isSuspended();
  }

  @Override
  public void execute(Handler<AsyncResult<PgResult<Row>>> handler) {
    if (result == null) {
      result = new ExtendedQueryResultHandler(handler);
      portal = fetch > 0 ? UUID.randomUUID().toString() : null;
      ps.execute(params, fetch, portal, false, result);
    } else if (result.isSuspended()) {
      result = new ExtendedQueryResultHandler(handler);
      ps.execute(params, fetch, portal, true, result);
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public void close(Handler<AsyncResult<Void>> completionHandler) {
    if (!closed) {
      closed = true;
      if (portal == null) {
        // Nothing to do
        completionHandler.handle(Future.succeededFuture());
      } else {
        if (!completed) {
          completed = true;
          ps.closePortal(portal, completionHandler);
        }
      }
    }
  }
}
