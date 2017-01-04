package agent1t;

//package 

import jade.core.Agent;
import jade.core.AID;

//import jade.core.Agent;
import jade.core.behaviours.*;
import java.util.*;
import java.util.List;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;


public class BuyerAgent extends Agent {
	
	/**
	  @author Ponomarev Sergey
	 */
	
	
	private AID[] sellerAgents;
	
	//Setup is analog to constuctor
	protected void setup() {
		// Printout a welcome message
		System.out.println("Hello! Buyer-agent "+getAID().getName()+" is ready.");
		// Get the title of the stuff to buy as a start-up argument
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
		targetStuffTitle = (String) args[0];
		System.out.println("Trying to buy "+targetStuffTitle);
		// Add a TickerBehaviour that schedules a request to seller agents every minute
		addBehaviour(new TickerBehaviour(this, 60000) {
			protected void onTick() {
				System.out.println("Trying to buy "+targetStuffTitle);
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

				// Perform the request
				myAgent.addBehaviour(new RequestPerformer());
			}
		} );
	}
			else {
			// Make the agent terminate
			System.out.println("No target stuff title specified");
			doDelete();
			}
			}
	//The title of the stuff to buy
	private String targetStuffTitle;
	// The list of known seller agents
	//private AID[] sellerAgents = {new AID("seller1", AID.ISLOCALNAME),
	//new AID("seller2", AID.ISLOCALNAME)};
	
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
						cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
						myAgent.send(cfp);
						// Prepare the template to get proposals
						mt = MessageTemplate.and(MessageTemplate.MatchConversationId("stuff-trade"),
								MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
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
								}
							}
							repliesCnt++;
							if (repliesCnt >= sellerAgents.length) {
								// We received all replies
								step = 2; 
							}
						}
						else {
							block();
						}
						break;
					case 2:
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
						step = 3;
						break;
					case 3:      
						// Receive the purchase order reply
						reply = myAgent.receive(mt);
						if (reply != null) {
							// Purchase order reply received
							if (reply.getPerformative() == ACLMessage.INFORM) {
								// Purchase successful. We can terminate
								System.out.println(targetStuffTitle+" successfully purchased from agent "+reply.getSender().getName());
								System.out.println("Price = "+bestPrice);
								myAgent.doDelete();
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
			
			
}
