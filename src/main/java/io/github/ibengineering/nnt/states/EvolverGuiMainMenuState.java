package io.github.ibengineering.nnt.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.style.BaseStyles;

public class EvolverGuiMainMenuState extends BaseAppState {

	private Node subGuiNode;
	
	private Container leftButtonPanel;
	
	@Override
	protected void initialize(Application app) {
		if(GuiGlobals.getInstance() == null) {
			GuiGlobals.initialize(app);
			BaseStyles.loadGlassStyle();
			GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
		}
		
		subGuiNode = new Node("GUI Node of EvolverGuiMainMenuState");
		((SimpleApplication)app).getGuiNode().attachChild(subGuiNode);
		
		leftButtonPanel = new Container();
//		subGuiNode.attachChild(leftButtonPanel);
		
		int w = app.getCamera().getWidth();
		int h = app.getCamera().getHeight();
		
		leftButtonPanel.setPreferredSize(new Vector3f(240, h, 0));
		leftButtonPanel.setLocalTranslation(0, h, 0);
		leftButtonPanel.setInsets(new Insets3f(24, 24, 24, 24));
		leftButtonPanel.addChild(new Label("text"));
	}

	@Override
	protected void cleanup(Application app) {
		
	}

	@Override
	protected void onEnable() {
	}

	@Override
	protected void onDisable() {
	}
	
	
	
}
