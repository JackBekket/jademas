package agent1t;

//package 

import jade.core.Agent;
import jade.core.AID;

//import jade.core.Agent;
import jade.core.behaviours.*;
import java.util.*;

//import agent1t.SellerAgent.PurchaseOrdersServer;
//import agent1t.SellerAgent.OfferRequestsServer;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

/**
@author Ponomarev Sergey
*/



public class BrokerAgent extends Agent {

	

	private AID[] sellerAgents;
	
	//Setup is analog to constuctor
	protected void setup() {
		// Printout a welcome message
		System.out.println("Hello! Broker-agent "+getAID().getName()+" is ready.");
		
		// Register the stuff-selling service in the yellow pages
					DFAgentDescription dfd = new DFAgentDescription();
					dfd.setName(getAID());
					ServiceDescription sd = new ServiceDescription();
					sd.setType("broker-selling");
					sd.setName("JADE-broker-trading");
					dfd.addServices(sd);
					try {
						DFService.register(this, dfd);
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}
					
		
		// The parameters now is not an argument need to rewrite this
		// Get the title of the stuff to buy as a start-up argument
		
		Object[] args = getArguments();
	//	if (args != null && args.length > 0) {
		//targetStuffTitle = (String) args[0];
		//	rule= (Integer) args[0];
			rule = 10;
			System.out.println("Overprice is "+rule);
			
	//	System.out.println("Trying to buy "+targetStuffTitle);
		
		
		// Add a TickerBehaviour that schedules a request to seller agents every minute
		addBehaviour(new TickerBehaviour(this, 30000) {
			protected void onTick() {
				
			//	System.out.println("Trying to buy "+targetStuffTitle);
				// Update the list of seller agents
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("stuff-selling");
				template.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template); 
					System.out.println("Found the following seller agents:");
			sellerAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						sellerAgents[i] = result[i].getName();
						System.out.println(sellerAgents[i].getName());
					}
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}

				// Add the behaviour serving requests for offer from buyer agents
				addBehaviour(new OfferRequestsServer1());
				
				//Add the bahavior for purchase
				// Add the behaviour serving purchase orders from buyer agents
				addBehaviour(new PurchaseOrdersServer1());
				
				
				
				// Perform the request
			//	myAgent.addBehaviour(new RequestPerformer());
				
				
			
			}
		} );
	}
		//	else {
			// Make the agent terminate
		//	System.out.println("No target stuff title specified");
		//	doDelete();
		//	}
			//}
	//The title of the stuff to buy
	private String targetStuffTitle;
	// The list of known seller agents
	//private AID[] sellerAgents = {new AID("seller1", AID.ISLOCALNAME),
	//new AID("seller2", AID.ISLOCALNAME)};
	
	private Integer rule;
	
	private Hashtable catalogueBroker;
	
	
	// Put agent clean-up operations here
			protected void takeDown() {
			// Printout a dismissal message
			System.out.println("Buyer-agent "+getAID().getName()+" terminating.");
			}
	
			
			
			
			
			
			
			
			
			/**
			   Inner class RequestPerformer.
			   This is the behaviour used by buyer agents to request seller 
			   agents the target stuff.
			 */
			private class RequestPerformer extends Behaviour {
				private AID bestSeller; // The agent who provides the best offer 
				private int bestPrice;  // The best offered price
				private int repliesCnt = 0; // The counter of replies from seller agents
				private MessageTemplate mt; // The template to receive replies
				private int step = 0;
				private int ourPrice;
				

				public void action() {
					switch (step) {
					case 0:
						// Send the cfp to all sellers
						ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
						for (int i = 0; i < sellerAgents.length; ++i) {
							cfp.addReceiver(sellerAgents[i]);
						} 
						cfp.setContent(targetStuffTitle);
						cfp.setConversationId("stuff-trade");
						//Уникальное значение нужно, что бы различать сообщения от продавцов, т.к. они начинают диалоги с одинаковыми названиями
						//Устаревшая хрень, но все равно занятно
						cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
						myAgent.send(cfp);
						// Prepare the template to get proposals
						mt = MessageTemplate.and(MessageTemplate.MatchConversationId("stuff-trade"),
								MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
						System.out.println("RequestPerformer init");
						step = 1;
						break;
					case 1:
						// Receive all proposals/refusals from seller agents
						ACLMessage reply = myAgent.receive(mt);
						if (reply != null) {
							// Reply received
							if (reply.getPerformative() == ACLMessage.PROPOSE) {
								// This is an offer 
								int price = Integer.parseInt(reply.getContent());
								if (bestSeller == null || price < bestPrice) {
									// This is the best offer at present
									bestPrice = price;
									bestSeller = reply.getSender();
									ourPrice=bestPrice + rule;
								}
							}
							repliesCnt++;
							if (repliesCnt >= sellerAgents.length) {
								// We received all replies
								
								/**
								//Send reply to the buyer
								if (bestPrice!=0){
								reply.setPerformative(ACLMessage.PROPOSE);
								reply.setContent(String.valueOf(bestPrice));
								}
								**/
								
								System.out.println("Recived all replies");
								step = 2; 
							}
						}
						else {
							block();
						}
						break;
					case 2:
						
						//Send reply to the buyer with price
						
						
						
						
						// Send the purchase order to the seller that provided the best offer
						ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
						order.addReceiver(bestSeller);
						order.setContent(targetStuffTitle);
						order.setConversationId("stuff-trade");
						order.setReplyWith("order"+System.currentTimeMillis());
						myAgent.send(order);
						// Prepare the template to get the purchase order reply
						mt = MessageTemplate.and(MessageTemplate.MatchConversationId("stuff-trade"),
								MessageTemplate.MatchInReplyTo(order.getReplyWith()));
						System.out.println("Purchase order to seller is sent");
						step = 3;
						
						
						
						break;
					case 3:      
						// Receive the purchase order reply
						reply = myAgent.receive(mt);
						if (reply != null) {
							// Purchase order reply received
							if (reply.getPerformative() == ACLMessage.INFORM) {
								// Purchase successful. 
								System.out.println(targetStuffTitle+" successfully purchased from agent "+reply.getSender().getName());
								System.out.println("Price = "+bestPrice);
								ourPrice = bestPrice+rule;
								//Input it to our catalogue
								catalogueBroker.put(targetStuffTitle, new Integer(ourPrice));
								
							//	myAgent.doDelete();
							}
							else {
								System.out.println("Attempt failed: requested stuff already sold.");
							}

							step = 4;
						}
						else {
							block();
						}
						break;
					}        
				}

				public boolean done() {
					if (step == 2 && bestSeller == null) {
						System.out.println("Attempt failed: "+targetStuffTitle+" not available for sale");
					}
					return ((step == 2 && bestSeller == null) || step == 4);
				}
			}  // End of inner class RequestPerformer
				
	
			/**
			   Inner class OfferRequestsServer.
			   This is the behaviour used by seller agents to serve incoming requests 
			   for offer from buyer agents.
			   If the requested stuff is in the local catalogue the seller agent replies 
			   with a PROPOSE message specifying the price. Otherwise a REFUSE message is
			   sent back.
			   
			   This class must recive messages from Buyer
			 */
			private class OfferRequestsServer1 extends CyclicBehaviour {
				public void action() {
					MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
					ACLMessage msg = myAgent.receive(mt);
					if (msg != null) {
						// CFP Message received. Process it
						String title = msg.getContent();
						ACLMessage reply = msg.createReply();

						targetStuffTitle = title;
						System.out.println("Name is" +targetStuffTitle);
						// Perform the request
						myAgent.addBehaviour(new RequestPerformer());
						System.out.println("Sold is ok" +targetStuffTitle);
						Integer price = (Integer) catalogueBroker.get(title);
						if (price != null) {
							// The requested stuff is available for sale. Reply with the price
							reply.setPerformative(ACLMessage.PROPOSE);
							reply.setContent(String.valueOf(price.intValue()));
						}
						else {
							// The requested stuff is NOT available for sale.
							reply.setPerformative(ACLMessage.REFUSE);
							reply.setContent("not-available");
						}
						myAgent.send(reply);
					}
					else {
						block();
					}
				}
			}  // End of inner class OfferRequestsServer
		
			
			
			/**
			   Inner class PurchaseOrdersServer.
			   This is the behaviour used by seller agents to serve incoming 
			   offer acceptances (i.e. purchase orders) from buyer agents.
			   The seller agent removes the purchased stuff from its catalogue 
			   and replies with an INFORM message to notify the buyer that the
			   purchase has been sucesfully completed.
			 */
			private class PurchaseOrdersServer1 extends CyclicBehaviour {
				public void action() {
					MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
					ACLMessage msg = myAgent.receive(mt);
					if (msg != null) {
						// ACCEPT_PROPOSAL Message received. Process it
						String title = msg.getContent();
						ACLMessage reply = msg.createReply();

						Integer price = (Integer) catalogueBroker.remove(title);
						if (price != null) {
							reply.setPerformative(ACLMessage.INFORM);
							System.out.println(title+" sold to agent "+msg.getSender().getName());
						}
						else {
							// The requested stuff has been sold to another buyer in the meanwhile .
							reply.setPerformative(ACLMessage.FAILURE);
							reply.setContent("not-available");
						}
						myAgent.send(reply);
					}
					else {
						block();
					}
				}
			}  // End of inner class PurshaseOffersServer
			
			
			
			
}
