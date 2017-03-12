import java.io.*;
import java.util.*;
import java.text.*;
import java.math.*;
import java.util.regex.*;

/**
   Author: Karthik Gopalakrishnan (github.com/g-karthik)
*/

public class SpreadsheetEvaluator {
    
    public static int A_ASCII = 65;
    public static String SPACE = " ";
    public static String cellIdRegex = "[A-Z][1-9]+[0-9]*";
    public static String circularDependencyError = "Error: Circular dependency!";

    /*
        Returns array with as many capitalized letters of the English alphabet as present in the spreadsheet's row ids
    */
    public static char [] initializeRowLetters(int rows)
    {
        char [] rowLetters = new char[rows];
        for(int index=0; index<rows; index++)
            rowLetters[index] = (char) (A_ASCII + index);
        return rowLetters;
    }
    
    /*
        A dependency graph: nodes represent cells in the input spreadsheet, a directed edge from node A to node B
        means that cell A's expression depends on the evaluation of cell B
    */    
    static class Graph
    {
        enum Color {WHITE, GRAY, BLACK};    //various stages of depth-first search processing of a node (see CLRS)
        
        class Node
        {
            String cellId;
            String expression;
            double value;
            Node[] dependencies;
            
            Color processingColor = Color.WHITE;
                        
            Node(String cellId)
            {
                this.cellId = cellId;
            }

            Node(String cellId, String expression)
            {
                this.cellId = cellId;
                this.expression = expression;
            }
            
            public String getCellId()
            {
                return cellId;
            }
            
            public void setCellId(String cellId)
            {
                this.cellId = cellId;
            }
            
            public Color getColor()
            {
                return processingColor;
            }
            
            public void setColor(Color color)
            {
                processingColor = color;
            }
            
            public String getExpression()
            {
                return expression;
            }
            
            public void setExpression(String expression)
            {
                this.expression = expression;
            }
            
            public double getValue()
            {
                return value;
            }
            
            public void setValue(double value)
            {
                this.value = value;
            }
            
            public Node[] getDependencies()
            {
                return dependencies;
            }
            
            public void setDependencies(Node[] dependencies)
            {
                this.dependencies = dependencies;
            }
            
            public List<String> computeDependencies(String expression)
            {
                String [] parts = expression.split(SPACE);
                List<String> cellIds = new ArrayList<>();

                for(String part: parts)
                {
                    if(part.matches(cellIdRegex))   //is the part a cellId?
                        cellIds.add(part);                    
                }

                return cellIds;
            }
            
            public void initializeNodeDependencies(List<String> cellIds)
            {
                dependencies = new Node[cellIds.size()];
                
                for(int index=0; index<cellIds.size(); index++)
                {
                    String cellId = cellIds.get(index);
                    Node fatherNode = null;
                    if(cellNodeMap.containsKey(cellId))
                        fatherNode = cellNodeMap.get(cellId);
                    else
                    {
                        fatherNode = new Node(cellId);
                        cellNodeMap.put(cellId, fatherNode);
                    }

                    dependencies[index] = fatherNode;
                }
            }
            
            /*
                Replace a specific dependency with its computed value
            */
            public void replaceDependencyWithValue(String dependency, double dValue)
            {
                expression = expression.replace(dependency, Double.toString(dValue));
            }

            /*
                Evaluate RPN expression
            */
            public void evaluateRPNExpression() throws ArithmeticException, EmptyStackException
            {
                Stack<Double> stack = new Stack<>();
                for(String token : expression.split("\\s+"))
                {
                    switch(token)
                    {
                        case "+":
                            stack.push(stack.pop() + stack.pop());
                            break;
                        case "-":
                            stack.push(-stack.pop() + stack.pop());
                            break;
                        case "*":
                            stack.push(stack.pop() * stack.pop());
                            break;
                        case "/":
                            double divisor = stack.pop();
                            stack.push(stack.pop() / divisor);
                            break;
                        default:
                            stack.push(Double.parseDouble(token));
                            break;
                    }
                }
                value = stack.pop();
            }
            
        }
        
