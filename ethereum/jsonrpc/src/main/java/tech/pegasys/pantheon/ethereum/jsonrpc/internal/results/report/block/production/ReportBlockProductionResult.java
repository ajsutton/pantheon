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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.report.block.production;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"proposers", "validatorsPresentInLastBlock"})
public class ReportBlockProductionResult {

  private final List<ProposerReportBlockProductionResult> proposers;

  private final List<ValidatorReportBlockProductionResult> validatorsPresentInLastBlock;

  public ReportBlockProductionResult(
      final List<ProposerReportBlockProductionResult> proposerReportBlockProductions,
      final List<ValidatorReportBlockProductionResult> validatorReportBlockProductions) {
    this.proposers = proposerReportBlockProductions;
    this.validatorsPresentInLastBlock = validatorReportBlockProductions;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ReportBlockProductionResult that = (ReportBlockProductionResult) o;
    return Objects.equals(proposers, that.proposers)
        && Objects.equals(validatorsPresentInLastBlock, that.validatorsPresentInLastBlock);
  }

  @Override
  public int hashCode() {
    return Objects.hash(proposers, validatorsPresentInLastBlock);
  }

  @JsonGetter(value = "proposers")
  public List<ProposerReportBlockProductionResult> getProposers() {
    return proposers;
  }

  @JsonGetter(value = "validatorsPresentInLastBlock")
  public List<ValidatorReportBlockProductionResult> getValidatorsPresentInLastBlock() {
    return validatorsPresentInLastBlock;
  }
}
