package raft;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;

public class Main {
  private static String logFilePath = "log/output.txt";

  public static void main(String[] args) {
    resetFile(logFilePath);
    RaftWorker worker = new RaftWorker(logFilePath);
    for (int i = 0; i < 3; i++) {
      worker.createNewNode();
    }
    worker.run();
    // TODO: 一定時間後にworkerクラスのノード停止メソッドを呼び出す
  }

  /**
   * csvファイルの内容をリセットするメソッド
   *
   * @param file
   */
  private static void resetFile(String filePath) {
    File file = new File(filePath);
    File parentDir = file.getParentFile();
    // logディレクトリが存在しない場合，新たにディレクトリを作成する．
    if (parentDir != null && !parentDir.exists()) {
      parentDir.mkdirs(); // ディレクトリを再帰的に作成
    }
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
      writer.write("");
    } catch (IOException e) {
      System.err.println("エラーが発生しました: " + e.getMessage());
    }
  }

}
