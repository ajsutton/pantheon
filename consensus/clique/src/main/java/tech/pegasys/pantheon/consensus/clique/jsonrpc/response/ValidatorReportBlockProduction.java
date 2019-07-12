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

import tech.pegasys.pantheon.ethereum.core.Address;

import java.util.Objects;

public class ValidatorReportBlockProduction {

    private final String address;
    private Long lastBlock;

    public ValidatorReportBlockProduction(Address address) {
        this.address = address.toString();
    }

    public void setLastBlock(Long lastBlock) {
        this.lastBlock = lastBlock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidatorReportBlockProduction that = (ValidatorReportBlockProduction) o;
        return Objects.equals(lastBlock, that.lastBlock) &&
                address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastBlock, address);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Validator{");
        sb.append("address=").append(address);
        if(lastBlock!=null) {
            sb.append(", ").append("lastBlock=").append(lastBlock);
        }
        return sb.append("}").toString();
    }
}