        int nodeCount;      //number of nodes in the graph
        Node[] nodes;       //references of all nodes in the graph: one node for each cell in the spreadsheet
        Map<String, Node> cellNodeMap;  //a mapping from cellId to cell Node reference
        
        int rows;       //spreadsheet row count
        int cols;       //spreadsheet column count
        
        /*
            Reads and initializes the dependency graph for the input spreadsheet
        */
        public void initialize() throws Exception
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            String [] inputDimensions = reader.readLine().split(SPACE);
            rows = Integer.parseInt(inputDimensions[1]);
            cols = Integer.parseInt(inputDimensions[0]);
            
            nodeCount = rows*cols;
            nodes = new Node[nodeCount];
            cellNodeMap = new HashMap<>();

            char [] rowLetters = initializeRowLetters(rows);
            
            int nodeIterator = 0;
            
            for(char row: rowLetters)
            {
                for(int colIndex=1; colIndex<=cols; colIndex++)
                {
                    String expression = reader.readLine();
                    String cellId = Character.toString(row) + Integer.toString(colIndex);

                    if(!cellNodeMap.containsKey(cellId))
                        nodes[nodeIterator] = new Node(cellId, expression);
                    else
                    {
                        nodes[nodeIterator] = cellNodeMap.get(cellId);
                        nodes[nodeIterator].setExpression(expression);
                    }
                    
                    List<String> dependencies = nodes[nodeIterator].computeDependencies(expression);
                    nodes[nodeIterator].initializeNodeDependencies(dependencies);
                    cellNodeMap.put(cellId, nodes[nodeIterator]);
                    
                    nodeIterator += 1;
                }
            }
        }
        
        /*
            Perform depth-first processing of 'node'
        */
        public double depthFirstProcess(Node node) throws Exception
        {
            node.setColor(Color.GRAY); //processing in progress
            Node[] dependencies = node.getDependencies();
            
            for(Node dependency: dependencies)
            {
                Color color = dependency.getColor();
                if(color == Color.GRAY) //circular dependency
                    throw new Exception(circularDependencyError);
                
                double value = depthFirstProcess(dependency);
                node.replaceDependencyWithValue(dependency.getCellId(), value);                
            }
            
            node.evaluateRPNExpression();
            node.setColor(Color.BLACK); //processing complete
            
            return node.getValue();
        }
        
        /*
            Evaluate all cells by performing depth-first processing
        */
        public void evaluate() throws Exception
        {
            for(String cellId: cellNodeMap.keySet())
            {
                Node node = cellNodeMap.get(cellId);
                if(node.getColor() == Color.WHITE)
                    depthFirstProcess(node);
            }
        }
        
        /*
            A helper function for debugging
        */
        public void print()
        {
            System.out.println(rows + " " + cols);
            
            for(String cellId: cellNodeMap.keySet())
            {
                System.out.println(cellId + "=" + cellNodeMap.get(cellId).getExpression());
                Node [] depend = cellNodeMap.get(cellId).getDependencies();
                for(int i=0; i<depend.length; i++)
                {
                    System.out.println(cellId + " depends on " + depend[i].getCellId());
                }
            }
        }
        
        /*
            Prints the output
        */
        public void printOutput()
        {
            for(int index=0; index<nodes.length; index++)
                System.out.println(String.format("%.5f", nodes[index].getValue()));
        }
        
    }
    
    public static void main(String args[]) throws Exception
    {
        Graph graph = new Graph();
        
        try
        {
            graph.initialize();
            graph.evaluate();
        }
        catch(Exception e)
        {
            String message = e.getMessage();
            if(message.equals(circularDependencyError))
            {
                System.out.println(message);
                return;
            }
            
            throw e;
        }
        
        graph.printOutput();
        
    }
}
