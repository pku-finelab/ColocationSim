package org.colocation.trace;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.colocation.ServiceEntity;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by wkj on 2019/7/16.
 */
public class RequestGraph  {
    private DirectedAcyclicGraph graph;
    private int graphTopologyCode;
    private RequestGraphNode root;
    private HashMap<String, RequestGraphNode> vexes;

    private ArrayList<RequestPath> paths;

    public RequestGraph() {
        DirectedAcyclicGraph<RequestGraphNode, RequestGraphEdge> directedGraph =
                new DirectedAcyclicGraph<RequestGraphNode, RequestGraphEdge>(RequestGraphEdge.class);
        this.graph = directedGraph;
        this.graphTopologyCode = -1;
        this.root = null;
        this.vexes = new HashMap<>();
        this.paths = new ArrayList<>();
    }
    public int getToplogyId(){
        if (this.graphTopologyCode == -1) {
            Set<RequestGraphNode> nodes = graph.vertexSet();
            List<RequestGraphNode> nodeList = nodes.stream().collect(Collectors.toList());
            Collections.sort(nodeList, (o1, o2) -> o1.toString().compareTo(o2.toString()));
            StringBuilder topoSb = new StringBuilder();
            for (int i = 0; i < nodeList.size(); i++) {
                String iNode = nodeList.get(i).getSvcName();
                topoSb.append(iNode);
            }

            Set<RequestGraphEdge> edges = graph.edgeSet();
            List<RequestGraphEdge> edgeList = edges.stream().collect(Collectors.toList());
            Collections.sort(edgeList, (o1, o2) -> o1.toString().compareTo(o2.toString()));
            for (int i = 0; i < edgeList.size(); i++) {
                RequestGraphNode source = (RequestGraphNode)edgeList.get(i).getSource();
                String srcName = source.getSvcName();
                RequestGraphNode end = (RequestGraphNode)edgeList.get(i).getTarget();
                String endName = source.getSvcName();
                String edgeName = srcName+"-"+endName;
                topoSb.append(edgeName);
            }
            String graphString = topoSb.toString();
            int hashCode = graphString.hashCode();
            this.graphTopologyCode = hashCode;
        }
        return this.graphTopologyCode;
    }

    public void addVertex(RequestGraphNode vex){
        this.graph.addVertex(vex);
        this.vexes.put(vex.toString(), vex);
    }

    public RequestGraphNode getVex(String id) {
        return vexes.get(id);
    }

    public void setStartNode(RequestGraphNode vex) {
        this.root = vex;
    }

    public void mergeGraph(RequestGraph newGraph){
        for(String vexKey : newGraph.vexes.keySet()) {
            RequestGraphNode nodeOld = this.vexes.get(vexKey);
            RequestGraphNode nodeNew = newGraph.vexes.get(vexKey);
            nodeOld.mergeNewNode(nodeNew);
        }
    }

    public Set<RequestGraphNode> getChild(RequestGraphNode vex) {
        Set<RequestGraphNode> res = new HashSet<RequestGraphNode>();
        Set<RequestGraphEdge> outEdges = graph.outgoingEdgesOf(vex);
        for (RequestGraphEdge edge : outEdges) {
            RequestGraphNode c = (RequestGraphNode) edge.getTarget();
            res.add(c);
        }
        return  res;
    }

    public Set<RequestGraphNode> getParents(RequestGraphNode vex) {
        Set<RequestGraphNode> res = new HashSet<RequestGraphNode>();
        Set<RequestGraphEdge> outEdges = graph.incomingEdgesOf(vex);
        for (RequestGraphEdge edge : outEdges) {
            RequestGraphNode c = (RequestGraphNode) edge.getSource();
            res.add(c);
        }
        return  res;
    }

    public void simplify(){
        // rule: if a vertex (with 1 indgree) && (parent with 1 outdegree) && (has the same netcomID with parent vertex)


        Set<String> visitedNode = new HashSet<>();
        Queue<RequestGraphNode> queue = new LinkedList<>();
        queue.add(root);
        while (queue.size() >0 ) {
            RequestGraphNode currVertex = queue.poll();
            if (visitedNode.contains(currVertex.toString())){
                continue;
            }
            visitedNode.add(currVertex.toString());
            //Log.printLine("visiting: "+currVertex.toString());
            Set<RequestGraphNode> children = this.getChild(currVertex);
            for (RequestGraphNode child : children) {
                //Log.printLine("ADD, Node has child #"+ currVertex+ "-> #"+child);
                queue.add(child);
            }
            //get parent
            Set<RequestGraphNode> parents = this.getParents(currVertex);

            for (RequestGraphNode parent: parents) {
                Set<RequestGraphNode> newChildren = this.getChild(currVertex);

                Set<RequestGraphNode> newParents = this.getParents(currVertex);
                if (currVertex.getNetcomID() == parent.getNetcomID()) {
                    //merge this vex to parent
                    parent.mergeEvent(currVertex);
                    //Log.printLine("remove node: " +currVertex);
                    graph.removeVertex(currVertex);
                    vexes.remove(currVertex.toString());
                    for (RequestGraphNode child : newChildren) {
                        //Log.printLine("build edge: "+ parent +" -> "+child);
                        graph.addEdge(parent, child);
                    }

                    for (RequestGraphNode p : newParents) {
                        if (p != parent ) {
                            //Log.printLine("dealing vertex:"+currVertex.toString());
                            //Log.printLine("build edge: "+ p+" -> "+parent);
                            graph.addEdge(p, parent);
                        }
                    }

                    currVertex = parent;
                } else {
                    //cannot merge, modify event to
                    currVertex.mergeEvent(null);
                }
            }

        }

        //  toplogy sort for graph and reID everynode,
        visitedNode.clear();
        queue.clear();
        queue.add(root);
        HashMap<String, Integer> stageCount = new HashMap<>();
        while (queue.size() >0 ) {
            RequestGraphNode currVertex = queue.poll();
            if (visitedNode.contains(currVertex.toString())){
                continue;
            }
            Set<RequestGraphNode> children = this.getChild(currVertex);
            for (RequestGraphNode child : children) {
                //Log.printLine("ADD, Node has child #"+ currVertex+ "-> #"+child);
                queue.add(child);
            }

            visitedNode.add(currVertex.toString());
            String serviceName = currVertex.getSvcName();
            int count = 0;
            if (stageCount.containsKey(serviceName)){
                count = stageCount.get(serviceName);
            }
            stageCount.put(serviceName, count+1);
            String newID = serviceName+"_"+count;
            String oldID = currVertex.getId();
            currVertex.setId(newID);
            vexes.remove(oldID);
            vexes.put(newID, currVertex);
        }

    }

