/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.ethereum.eth.manager;

import tech.pegasys.pantheon.ethereum.eth.manager.EthPeer.DisconnectCallback;
import tech.pegasys.pantheon.ethereum.p2p.api.PeerConnection;
import tech.pegasys.pantheon.util.Subscribers;

import java.time.Clock;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EthPeers {
  public static final Comparator<EthPeer> TOTAL_DIFFICULTY =
      Comparator.comparing(
          ((final EthPeer p) -> p.chainState().getBestBlock().getTotalDifficulty()));

  public static final Comparator<EthPeer> CHAIN_HEIGHT =
      Comparator.comparing(((final EthPeer p) -> p.chainState().getEstimatedHeight()));

  public static final Comparator<EthPeer> BEST_CHAIN = TOTAL_DIFFICULTY.thenComparing(CHAIN_HEIGHT);

  private final int maxOutstandingRequests = 5;
  private final Map<PeerConnection, EthPeer> connections = new ConcurrentHashMap<>();
  private final String protocolName;
  private final Clock clock;
  private final Subscribers<ConnectCallback> connectCallbacks = new Subscribers<>();
  private final Subscribers<DisconnectCallback> disconnectCallbacks = new Subscribers<>();

  public EthPeers(final String protocolName, final Clock clock) {
    this.protocolName = protocolName;
    this.clock = clock;
  }

  void registerConnection(final PeerConnection peerConnection) {
    final EthPeer peer =
        new EthPeer(peerConnection, protocolName, this::invokeConnectionCallbacks, clock);
    connections.putIfAbsent(peerConnection, peer);
  }

  void registerDisconnect(final PeerConnection connection) {
    final EthPeer peer = connections.remove(connection);
    if (peer != null) {
      disconnectCallbacks.forEach(callback -> callback.onDisconnect(peer));
      peer.handleDisconnect();
    }
  }

  public EthPeer peer(final PeerConnection peerConnection) {
    return connections.get(peerConnection);
  }

  public long subscribeConnect(final ConnectCallback callback) {
    return connectCallbacks.subscribe(callback);
  }

  public void unsubscribeConnect(final long id) {
    connectCallbacks.unsubscribe(id);
  }

  public long subscribeDisconnect(final DisconnectCallback callback) {
    return disconnectCallbacks.subscribe(callback);
  }

  public int peerCount() {
    return connections.size();
  }

  public Stream<EthPeer> availablePeers() {
    return connections.values().stream().filter(EthPeer::readyForRequests);
  }

  public Optional<EthPeer> bestPeer() {
    return availablePeers().max(BEST_CHAIN);
  }

  private Stream<EthPeer> idlePeers() {
    return availablePeers()
        .filter(p -> p.outstandingRequests() < maxOutstandingRequests)
        .sorted(Comparator.comparing(EthPeer::getAverageResponseTime));
  }

  public Optional<EthPeer> idlePeer(final long withBlocksUpTo) {
    return idlePeers().filter(p -> p.chainState().getEstimatedHeight() >= withBlocksUpTo).findAny();
  }

  @FunctionalInterface
  public interface ConnectCallback {
    void onPeerConnected(EthPeer newPeer);
  }

  @Override
  public String toString() {
    final String connectionsList =
        connections.values().stream().map(EthPeer::toString).collect(Collectors.joining(","));
    return "EthPeers{connections=" + connectionsList + '}';
  }

  private void invokeConnectionCallbacks(final EthPeer peer) {
    connectCallbacks.forEach(cb -> cb.onPeerConnected(peer));
  }
}
