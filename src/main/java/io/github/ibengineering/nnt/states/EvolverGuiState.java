package io.github.ibengineering.nnt.states;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

public class EvolverGuiState extends BaseAppState {
	
	private EvolverGuiMainMenuState stateMainMenu;

	@Override
	protected void initialize(Application app) {
		getStateManager().attach(stateMainMenu);
	}

	@Override
	protected void cleanup(Application app) {
		getStateManager().detach(stateMainMenu);
	}

	@Override
	protected void onEnable() {
		stateMainMenu.setEnabled(true);
	}

	@Override
	protected void onDisable() {
		stateMainMenu.setEnabled(false);
	}
	
	
	
}
