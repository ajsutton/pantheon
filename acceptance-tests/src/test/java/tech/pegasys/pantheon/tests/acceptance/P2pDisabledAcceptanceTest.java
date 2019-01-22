/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.pantheon.tests.acceptance;

import tech.pegasys.pantheon.tests.acceptance.dsl.AcceptanceTestBase;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.Node;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.cluster.Cluster;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.cluster.ClusterConfiguration;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.cluster.ClusterConfigurationBuilder;

import org.junit.Before;
import org.junit.Test;

public class P2pDisabledAcceptanceTest extends AcceptanceTestBase {
  private Node node;
  private Cluster p2pDisabledCluster;

  @Before
  public void setUp() throws Exception {
    final ClusterConfiguration clusterConfiguration =
        new ClusterConfigurationBuilder().setAwaitPeerDiscovery(false).build();

    p2pDisabledCluster = new Cluster(clusterConfiguration, net);
    node = pantheon.createArchiveNodeWithP2pDisabled("node1");
    p2pDisabledCluster.start(node);
  }

  @Override
  public void tearDownAcceptanceTestBase() {
    p2pDisabledCluster.stop();
    super.tearDownAcceptanceTestBase();
  }

  @Test
  public void shouldSucceedExecutingUnaffectedJsonRpcCall() {
    final String input = "0x68656c6c6f20776f726c64";
    final String expectedHash =
        "0x47173285a8d7341e5e972fc677286384f802f8ef42a5ec5f03bbfa254cb01fad";

    node.verify(web3.sha3(input, expectedHash));
  }

  @Test
  public void shouldFailExecutingAffectedJsonRpcCall() {
    node.verify(net.awaitPeerCountExceptional());
  }
}
