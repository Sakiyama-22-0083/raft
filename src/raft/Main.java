package raft;

public class Main {

  public static void main(String[] args) {
    RaftWorker worker = new RaftWorker();
    for (int i = 0; i < 3; i++) {
      worker.createNewNode();
    }
    worker.run();
  }

}
