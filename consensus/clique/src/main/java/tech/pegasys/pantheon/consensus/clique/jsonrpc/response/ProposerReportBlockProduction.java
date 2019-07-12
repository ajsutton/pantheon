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

public class ProposerReportBlockProduction {

    private final String address;
    private long nbBlock;

    public ProposerReportBlockProduction(Address address) {
        this.address = address.toString();
        this.nbBlock = 1;
    }

    public void incrementeNbBlock() {
        this.nbBlock++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProposerReportBlockProduction that = (ProposerReportBlockProduction) o;
        return nbBlock == that.nbBlock &&
                Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nbBlock, address);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Proposer{");
        sb.append("address=").append(address).append(", ");
        sb.append("nbBlock=").append(nbBlock);
        return sb.append("}").toString();
    }
}
