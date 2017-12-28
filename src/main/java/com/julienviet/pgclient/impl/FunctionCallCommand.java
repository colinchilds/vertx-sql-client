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

import com.julienviet.pgclient.PgException;
import com.julienviet.pgclient.Tuple;
import com.julienviet.pgclient.impl.codec.DataFormat;
import com.julienviet.pgclient.impl.codec.DataType;
import com.julienviet.pgclient.impl.codec.decoder.DecodeContext;
import com.julienviet.pgclient.impl.codec.decoder.InboundMessage;
import com.julienviet.pgclient.impl.codec.decoder.message.ErrorResponse;
import com.julienviet.pgclient.impl.codec.decoder.message.FunctionCallResponse;
import com.julienviet.pgclient.impl.codec.encoder.message.FunctionCall;
import io.vertx.core.Handler;

public class FunctionCallCommand<R> extends CommandBase<R> {

  private final int oid;
  private final DataType[] dataTypes;
  private final Tuple params;
  private final DataType<?> returnDataType;

  public FunctionCallCommand(long oid, DataType[] dataTypes, Tuple params,
                             DataType<R> returnDataType,
                             Handler<? super CommandResponse<R>> handler) {
    super(handler);
    this.oid = (int)oid;
    this.dataTypes = dataTypes;
    this.params = params;
    this.returnDataType = returnDataType;
  }

  @Override
  void exec(SocketConnection conn) {
    conn.decodeQueue.add(new DecodeContext(false, null, DataFormat.BINARY, returnDataType));
    conn.writeMessage(new FunctionCall(oid, params, dataTypes));
  }

  @Override
  public void handleMessage(InboundMessage msg) {
    if (msg instanceof FunctionCallResponse) {
      result = (R) ((FunctionCallResponse)msg).value();
    } else if (msg.getClass() == ErrorResponse.class) {
      ErrorResponse error = (ErrorResponse) msg;
      failure = new PgException(error);
    } else {
      super.handleMessage(msg);
    }
  }

  @Override
  void fail(Throwable err) {
    err.printStackTrace();
  }
}
