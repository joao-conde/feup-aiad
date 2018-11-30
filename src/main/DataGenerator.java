package main;

import java.io.File;

import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import utilities.MarketLogger;

/*
 * DataGenerator
 * 
 * Responsible for booting up JADE runtime and main container aswell
 * as one agent that will re-run the auction market simulation multiple
 * times
 */
public class DataGenerator {

	
	private Runtime jadeRt;
	private ContainerController mainContainer;
	
	public static void main(String[] args) {
		new DataGenerator(Integer.parseInt(args[0]));
	}
	
	public DataGenerator(Integer simulations) {
		try {
			MarketLogger.calculateLogPath();
			new File(MarketLogger.logPath).mkdirs();
			this.initializeContainers();
			this.initializeSimulatorAgent(simulations);
		} catch (SecurityException | StaleProxyException e) {
			e.printStackTrace();
		}	
	}
	
	public void initializeContainers() {
		this.jadeRt = Runtime.instance();
		this.mainContainer = jadeRt.createMainContainer(new ProfileImpl());
	}
	
	public void initializeSimulatorAgent(Integer simulations) throws StaleProxyException {
		Object[] args = {simulations};
		AgentController simulatorAgent = this.mainContainer.createNewAgent("SimulatorAgent", "main.SimulatorAgent", args);
		simulatorAgent.start();
	}

}
