import java.io.*;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;

public class Check {
  final static int N_NODES = 2; // initial nodes

  public static void main(String[] args) {
    String fileName = args[0];
    String line;
    SortedMap<Integer, Integer> numNodesInView = new TreeMap<>();
    SortedMap<Integer, Integer> numSentInView = new TreeMap<>();
    SortedMap<Integer, Integer> numRecvInView = new TreeMap<>();

    //vsmanager view 1 of 3 nodes
    //vsnodeG1 multicasts 63 in view 6 to 3 nodes
    //vsnodeJ1 delivers 63 from vsnodeG1 in view 6

    try {
      BufferedReader reader =
        new BufferedReader(new FileReader(fileName));

      while((line = reader.readLine()) != null) {
        String[] l = line.split(" ");
        System.out.println(Arrays.toString(l));
        if(l.length < 2) continue;
        if (l[1].equals("view")) {
          int viewId = Integer.parseInt(l[2]);
          int numNodes = Integer.parseInt(l[4]);

          numNodesInView.put(viewId, numNodes);
        }
        else if (l[1].equals("multicasts")) {
          int viewId = Integer.parseInt(l[5]);
          int numSent = Integer.parseInt(l[7]);
          //int seqnoSent = Integer.parseInt(l[2]);

          numSentInView.compute(viewId, (Integer k, Integer v) -> {
            if (v==null) return numSent;
            else return v + numSent;
          });
          System.out.println(numSentInView);
        }
        else if (l[1].equals("delivers")) {
          int viewId = Integer.parseInt(l[7]);
          //int seqnoRecv = Integer.parseInt(l[2]);

          numRecvInView.compute(viewId, (Integer k, Integer v) -> {
            if (v==null) return 1;
            else return v + 1;
          });
          System.out.println(numRecvInView);
        }
      }
      for (int x : numSentInView.keySet()) {
        int numNodes = numNodesInView.getOrDefault(x, N_NODES);
        int numSent = numSentInView.get(x);
        int numRecv = numRecvInView.getOrDefault(x, 0);
        
        System.out.println(
                "View: " + x
                + " Nodes: " + numNodes
                + " Sent: " + numSent
                + " Recv: " + numRecv
                + " Synch: " + (numSent == numRecv)
        );
      }
      reader.close();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }
}
