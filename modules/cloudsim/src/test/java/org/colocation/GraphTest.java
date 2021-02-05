package org.colocation;

import org.cloudbus.cloudsim.Log;
import org.colocation.trace.RequestGraph;
import org.colocation.trace.RequestGraphNode;
import org.junit.Test;

/**
 * Created by wkj on 2019/7/28.
 */
public class GraphTest {
    @Test
    public void TestGraphSimplify() {
        RequestGraph graph = new RequestGraph();
        int hostID = 1;
        RequestGraphNode n1 = new RequestGraphNode("1",1.0, 1L,1,"svc1", hostID, 1);
        RequestGraphNode n2 = new RequestGraphNode("2",2.0, 1L,2,"svc1", hostID, 1);
        RequestGraphNode n3 = new RequestGraphNode("3",3.0, 1L,3,"svc1", hostID, 1);
        RequestGraphNode n4 = new RequestGraphNode("4",3.0, 1L,4,"svc1", hostID, 1);
        RequestGraphNode n5 = new RequestGraphNode("5",4.0, 2,5,"svc2", hostID, 2);
        RequestGraphNode n6 = new RequestGraphNode("6",4.0, 3,6, "svc3", hostID, 3);
        RequestGraphNode n7 = new RequestGraphNode("7",5.0, 2,7, "svc2", hostID, 2);
        RequestGraphNode n8 = new RequestGraphNode("8",5.0, 3,8,"svc3", hostID, 3);
        RequestGraphNode n9 = new RequestGraphNode("9",6.0, 2,9,"svc2", hostID, 2);
        RequestGraphNode n10 = new RequestGraphNode("10",6.0, 3,10,"svc3", hostID, 3);
        RequestGraphNode n11 = new RequestGraphNode("11",7.0, 1,11,"svc1", hostID, 1);
        RequestGraphNode n12 = new RequestGraphNode("12",7.0, 1,12,"svc1", hostID, 1);
        RequestGraphNode n13 = new RequestGraphNode("13",8.0, 1,13,"svc1", hostID, 1);
        RequestGraphNode n14 = new RequestGraphNode("14",9.0, 1,14,"svc1", hostID, 1);
        RequestGraphNode n15 = new RequestGraphNode("15",10.0, 1,15,"svc1", hostID, 1);

        graph.addVertex(n1);
        graph.addVertex(n2);
        graph.addVertex(n3);
        graph.addVertex(n4);
        graph.addVertex(n5);
        graph.addVertex(n6);
        graph.addVertex(n7);
        graph.addVertex(n8);
        graph.addVertex(n9);
        graph.addVertex(n10);
        graph.addVertex(n11);
        graph.addVertex(n12);
        graph.addVertex(n13);
        graph.addVertex(n14);
        graph.addVertex(n15);

        graph.addEdge(n1, n2);
        graph.addEdge(n2, n3);
        graph.addEdge(n2, n4);
        graph.addEdge(n3, n5);
        graph.addEdge(n4, n6);
        graph.addEdge(n5, n7);
        graph.addEdge(n6, n8);
        graph.addEdge(n8, n10);
        graph.addEdge(n7, n9);
        graph.addEdge(n9, n11);
        graph.addEdge(n10, n12);
        graph.addEdge(n11, n13);
        graph.addEdge(n12, n13);
        graph.addEdge(n13, n14);
        graph.addEdge(n14, n15);
        graph.setStartNode(n1);
        graph.simplify();

        Log.printLine(graph.toString());
    }
}
