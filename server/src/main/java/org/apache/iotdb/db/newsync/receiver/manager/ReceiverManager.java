/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.db.newsync.receiver.manager;

import org.apache.iotdb.db.exception.StartupException;
import org.apache.iotdb.db.newsync.receiver.recovery.ReceiverLog;
import org.apache.iotdb.db.newsync.receiver.recovery.ReceiverLogAnalyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class ReceiverManager {

  private static final Logger logger = LoggerFactory.getLogger(ReceiverManager.class);

  private boolean pipeServerEnable;
  // <pipeName, <remoteIp, pipeInfo>>
  private Map<String, Map<String, PipeInfo>> pipeInfoMap;
  private ReceiverLog log;

  public void init() throws StartupException {
    ReceiverLogAnalyzer.scan();
    pipeInfoMap = ReceiverLogAnalyzer.getPipeInfoMap();
    pipeServerEnable = ReceiverLogAnalyzer.isPipeServerEnable();
    log = new ReceiverLog();
  }

  public void close() throws IOException {
    log.close();
  }

  public void startServer() throws IOException {
    log.startPipeServer();
    pipeServerEnable = true;
  }

  public void stopServer() throws IOException {
    log.stopPipeServer();
    pipeServerEnable = false;
  }

  public void createPipe(String pipeName, String remoteIp, long createTime) throws IOException {
    log.createPipe(pipeName, remoteIp, createTime);
    if (!pipeInfoMap.containsKey(pipeName)) {
      pipeInfoMap.put(pipeName, new HashMap<>());
    }
    pipeInfoMap
        .get(pipeName)
        .put(remoteIp, new PipeInfo(pipeName, remoteIp, PipeStatus.RUNNING, createTime));
  }

  public void startPipe(String pipeName, String remoteIp) throws IOException {
    log.startPipe(pipeName, remoteIp);
    pipeInfoMap.get(pipeName).get(remoteIp).setStatus(PipeStatus.RUNNING);
  }

  public void stopPipe(String pipeName, String remoteIp) throws IOException {
    log.stopPipe(pipeName, remoteIp);
    pipeInfoMap.get(pipeName).get(remoteIp).setStatus(PipeStatus.PAUSE);
  }

  public void dropPipe(String pipeName, String remoteIp) throws IOException {
    log.dropPipe(pipeName, remoteIp);
    pipeInfoMap.get(pipeName).get(remoteIp).setStatus(PipeStatus.DROP);
  }

  public List<PipeInfo> getPipeInfos(String pipeName) {
    return new ArrayList<>(pipeInfoMap.get(pipeName).values());
  }

  public List<PipeInfo> getAllPipeInfos() {
    List<PipeInfo> res = new ArrayList<>();
    for (String pipeName : pipeInfoMap.keySet()) {
      res.addAll(pipeInfoMap.get(pipeName).values());
    }
    return res;
  }

  public boolean isPipeServerEnable() {
    return pipeServerEnable;
  }

  public void setPipeServerEnable(boolean pipeServerEnable) {
    this.pipeServerEnable = pipeServerEnable;
  }

  public static ReceiverManager getInstance() {
    return ReceiverMonitorHolder.INSTANCE;
  }

  private ReceiverManager() {}

  private static class ReceiverMonitorHolder {
    private static final ReceiverManager INSTANCE = new ReceiverManager();

    private ReceiverMonitorHolder() {}
  }
}
