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
package tech.pegasys.pantheon.ethereum.trie;

import static tech.pegasys.pantheon.crypto.Hash.keccak256;

import tech.pegasys.pantheon.ethereum.rlp.BytesValueRLPOutput;
import tech.pegasys.pantheon.ethereum.rlp.RLP;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

class LeafNode<V> implements Node<V> {
  private final BytesValue path;
  private final V value;
  private final NodeFactory<V> nodeFactory;
  private final Function<V, BytesValue> valueSerializer;
  private Supplier<BytesValue> rlp;
  private Supplier<Bytes32> hash;
  private boolean dirty = false;

  LeafNode(
      final BytesValue path,
      final V value,
      final NodeFactory<V> nodeFactory,
      final Function<V, BytesValue> valueSerializer,
      final Optional<BytesValue> rlp) {
    this.path = path;
    this.value = value;
    this.nodeFactory = nodeFactory;
    this.valueSerializer = valueSerializer;
    this.rlp = Suppliers.memoize(() -> rlp.orElseGet(this::writeRlp));
    this.hash = Suppliers.memoize(() -> keccak256(getRlp()));
  }

  @Override
  public Node<V> accept(final PathNodeVisitor<V> visitor, final BytesValue path) {
    return visitor.visit(this, path);
  }

  @Override
  public void accept(final NodeVisitor<V> visitor) {
    visitor.visit(this);
  }

  @Override
  public BytesValue getPath() {
    return path;
  }

  @Override
  public Optional<V> getValue() {
    return Optional.of(value);
  }

  @Override
  public Optional<List<Node<V>>> getChildren() {
    return Optional.empty();
  }

  @Override
  public BytesValue getRlp() {
    return rlp.get();
  }

  private BytesValue writeRlp() {
    final BytesValueRLPOutput out = new BytesValueRLPOutput();
    out.startList();
    out.writeBytesValue(CompactEncoding.encode(path));
    out.writeBytesValue(valueSerializer.apply(value));
    out.endList();
    return out.encoded();
  }

  @Override
  public BytesValue getRlpRef() {
    if (isReferencedByHash()) {
      return RLP.encodeOne(getHash());
    } else {
      return getRlp();
    }
  }

  @Override
  public Bytes32 getHash() {
    return hash.get();
  }

  @Override
  public Node<V> replacePath(final BytesValue path) {
    return nodeFactory.createLeaf(path, value);
  }

  @Override
  public String print() {
    return "Leaf:"
        + "\n\tRef: "
        + getRlpRef()
        + "\n\tPath: "
        + CompactEncoding.encode(path)
        + "\n\tValue: "
        + getValue().map(Object::toString).orElse("empty");
  }

  @Override
  public boolean isDirty() {
    return dirty;
  }

  @Override
  public void markDirty() {
    dirty = true;
  }
}
