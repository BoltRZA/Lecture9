package Behaviours;

import jade.core.AID;
import jade.core.Agent;
import jade.core.Service;
import jade.core.ServiceDescriptor;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

import java.util.*;

public class CTInitiator extends ContractNetInitiator {
    private String service = null;
    private Agent agent;
    private AID bestSeller = null;
    private double bestPrice = -1;
    private ACLMessage bestOffer = null;

    public CTInitiator(Agent agent, ACLMessage cfp, String service) {
        super(agent, cfp);
        this.service = service;
    }

    @Override
    protected Vector prepareCfps(ACLMessage cfp) {
        List<AID> agentList = getAvailibleAgents();
        cfp.setContent("i want to buy a book");
        for (AID aid:agentList){
            cfp.addReceiver(aid);
        }
        Date date = new Date(System.currentTimeMillis() + 5000);
        cfp.setReplyByDate(date);//Ждет до момента, потом начинает выполнение.
        return super.prepareCfps(cfp);
    }

    @Override
    protected void handleAllResponses(Vector responses, Vector acceptances) {
        Set<AID> participants = new HashSet<AID>();

        for (Object object:responses) { // Мы хз какого типа этот message, потом приведем тип
            if (object instanceof ACLMessage) {
                ACLMessage message = (ACLMessage) object;
                if (message.getPerformative() == ACLMessage.PROPOSE) {
                    participants.add(message.getSender());
                    System.out.println(agent.getLocalName() + " i've received propose " +
                            message.getContent() + " from " + message.getSender().getLocalName());
                    if (bestPrice == -1) {
                        bestPrice = Double.parseDouble(message.getContent());
                        bestSeller = message.getSender();
                        bestOffer = message;
                    } else {
                        double offeredPrice = Double.parseDouble(message.getContent());
                        if (offeredPrice < bestPrice) {
                            bestSeller = message.getSender();
                            bestPrice = offeredPrice;
                            bestOffer = message;
                        }
                    }
                } else if (message.getPerformative() == ACLMessage.REFUSE) {
                    System.out.println(agent.getLocalName() + " i've received refuse from "
                            + message.getSender().getLocalName());
                }
            }
        }
        if (bestSeller != null){
            ACLMessage reply = bestOffer.createReply();
            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            acceptances.add(reply);
        }
        for (Object obj: responses){
            if (obj instanceof ACLMessage){
                ACLMessage refuse = (ACLMessage)obj;
                if (!refuse.getSender().equals(bestSeller)){
                    ACLMessage reply = refuse.createReply();
                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    acceptances.add(reply);
                }
            }
        }
        super.handleAllResponses(responses, acceptances);
    }

    @Override
    protected void handleAllResultNotifications(Vector resultNotifications) {
        for (Object obj:resultNotifications){
            if (obj instanceof ACLMessage){
                ACLMessage note = (ACLMessage)obj;
                System.out.println(agent.getLocalName() + " I've received notification " + note.getContent() +
                        " from " + note.getSender().getLocalName());
//                TODO: if type of msg equals Inform then you should handle result
//                TODO: else repeat contractNetAuction
            }
        }
        super.handleAllResultNotifications(resultNotifications);
    }

    private List<AID> getAvailibleAgents(){
        List<AID> agentList = new ArrayList<AID>();

        if (service == null){
            System.out.println("Service must be not null!");
        }
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-buying");
        template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(agent,template);
                for (int i = 0;i < result.length;i++){
                    agentList.add(result[i].getName());
                }
            } catch (FIPAException e) {
                e.printStackTrace();
            }
        return agentList;
    }
}
