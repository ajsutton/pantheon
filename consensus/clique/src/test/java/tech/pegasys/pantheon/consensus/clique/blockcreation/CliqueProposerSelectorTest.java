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
package tech.pegasys.pantheon.consensus.clique.blockcreation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import tech.pegasys.pantheon.consensus.clique.VoteTallyCache;
import tech.pegasys.pantheon.consensus.common.VoteTally;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.AddressHelpers;
import tech.pegasys.pantheon.ethereum.core.BlockHeaderTestFixture;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class CliqueProposerSelectorTest {

  private final List<Address> validatorList =
      Arrays.asList(
          AddressHelpers.ofValue(1),
          AddressHelpers.ofValue(2),
          AddressHelpers.ofValue(3),
          AddressHelpers.ofValue(4));
  private final VoteTally voteTally = new VoteTally(validatorList);
  private VoteTallyCache voteTallyCache;

  @Before
  public void setup() {
    voteTallyCache = mock(VoteTallyCache.class);
    when(voteTallyCache.getVoteTallyAfterBlock(any())).thenReturn(voteTally);
  }

  @Test
  public void proposerForABlockIsBasedOnModBlockNumber() {
    final BlockHeaderTestFixture headerBuilderFixture = new BlockHeaderTestFixture();

    for (int prevBlockNumber = 0; prevBlockNumber < 10; prevBlockNumber++) {
      headerBuilderFixture.number(prevBlockNumber);
      final CliqueProposerSelector selector = new CliqueProposerSelector(voteTallyCache);
      final Address nextProposer =
          selector.selectProposerForNextBlock(headerBuilderFixture.buildHeader());
      assertThat(nextProposer)
          .isEqualTo(validatorList.get((prevBlockNumber + 1) % validatorList.size()));
    }
  }
}
