package henryhoo;

public class Node {
 byte weight;
 byte high;
// byte low;
 byte[] hpath ; 
// byte[] lpath ;
public Node() {
	this.weight = 0;
	this.high = 0;
//	this.low = 0;
	hpath=new byte[256];
//	lpath=new byte[256];
	// TODO Auto-generated constructor stub
}
}
