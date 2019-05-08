package tech.pegasys.pantheon.ethereum.trie;

import java.util.function.Consumer;

public class AllNodesVisitor<V> implements NodeVisitor<V> {

  private final Consumer<Node<V>> handler;

  public AllNodesVisitor(final Consumer<Node<V>> handler) {
    this.handler = handler;
  }

  @Override
  public void visit(final ExtensionNode<V> extensionNode) {
    handler.accept(extensionNode);
    extensionNode.getChild().accept(this);
  }

  @Override
  public void visit(final BranchNode<V> branchNode) {
    handler.accept(branchNode);
    for (byte i = 0; i < BranchNode.RADIX; i++) {
      branchNode.child(i).accept(this);
    }
  }

  @Override
  public void visit(final LeafNode<V> leafNode) {
    handler.accept(leafNode);
  }

  @Override
  public void visit(final NullNode<V> nullNode) {
  }
}
