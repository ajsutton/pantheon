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
package tech.pegasys.pantheon.consensus.common;

import tech.pegasys.pantheon.ethereum.chain.BlockAddedEvent;
import tech.pegasys.pantheon.ethereum.chain.BlockAddedObserver;
import tech.pegasys.pantheon.ethereum.chain.Blockchain;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.metrics.MetricCategory;
import tech.pegasys.pantheon.metrics.MetricsSystem;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

public class PoAValidatorMetrics implements BlockAddedObserver {

  private static final int RECENT_BLOCK_COUNT = 100;
  private final Deque<Address> recentProposers = new LinkedBlockingDeque<>();
  private final BlockInterface blockInterface;

  public PoAValidatorMetrics(
      final MetricsSystem metricsSystem,
      final VoteTallyCache voteTallyCache,
      final BlockInterface blockInterface) {
    this.blockInterface = blockInterface;

    metricsSystem.createLongGauge(
        MetricCategory.VALIDATORS,
        "recent_proposer_count",
        "Number of unique proposers in the last 100 imported blocks",
        () -> recentProposers.stream().distinct().count());

    metricsSystem.createIntegerGauge(
        MetricCategory.VALIDATORS,
        "validator_count_current",
        "Current number of validators",
        () -> voteTallyCache.getVoteTallyAtHead().getValidators().size());
  }

  @Override
  public void onBlockAdded(final BlockAddedEvent event, final Blockchain blockchain) {
    final Address proposerAddress = blockInterface.getProposerOfBlock(event.getBlock().getHeader());
    recentProposers.addLast(proposerAddress);
    if (recentProposers.size() > RECENT_BLOCK_COUNT) {
      recentProposers.removeFirst();
    }
  }
}
