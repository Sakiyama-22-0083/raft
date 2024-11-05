package raft;

import raft.messages.Message;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

public class RaftWorker implements Runnable {

  public List<Node> nodes;
  public BlockingQueue<Message> queues;

  public RaftWorker() {
    nodes = new ArrayList<Node>();
  }

  public void createNewNode() {
    Node node = new Node(nodes.size() + 1);
    node.setNodeList(nodes);
    node.start();

    nodes.add(node);
  }

  public void run() {
  }

}