    public void generatePath(){
        RequestPath initPath = new RequestPath(root);
        this.paths.add(initPath);
        while ( true ) {
            boolean allPathEnd = true;
            for (int i = 0; i < this.paths.size(); i++) {
                boolean pathiEnd = false;
                RequestPath path = this.paths.get(i);
                RequestGraphNode lastNode = path.getLastOne();
                Set<RequestGraphEdge> outgoingEdges = this.graph.outgoingEdgesOf(lastNode);
                ArrayList<RequestGraphNode> outNodes = new ArrayList<>();
                for (RequestGraphEdge edge: outgoingEdges) {
                    outNodes.add((RequestGraphNode)edge.getTarget());
                }
                if (outNodes.size() == 0 ){
                    pathiEnd = true;
                } else {
                    // add every succeed to path
                    for (int j = 1; j < outNodes.size(); j++) {
                        RequestGraphNode next = outNodes.get(j);
                        RequestPath newPath = path.deepAdd(next);
                        this.paths.add(newPath);
                    }
                    RequestGraphNode next = outNodes.get(0);
                    // add to current path
                    path.add(next);
                }
                allPathEnd = allPathEnd && pathiEnd;
            }
            if (allPathEnd) {
                break;
            }
        }
    }

    public void generatePath2(){
        RequestPath initPath = new RequestPath(root);
        this.paths.add(initPath);
        while ( true ) {
            boolean allPathEnd = true;
            for (int i = 0; i < this.paths.size(); i++) {
                boolean pathiEnd = false;
                RequestPath path = this.paths.get(i);
                RequestGraphNode lastNode = path.getLastOne();
                Set<RequestGraphEdge> outgoingEdges = this.graph.outgoingEdgesOf(lastNode);
                ArrayList<RequestGraphNode> outNodes = new ArrayList<>();
                for (RequestGraphEdge edge: outgoingEdges) {
                    outNodes.add((RequestGraphNode)edge.getTarget());
                }
                if (outNodes.size() == 0 ){
                    pathiEnd = true;
                } else {
                    // add every succeed to path
                    for (int j = 1; j < outNodes.size(); j++) {
                        RequestGraphNode next = outNodes.get(j);
                        RequestPath newPath = path.deepAdd(next);
                        this.paths.add(newPath);
                    }
                    RequestGraphNode next = outNodes.get(0);
                    // add to current path
                    path.add(next);
                }
                allPathEnd = allPathEnd && pathiEnd;
            }
            if (allPathEnd) {
                break;
            }
        }

        //simplified every path
        for( RequestPath path : this.paths) {
            path.simplify();
            path.genID();
        }
    }

    public ArrayList<RequestPath> getPaths() {
        return this.paths;
    }

    public String toString() {
        String value = "";
        for (String vexKey : vexes.keySet()) {
            RequestGraphNode n =  vexes.get(vexKey);
            value = value+" "+ n.toString()+" value:"+n.value();
        }
        return graph.toString() + " "+ value;
    }

    public void addEdge(RequestGraphNode src, RequestGraphNode dest) {
        this.graph.addEdge(src, dest);
    }

    public void setHistory(){
        for(String vexKey : this.vexes.keySet()) {
            RequestGraphNode node = this.vexes.get(vexKey);
            node.genFirstHistory();
        }
    }
    
    public ArrayList involveHosts(){
        ArrayList<Integer> hostList = new ArrayList<>();
        for (String vexKey : vexes.keySet()) {
            RequestGraphNode n =  vexes.get(vexKey);
            int hostID = n.getHostID();
            if (!hostList.contains(hostID)) {
                hostList.add(hostID);
            }
        }
        return hostList;
    }

    public ArrayList pathFilter(int hostID){
        ArrayList<RequestPath> res = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            RequestPath path = paths.get(i);
            if (path.hasHost(hostID)){
                res.add(path);
            }
        }
        return res;
    }

    public String getApp(){
        String svcName = this.root.getSvcName();
        ServiceEntity se = (ServiceEntity) CloudSim.getEntity(svcName);
        return se.getApp();
    }
}
