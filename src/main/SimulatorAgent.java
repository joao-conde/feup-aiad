package main;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

public class SimulatorAgent extends Agent{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	protected void setup() {
		Object[] args = this.getArguments();
		addBehaviour(new LaunchSimulationsBehaviour(this, Integer.parseInt(args[0].toString())));
	}
	
	private class LaunchSimulationsBehaviour extends SimpleBehaviour{
		
		Boolean finished = false;
		Integer simulations, currSimulation = 1;
		
		public LaunchSimulationsBehaviour(Agent myAgent, Integer simulations) {
			this.myAgent = myAgent;
			this.simulations = simulations;
		}
		
		@Override
		public void action() {
			System.out.println("Simulation No " + currSimulation);
			
			currSimulation++;
			if(currSimulation > simulations) finished = true;
		}
		
		@Override
		public boolean done() {
			return finished;
		}
		
	}
	
}
