package main;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import utilities.Utils;

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
		
		private Boolean finished = false;
		protected Integer simulations;
		protected Simulation currentSimulation = null;
		
		public LaunchSimulationsBehaviour(Agent myAgent, Integer simulations) {
			this.myAgent = myAgent;
			this.simulations = simulations;
		}
		
		@Override
		public void action() {
			
			if(currentSimulation != null && !currentSimulation.finished)
				return;
			
			if(simulations <= 0) {
				finished = true;
				System.out.println("No more simulations to run");
				return;
			}
			
			this.currentSimulation = new Simulation();
			System.out.println("Launched simulaton number " + simulations);
			addBehaviour(currentSimulation);
			simulations--;
		}
		
		@Override
		public boolean done() {
			return finished;
		}
		
		private class Simulation extends SimpleBehaviour{
			
			public Boolean finished = false;
			public Boolean init = false;
			public Market market = null;

			@Override
			public void action() {
				
				
				if(!init) {
					System.out.println("Running simulation untill all agents die");
					market = new Market();
					this.init = true;
				}
				//myAgent.getAgentState() ---> Deleted
				finished = true;
				//System.out.println("Simulation over");
			}
			
			@Override
			public boolean done() {
				return finished;
			}
		}
		
	}
	
	
	
}
