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
package tech.pegasys.pantheon.consensus.clique.jsonrpc.response;

import java.util.List;
import java.util.Objects;

public class GetReportValidatorBlockProductionResponse {

    private final List<ProposerReportBlockProduction> proposers;

    private final List<ValidatorReportBlockProduction> validatorsPresentInLastBlock;

    public GetReportValidatorBlockProductionResponse(
            List<ProposerReportBlockProduction> proposerReportBlockProductions,
            List<ValidatorReportBlockProduction> validatorReportBlockProductions) {
        this.proposers = proposerReportBlockProductions;
        this.validatorsPresentInLastBlock = validatorReportBlockProductions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetReportValidatorBlockProductionResponse that = (GetReportValidatorBlockProductionResponse) o;
        return Objects.equals(proposers, that.proposers) &&
                Objects.equals(validatorsPresentInLastBlock, that.validatorsPresentInLastBlock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proposers, validatorsPresentInLastBlock);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ReportBlockProduction{");
        sb.append("proposers=").append(proposers).append(", ");
        sb.append("validatorsPresentInLastBlock=").append(validatorsPresentInLastBlock);
        return sb.append("}").toString();
    }
}
