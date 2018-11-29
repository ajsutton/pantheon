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
package tech.pegasys.pantheon.tests.acceptance.dsl.condition.clique;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.pantheon.tests.acceptance.dsl.WaitUtils.waitFor;

import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.tests.acceptance.dsl.condition.Condition;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.Node;
import tech.pegasys.pantheon.tests.acceptance.dsl.transaction.clique.CliqueTransactions;

import java.util.Map;

public class ExpectProposals implements Condition {
  private final CliqueTransactions clique;
  private final Map<Address, Boolean> proposers;

  public ExpectProposals(final CliqueTransactions clique, final Map<Address, Boolean> proposers) {
    this.clique = clique;
    this.proposers = proposers;
  }

  @Override
  public void verify(final Node node) {
    waitFor(() -> assertThat(node.execute(clique.createProposals())).isEqualTo(proposers));
  }
}
