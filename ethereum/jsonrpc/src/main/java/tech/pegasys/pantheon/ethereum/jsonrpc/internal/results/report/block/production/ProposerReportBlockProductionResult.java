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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import tech.pegasys.pantheon.ethereum.core.Address;

import java.util.Objects;

@JsonPropertyOrder({"address", "nbBlockProposed"})
public class ProposerReportBlockProductionResult {

    public final String address;
    public long nbBlockProposed;

    public ProposerReportBlockProductionResult(final Address address) {
        this.address = address.toString();
        this.nbBlockProposed = 1;
    }

    public void incrementeNbBlock() {
        this.nbBlockProposed++;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProposerReportBlockProductionResult that = (ProposerReportBlockProductionResult) o;
        return nbBlockProposed == that.nbBlockProposed &&
                Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nbBlockProposed, address);
    }

    @JsonGetter(value = "address")
    public String getAddress() {
        return address;
    }

    @JsonGetter(value = "nbBlockProposed")
    public long getNbBlockProposed() {
        return nbBlockProposed;
    }
}
